package mr.popo.localaiagent.chat.service;

import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.chat.domain.ChatMode;
import mr.popo.localaiagent.chat.domain.ChatSession;
import mr.popo.localaiagent.chat.dto.ChatSessionDto;
import mr.popo.localaiagent.chat.dto.CreateChatRequest;
import mr.popo.localaiagent.chat.dto.UpdateChatRequest;
import mr.popo.localaiagent.chat.mapper.ChatMapper;
import mr.popo.localaiagent.chat.repository.ChatSessionRepository;
import mr.popo.localaiagent.common.exception.ResourceNotFoundException;
import mr.popo.localaiagent.common.pagination.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository repository;
    private final ChatMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ChatSessionDto> list(Long ownerId, Pageable pageable) {
        Page<ChatSession> page = repository.findAllByOwner(ownerId, pageable);
        return PageResponse.of(page.map(mapper::toDto));
    }

    @Transactional(readOnly = true)
    public ChatSessionDto get(Long id, Long ownerId) {
        return mapper.toDto(loadOwned(id, ownerId));
    }

    @Transactional
    public ChatSessionDto create(Long ownerId, CreateChatRequest request) {
        ChatSession session = new ChatSession();
        session.setOwnerId(ownerId);
        session.setTitle(request.title() != null ? request.title() : "New chat");
        session.setMode(request.mode() != null ? request.mode() : ChatMode.NORMAL);
        session.setModelName(request.modelName());
        session.setKnowledgeBaseId(request.knowledgeBaseId());
        return mapper.toDto(repository.save(session));
    }

    @Transactional
    public ChatSessionDto update(Long id, Long ownerId, UpdateChatRequest request) {
        ChatSession session = loadOwned(id, ownerId);
        if (request.title() != null) session.setTitle(request.title());
        if (request.mode() != null) session.setMode(request.mode());
        if (request.modelName() != null) session.setModelName(request.modelName());
        if (request.knowledgeBaseId() != null) session.setKnowledgeBaseId(request.knowledgeBaseId());
        return mapper.toDto(session);
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        ChatSession session = loadOwned(id, ownerId);
        repository.delete(session);
    }

    @Transactional
    public void touchLastMessage(Long sessionId) {
        repository.findById(sessionId).ifPresent(s -> s.setLastMessageAt(OffsetDateTime.now()));
    }

    /**
     * Charge une session en vérifiant l'ownership. 404 si elle n'appartient pas
     * à l'utilisateur — on ne leak pas son existence.
     */
    @Transactional(readOnly = true)
    public ChatSession loadOwned(Long id, Long ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("ChatSession", id));
    }
}
