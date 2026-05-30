package mr.popo.localaiagent.llm.service;

import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.llm.client.LlmClient;
import mr.popo.localaiagent.llm.config.LlmProperties;
import mr.popo.localaiagent.llm.dto.ChatCompletionRequest;
import mr.popo.localaiagent.llm.dto.ChatCompletionResponse;
import mr.popo.localaiagent.llm.dto.ChatDelta;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;
import mr.popo.localaiagent.llm.dto.LlmHealthDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Façade applicative au-dessus de {@link LlmClient} — choisit le modèle
 * selon le contexte, expose des méthodes simples au reste du backend.
 */
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmClient client;
    private final LlmProperties props;

    public Flux<ChatDelta> streamChat(List<ChatMessageDto> messages, String modelName) {
        return streamChat(messages, modelName, null);
    }

    public Flux<ChatDelta> streamChat(List<ChatMessageDto> messages, String modelName, List<String> stop) {
        ChatCompletionRequest req = new ChatCompletionRequest(
                resolveModel(modelName), messages, Boolean.TRUE, null, null, stop);
        return client.streamChat(req);
    }

    public ChatCompletionResponse simpleChat(List<ChatMessageDto> messages, String modelName) {
        return simpleChat(messages, modelName, null);
    }

    /**
     * Version avec stop sequences. Indispensable pour ReAct : on force le LLM
     * à s'arrêter dès qu'il écrit "Observation:" sinon il hallucine la suite.
     */
    public ChatCompletionResponse simpleChat(List<ChatMessageDto> messages, String modelName,
                                             List<String> stop) {
        ChatCompletionRequest req = new ChatCompletionRequest(
                resolveModel(modelName), messages, Boolean.FALSE, null, null, stop);
        return client.chat(req);
    }

    public LlmHealthDto health() {
        return client.health();
    }

    public String defaultModel() {
        return props.getDefaultModel();
    }

    private String resolveModel(String requested) {
        if (requested != null && !requested.isBlank()) return requested;
        return props.getDefaultModel();
    }
}
