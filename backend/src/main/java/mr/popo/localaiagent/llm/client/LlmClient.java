package mr.popo.localaiagent.llm.client;

import mr.popo.localaiagent.llm.dto.ChatCompletionRequest;
import mr.popo.localaiagent.llm.dto.ChatCompletionResponse;
import mr.popo.localaiagent.llm.dto.ChatDelta;
import mr.popo.localaiagent.llm.dto.LlmHealthDto;
import reactor.core.publisher.Flux;

/**
 * Abstraction du serveur LLM. Une seule impl en Phase 1 :
 * {@link OpenAiCompatibleLlmClient} (Ollama et vLLM exposent tous deux le
 * format OpenAI). Pour ajouter un autre provider, créer une autre impl
 * et l'enregistrer comme bean primaire.
 */
public interface LlmClient {

    /** Streaming SSE — chaque {@link ChatDelta} porte un fragment ou un finish_reason. */
    Flux<ChatDelta> streamChat(ChatCompletionRequest request);

    /** Appel non-stream (utilisé par /llm/test). */
    ChatCompletionResponse chat(ChatCompletionRequest request);

    /** Diagnostic — ping du serveur + vérification du modèle chargé. */
    LlmHealthDto health();
}
