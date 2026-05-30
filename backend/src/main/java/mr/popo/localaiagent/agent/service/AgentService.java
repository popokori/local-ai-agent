package mr.popo.localaiagent.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.agent.dto.AgentEvent;
import mr.popo.localaiagent.agent.dto.AgentRequest;
import mr.popo.localaiagent.chat.domain.ChatMode;
import mr.popo.localaiagent.llm.config.LlmProperties;
import mr.popo.localaiagent.llm.dto.ChatDelta;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;
import mr.popo.localaiagent.llm.service.LlmService;
import mr.popo.localaiagent.rag.dto.RagAnswer;
import mr.popo.localaiagent.rag.service.RagService;
import mr.popo.localaiagent.memory.service.MemoryBlockBuilder;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import mr.popo.localaiagent.tools.registry.ToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Orchestrateur agent — Phase 3 : boucle ReAct (Thought / Action / Observation
 * / Final Answer) en format texte. Plus tolérant que le tool calling natif sur
 * les petits modèles type Llama 3.1 8B.
 * <p>
 * Flux émis vers le client :
 * <ol>
 *   <li>{@code Sources} si la session a une KB attachée et que le RAG a renvoyé des hits.</li>
 *   <li>Pour chaque itération outil : {@code ToolStart} → {@code ToolEnd}.</li>
 *   <li>{@code Token} sur tout le bloc "Final Answer" en un seul événement.</li>
 *   <li>{@code End}.</li>
 * </ol>
 * Le streaming token-par-token de la réponse finale arrivera en Phase 4
 * (parsing incrémental du stream LLM).
 */
@Slf4j
@Service
public class AgentService {

    private final PromptBuilder promptBuilder;
    private final ReactPromptBuilder reactPromptBuilder;
    private final ReactParser reactParser;
    private final LlmService llmService;
    private final RagService ragService;
    private final ToolRegistry toolRegistry;
    private final MemoryBlockBuilder memoryBlockBuilder;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.rag.top-k:5}")
    private int defaultTopK;

    @Value("${app.agent.max-iterations:5}")
    private int maxIterations;

    public AgentService(PromptBuilder promptBuilder,
                        ReactPromptBuilder reactPromptBuilder,
                        LlmService llmService,
                        RagService ragService,
                        ToolRegistry toolRegistry,
                        MemoryBlockBuilder memoryBlockBuilder,
                        LlmProperties llmProperties,
                        ObjectMapper objectMapper) {
        this.promptBuilder = promptBuilder;
        this.reactPromptBuilder = reactPromptBuilder;
        this.llmService = llmService;
        this.ragService = ragService;
        this.toolRegistry = toolRegistry;
        this.memoryBlockBuilder = memoryBlockBuilder;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.reactParser = new ReactParser(objectMapper);
    }

    public Flux<AgentEvent> handle(AgentRequest request) {
        return Flux.create(sink -> {
            try {
                runReact(request, sink::next);
                sink.complete();
            } catch (Exception ex) {
                log.error("Agent loop failed", ex);
                sink.next(new AgentEvent.Error(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
                sink.complete();
            }
        });
    }

    private void runReact(AgentRequest request, java.util.function.Consumer<AgentEvent> emit) {
        // 0. Sélection du modèle selon le mode (override user prime sur tout)
        String effectiveModel = resolveModelForMode(request.modelOverride(), request.mode());

        // 1. Auto-RAG si la session a une KB attachée (compat Phase 2)
        RagAnswer rag = maybeRunRag(request);
        if (!rag.isEmpty()) {
            emit.accept(new AgentEvent.Sources(rag.hits()));
        }

        // 2. Construire le system prompt + protocole ReAct.
        //    Si l'utilisateur n'a pas de KB attachée, rag_search n'a pas de cible →
        //    on le retire de la liste pour éviter que le LLM ne tente de l'utiliser.
        List<Tool> tools = new ArrayList<>(toolRegistry.all());
        if (request.knowledgeBaseId() == null) {
            tools.removeIf(t -> "rag_search".equals(t.name()));
        }
        String toolBlock = tools.isEmpty() ? null : reactPromptBuilder.build(tools);

        // Bloc mémoire (profil + souvenirs pertinents)
        String memoryBlock = memoryBlockBuilder.build(request.userId(), request.userMessage());

        List<ChatMessageDto> messages = promptBuilder.build(
                request.mode(), request.history(), request.userMessage(),
                rag.isEmpty() ? null : rag.contextBlock(),
                toolBlock,
                memoryBlock);

        ToolContext ctx = new ToolContext(request.userId(), request.sessionId());

        // 3. Boucle ReAct (streaming par tour — la Final Answer est émise token par token)
        final List<String> stopSeqs = List.of("\nObservation:", "\nObservation :");
        for (int iter = 1; iter <= maxIterations; iter++) {
            StreamResult sr;
            try {
                sr = streamOneIteration(effectiveModel, messages, stopSeqs, emit);
            } catch (Exception ex) {
                log.warn("LLM call failed at iter {}: {}", iter, ex.getMessage());
                emit.accept(new AgentEvent.Error("LLM call failed: " + ex.getMessage()));
                emit.accept(new AgentEvent.End("error"));
                return;
            }
            String llmText = sr.buffer;
            if (llmText.isBlank()) {
                emit.accept(new AgentEvent.Token("(Aucune réponse du modèle.)"));
                emit.accept(new AgentEvent.End("empty"));
                return;
            }
            log.debug("ReAct iter {} llm raw:\n{}", iter, abbreviate(llmText));

            if (sr.streamedFinalAnswer) {
                emit.accept(new AgentEvent.End("stop"));
                return;
            }
            ReactParser.Decision decision = reactParser.parse(llmText);
            if (decision instanceof ReactParser.Decision.FinalAnswer fa) {
                // Cas où le marker n'avait pas été détecté en streaming (ex. seul "Final Answer:" sans contenu après dans le flux)
                String text = fa.text().isBlank() ? llmText.trim() : fa.text();
                emit.accept(new AgentEvent.Token(text));
                emit.accept(new AgentEvent.End("stop"));
                return;
            }
            if (decision instanceof ReactParser.Decision.ToolCall tc) {
                emit.accept(new AgentEvent.ToolStart(iter, tc.toolName(), tc.arguments()));
                String observation;
                boolean success;
                String summary;
                Optional<Tool> tool = toolRegistry.find(tc.toolName());
                if (tool.isEmpty()) {
                    success = false;
                    summary = "unknown tool '" + tc.toolName() + "'";
                    observation = summary;
                } else {
                    try {
                        ToolResult result = tool.get().execute(tc.arguments(), ctx);
                        success = result.success();
                        summary = result.summary() == null ? "" : result.summary();
                        observation = renderObservation(result);
                    } catch (Exception ex) {
                        log.warn("tool {} failed: {}", tc.toolName(), ex.getMessage());
                        success = false;
                        summary = ex.getMessage();
                        observation = "error: " + ex.getMessage();
                    }
                }
                emit.accept(new AgentEvent.ToolEnd(iter, tc.toolName(), success, summary));

                messages = appendForReact(messages, llmText, observation);
                continue;
            }
        }
        // Pas de Final Answer après maxIterations
        emit.accept(new AgentEvent.Token(
                "(Désolé, je n'ai pas pu finaliser la réponse après " + maxIterations + " étapes d'outils.)"));
        emit.accept(new AgentEvent.End("max_iterations"));
    }

    private RagAnswer maybeRunRag(AgentRequest request) {
        if (request.knowledgeBaseId() == null) {
            return new RagAnswer("", List.of());
        }
        // Mode EXPERT : on remonte plus de passages pour avoir un raisonnement plus solide
        int topK = request.mode() == ChatMode.EXPERT ? Math.max(defaultTopK * 2, 8) : defaultTopK;
        try {
            return ragService.query(request.userId(), request.knowledgeBaseId(),
                    request.userMessage(), topK);
        } catch (Exception ex) {
            log.warn("RAG failed for kb={} : {} — fallback sans contexte",
                    request.knowledgeBaseId(), ex.getMessage());
            return new RagAnswer("", new ArrayList<>());
        }
    }

    /**
     * Sélection du modèle LLM selon le mode :
     *   NORMAL/FACT_CHECK → default-model, EXPERT → expert-model.
     * Un {@code modelOverride} explicite de la session prend toujours le pas.
     */
    private String resolveModelForMode(String modelOverride, ChatMode mode) {
        if (modelOverride != null && !modelOverride.isBlank()) return modelOverride;
        if (mode == ChatMode.EXPERT && llmProperties.getExpertModel() != null
                && !llmProperties.getExpertModel().isBlank()) {
            return llmProperties.getExpertModel();
        }
        if (mode == ChatMode.FACT_CHECK && llmProperties.getFactCheckModel() != null
                && !llmProperties.getFactCheckModel().isBlank()) {
            return llmProperties.getFactCheckModel();
        }
        return llmProperties.getDefaultModel();
    }

    /** Ajoute la réponse LLM (assistant) + l'Observation (user) pour le prochain tour. */
    private List<ChatMessageDto> appendForReact(List<ChatMessageDto> messages,
                                                String llmReply, String observation) {
        List<ChatMessageDto> next = new ArrayList<>(messages.size() + 2);
        next.addAll(messages);
        next.add(ChatMessageDto.assistant(llmReply));
        next.add(ChatMessageDto.user("Observation: " + observation));
        return next;
    }

    /**
     * Stream une itération ReAct depuis le LLM. Détecte "Final Answer:" en
     * cours de stream et bascule en émission directe de tokens vers le client.
     * Renvoie le buffer complet + un flag indiquant si on a déjà streamé la FA.
     */
    private StreamResult streamOneIteration(String modelOverride,
                                            List<ChatMessageDto> messages,
                                            List<String> stopSeqs,
                                            java.util.function.Consumer<AgentEvent> emit) {
        final String marker = "Final Answer:";
        final StringBuilder buffer = new StringBuilder();
        final StringBuilder pending = new StringBuilder();
        final boolean[] inFinalAnswer = {false};

        llmService.streamChat(messages, modelOverride, stopSeqs)
                .doOnNext(delta -> handleDelta(delta, buffer, pending, marker, inFinalAnswer, emit))
                .blockLast();

        // Flush résiduel : si on était passé en mode FA et qu'il reste des chars dans `pending`,
        // on les émet maintenant.
        if (inFinalAnswer[0] && pending.length() > 0) {
            emit.accept(new AgentEvent.Token(pending.toString()));
            pending.setLength(0);
        }
        return new StreamResult(buffer.toString(), inFinalAnswer[0]);
    }

    /**
     * Seuil après lequel on décide si le LLM est en mode ReAct ou en mode
     * réponse directe. Si après ce nombre de chars on n'a vu ni "Thought:" ni
     * "Action:" ni "Final Answer:", on suppose une réponse directe et on
     * commence à streamer immédiatement (cas des modèles non ReAct-aware comme
     * dolphin-mistral, mistral:7b-instruct, etc.).
     */
    private static final int DIRECT_MODE_DETECTION_CHARS = 60;

    private void handleDelta(ChatDelta delta, StringBuilder buffer, StringBuilder pending,
                             String marker, boolean[] inFinalAnswer,
                             java.util.function.Consumer<AgentEvent> emit) {
        String chunk = delta.content();
        if (chunk == null || chunk.isEmpty()) return;
        buffer.append(chunk);

        if (inFinalAnswer[0]) {
            // Mode streaming actif : émission token par token au client
            emit.accept(new AgentEvent.Token(chunk));
            return;
        }

        // Surveille le marker "Final Answer:" — déclenche le mode stream
        int idx = buffer.indexOf(marker);
        if (idx >= 0) {
            inFinalAnswer[0] = true;
            String afterMarker = buffer.substring(idx + marker.length());
            int j = 0;
            while (j < afterMarker.length() && Character.isWhitespace(afterMarker.charAt(j))) j++;
            String toEmit = afterMarker.substring(j);
            if (!toEmit.isEmpty()) {
                emit.accept(new AgentEvent.Token(toEmit));
            }
            return;
        }

        // Heuristique : si après quelques chars on ne voit AUCUN marqueur ReAct
        // (Thought:/Action:), on est probablement face à une réponse directe
        // (modèle non-ReAct comme dolphin) → on bascule en stream immédiat
        if (buffer.length() >= DIRECT_MODE_DETECTION_CHARS) {
            String head = buffer.substring(0, Math.min(buffer.length(), 200));
            String lower = head.toLowerCase(Locale.ROOT);
            if (!lower.contains("thought:") && !lower.contains("action:")) {
                inFinalAnswer[0] = true;
                emit.accept(new AgentEvent.Token(buffer.toString()));
            }
        }
        // Sinon on continue d'accumuler en silence (probablement Thought / Action / Action Input)
    }

    /** Résultat d'une itération de stream LLM. */
    private record StreamResult(String buffer, boolean streamedFinalAnswer) {}

    /** Sérialise un {@link ToolResult} en texte raisonnable pour le prochain prompt. */
    private String renderObservation(ToolResult result) {
        if (!result.success()) {
            return "error: " + (result.summary() == null ? "" : result.summary());
        }
        try {
            String json = result.data() == null
                    ? result.summary()
                    : objectMapper.writeValueAsString(result.data());
            if (json != null && json.length() > 4000) {
                json = json.substring(0, 4000) + " ... [truncated]";
            }
            return json;
        } catch (Exception ex) {
            return result.summary();
        }
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() <= 300 ? s : s.substring(0, 297) + "...";
    }
}
