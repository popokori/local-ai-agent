package mr.popo.localaiagent.message.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.agent.dto.AgentEvent;
import mr.popo.localaiagent.agent.dto.AgentRequest;
import mr.popo.localaiagent.agent.service.AgentService;
import mr.popo.localaiagent.audit.domain.AuditAction;
import mr.popo.localaiagent.audit.service.AuditService;
import mr.popo.localaiagent.chat.domain.ChatSession;
import mr.popo.localaiagent.chat.service.ChatService;
import mr.popo.localaiagent.common.pagination.PageResponse;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;
import mr.popo.localaiagent.message.domain.Message;
import mr.popo.localaiagent.message.domain.MessageRole;
import mr.popo.localaiagent.message.dto.MessageDto;
import mr.popo.localaiagent.message.dto.SendMessageRequest;
import mr.popo.localaiagent.message.mapper.MessageMapper;
import mr.popo.localaiagent.memory.service.MemoryConsolidator;
import mr.popo.localaiagent.message.repository.MessageRepository;
import mr.popo.localaiagent.rag.dto.RagHit;
import mr.popo.localaiagent.streaming.dto.StreamEventType;
import mr.popo.localaiagent.streaming.service.SseStreamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatService chatService;
    private final AgentService agentService;
    private final SseStreamService sseStreamService;
    private final AuditService auditService;
    private final MemoryConsolidator memoryConsolidator;

    @Value("${app.agent.history-window:20}")
    private int historyWindow;

    @Value("${app.sse.timeout-seconds:300}")
    private long sseTimeoutSeconds;

    @Transactional
    public PageResponse<MessageDto> history(Long sessionId, Long ownerId, Pageable pageable) {
        chatService.loadOwned(sessionId, ownerId);
        Page<Message> page = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId, pageable);
        return PageResponse.of(page.map(messageMapper::toDto));
    }

    public SseEmitter sendAndStream(Long sessionId, Long ownerId, SendMessageRequest request) {
        ChatSession session = chatService.loadOwned(sessionId, ownerId);

        if (request.clientRequestId() != null) {
            Optional<Message> existing = messageRepository
                    .findBySessionIdAndClientRequestId(sessionId, request.clientRequestId());
            if (existing.isPresent()) {
                return replayCachedAnswer(sessionId, request.clientRequestId());
            }
        }

        Message userMsg = persistUserMessage(sessionId, request);
        List<ChatMessageDto> history = loadHistoryForLlm(sessionId, userMsg.getId());

        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(sseTimeoutSeconds).toMillis());
        StringBuilder fullReply = new StringBuilder();
        List<RagHit> capturedSources = new ArrayList<>();
        long startMs = System.currentTimeMillis();

        AgentRequest agentRequest = new AgentRequest(
                session.getId(), ownerId, session.getMode(), request.content(),
                history, session.getModelName(), session.getKnowledgeBaseId());

        Executors.newVirtualThreadPerTaskExecutor().execute(() ->
                runStream(emitter, agentRequest, session, userMsg, fullReply, capturedSources, startMs));

        emitter.onCompletion(() -> log.debug("SSE completed for session {}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE timed out for session {}", sessionId);
            emitter.complete();
        });
        emitter.onError(ex -> log.warn("SSE error for session {}: {}", sessionId, ex.getMessage()));

        return emitter;
    }

    private void runStream(SseEmitter emitter, AgentRequest agentRequest,
                           ChatSession session, Message userMsg,
                           StringBuilder fullReply, List<RagHit> sources, long startMs) {
        try {
            agentService.handle(agentRequest)
                    .doOnNext(event -> dispatchEvent(emitter, event, fullReply, sources))
                    .doOnError(ex -> handleError(emitter, session, userMsg, ex, startMs))
                    .doOnComplete(() -> handleComplete(emitter, session, userMsg, fullReply, sources, startMs))
                    .blockLast();
        } catch (Exception ex) {
            handleError(emitter, session, userMsg, ex, startMs);
        }
    }

    private void dispatchEvent(SseEmitter emitter, AgentEvent event,
                               StringBuilder full, List<RagHit> sources) {
        switch (event) {
            case AgentEvent.Token t -> {
                if (t.text() != null && !t.text().isEmpty()) {
                    full.append(t.text());
                    sseStreamService.send(emitter, StreamEventType.TOKEN, Map.of("text", t.text()));
                }
            }
            case AgentEvent.Sources s -> {
                sources.addAll(s.hits());
                for (int i = 0; i < s.hits().size(); i++) {
                    RagHit h = s.hits().get(i);
                    sseStreamService.send(emitter, StreamEventType.SOURCE, Map.of(
                            "index", i + 1,
                            "documentId", h.documentId(),
                            "documentName", h.documentName(),
                            "page", h.pageNumber() == null ? "" : h.pageNumber(),
                            "score", Math.round(h.score() * 1000.0) / 1000.0,
                            "snippet", abbreviate(h.text(), 240)
                    ));
                }
            }
            case AgentEvent.ToolStart ts -> sseStreamService.send(emitter, StreamEventType.TOOL_START,
                    Map.of("iteration", ts.iteration(),
                            "name", ts.toolName(),
                            "arguments", ts.arguments() == null ? Map.of() : ts.arguments()));
            case AgentEvent.ToolEnd te -> sseStreamService.send(emitter, StreamEventType.TOOL_END,
                    Map.of("iteration", te.iteration(),
                            "name", te.toolName(),
                            "success", te.success(),
                            "summary", abbreviate(te.summary(), 400)));
            case AgentEvent.End ignored -> { /* persistance dans handleComplete */ }
            case AgentEvent.Error err -> sseStreamService.send(emitter, StreamEventType.ERROR,
                    Map.of("message", err.message()));
        }
    }

    @Transactional
    protected void handleComplete(SseEmitter emitter, ChatSession session, Message userMsg,
                                  StringBuilder full, List<RagHit> sources, long startMs) {
        long durationMs = System.currentTimeMillis() - startMs;
        Message assistant = new Message();
        assistant.setSessionId(session.getId());
        assistant.setRole(MessageRole.ASSISTANT);
        assistant.setContent(full.toString());
        assistant.setLatencyMs((int) durationMs);
        if (!sources.isEmpty()) {
            assistant.setSourcesJson(Map.of("hits",
                    sources.stream().map(h -> Map.<String, Object>of(
                            "documentId", h.documentId(),
                            "documentName", h.documentName(),
                            "page", h.pageNumber() == null ? -1 : h.pageNumber(),
                            "score", h.score()
                    )).toList()));
        }
        Message saved = messageRepository.save(assistant);

        chatService.touchLastMessage(session.getId());

        sseStreamService.send(emitter, StreamEventType.FINAL,
                Map.of("messageId", saved.getId(), "userMessageId", userMsg.getId()));
        emitter.complete();

        auditService.log(AuditAction.LLM_CALL, session.getOwnerId(), session.getId(),
                Map.of("model", session.getModelName() == null ? "" : session.getModelName(),
                        "chars", full.length(),
                        "sources", sources.size()),
                true, (int) durationMs);

        // Consolidation mémoire en arrière-plan (extraction de faits + résumé épisodique).
        // Ne bloque jamais la réponse au client.
        try {
            memoryConsolidator.consolidate(session.getOwnerId(), session.getId(),
                    userMsg.getContent(), full.toString());
        } catch (Exception ignore) {
            log.debug("Memory consolidation submit failed", ignore);
        }
    }

    @Transactional
    protected void handleError(SseEmitter emitter, ChatSession session, Message userMsg,
                               Throwable ex, long startMs) {
        long durationMs = System.currentTimeMillis() - startMs;
        log.error("Agent streaming failed for session {}", session.getId(), ex);
        sseStreamService.send(emitter, StreamEventType.ERROR,
                Map.of("message", "Agent call failed: " + ex.getMessage()));
        emitter.completeWithError(ex);
        auditService.log(AuditAction.LLM_CALL, session.getOwnerId(), session.getId(),
                Map.of("error", ex.getClass().getSimpleName(),
                        "userMessageId", userMsg.getId()),
                false, (int) durationMs);
    }

    @Transactional
    protected Message persistUserMessage(Long sessionId, SendMessageRequest request) {
        Message msg = new Message();
        msg.setSessionId(sessionId);
        msg.setRole(MessageRole.USER);
        msg.setContent(request.content());
        msg.setClientRequestId(request.clientRequestId());
        return messageRepository.save(msg);
    }

    private List<ChatMessageDto> loadHistoryForLlm(Long sessionId, Long excludeMessageId) {
        List<Message> recent = messageRepository.findTop40BySessionIdOrderByCreatedAtDesc(sessionId);
        Collections.reverse(recent);
        return recent.stream()
                .filter(m -> excludeMessageId == null || !m.getId().equals(excludeMessageId))
                .skip(Math.max(0, recent.size() - historyWindow - 1))
                .map(m -> new ChatMessageDto(roleToLlm(m.getRole()), m.getContent()))
                .toList();
    }

    private String roleToLlm(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    private SseEmitter replayCachedAnswer(Long sessionId, UUID clientRequestId) {
        SseEmitter emitter = new SseEmitter(5_000L);
        Optional<Message> userMsg = messageRepository
                .findBySessionIdAndClientRequestId(sessionId, clientRequestId);
        userMsg.ifPresent(um -> messageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId, Pageable.ofSize(100))
                .stream()
                .filter(m -> m.getRole() == MessageRole.ASSISTANT
                        && m.getCreatedAt().isAfter(um.getCreatedAt()))
                .findFirst()
                .ifPresent(answer -> {
                    sseStreamService.send(emitter, StreamEventType.TOKEN,
                            Map.of("text", answer.getContent()));
                    sseStreamService.send(emitter, StreamEventType.FINAL,
                            Map.of("messageId", answer.getId(), "replayed", true));
                }));
        emitter.complete();
        return emitter;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
