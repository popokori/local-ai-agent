package mr.popo.localaiagent.llm.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.llm.config.LlmProperties;
import mr.popo.localaiagent.llm.dto.ChatCompletionRequest;
import mr.popo.localaiagent.llm.dto.ChatCompletionResponse;
import mr.popo.localaiagent.llm.dto.ChatDelta;
import mr.popo.localaiagent.llm.dto.LlmHealthDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Client LLM compatible OpenAI : fonctionne avec Ollama (http://host:11434/v1)
 * et vLLM (http://host:8000/v1) sans changement de code.
 * <p>
 * Phase 1 : pas de Resilience4j (retry/CB) — uniquement le timeout WebClient
 * configuré dans WebClientConfig. Voir docs/PHASE_ROADMAP.md.
 */
@Slf4j
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final WebClient web;
    private final WebClient nativeWeb;
    private final LlmProperties props;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(WebClient.Builder builder,
                                     LlmProperties props,
                                     ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;

        WebClient.Builder shared = builder.clone()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            shared.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey());
        }
        this.web = shared.baseUrl(props.getBaseUrl()).build();
        this.nativeWeb = builder.clone().baseUrl(props.getNativeBaseUrl()).build();
    }

    @Override
    public Flux<ChatDelta> streamChat(ChatCompletionRequest request) {
        ChatCompletionRequest req = new ChatCompletionRequest(
                request.model() != null ? request.model() : props.getDefaultModel(),
                request.messages(),
                Boolean.TRUE,
                request.temperature() != null ? request.temperature() : props.getTemperature(),
                request.maxTokens() != null ? request.maxTokens() : props.getMaxTokens(),
                request.stop());

        return web.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(req)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .mapNotNull(this::parseChunk)
                .takeUntil(ChatDelta::isEnd);
    }

    @Override
    public ChatCompletionResponse chat(ChatCompletionRequest request) {
        ChatCompletionRequest req = new ChatCompletionRequest(
                request.model() != null ? request.model() : props.getDefaultModel(),
                request.messages(),
                Boolean.FALSE,
                request.temperature() != null ? request.temperature() : props.getTemperature(),
                request.maxTokens() != null ? request.maxTokens() : props.getMaxTokens(),
                request.stop());
        return web.post()
                .uri("/chat/completions")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .block();
    }

    @Override
    public LlmHealthDto health() {
        long start = System.currentTimeMillis();
        try {
            // Ollama : GET /api/tags → liste des modèles
            // vLLM   : GET /v1/models → idem
            String path = "ollama".equalsIgnoreCase(props.getProvider()) ? "/api/tags" : "/v1/models";
            String body = nativeWeb.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            long latency = System.currentTimeMillis() - start;

            boolean modelLoaded = body != null && body.contains(stripTag(props.getDefaultModel()));
            return new LlmHealthDto(props.getProvider(), props.getBaseUrl(),
                    true, modelLoaded, props.getDefaultModel(), null, latency, null);
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.warn("LLM health check failed: {}", ex.getMessage());
            return new LlmHealthDto(props.getProvider(), props.getBaseUrl(),
                    false, false, props.getDefaultModel(), null, latency, ex.getMessage());
        }
    }

    /**
     * Parse un chunk SSE OpenAI : "data: {...}" → ChatDelta.
     * "data: [DONE]" → ChatDelta.end("stop").
     */
    private ChatDelta parseChunk(String chunk) {
        if (chunk == null || chunk.isBlank()) return null;
        String payload = chunk.startsWith("data:") ? chunk.substring(5).trim() : chunk.trim();
        if (payload.isEmpty()) return null;
        if ("[DONE]".equals(payload)) return ChatDelta.end("stop");

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;
            JsonNode first = choices.get(0);
            String finish = first.hasNonNull("finish_reason") ? first.get("finish_reason").asText() : null;
            JsonNode delta = first.path("delta");
            String content = delta.path("content").asText("");
            if (finish != null) {
                return new ChatDelta(content.isEmpty() ? null : content, finish);
            }
            return content.isEmpty() ? null : ChatDelta.token(content);
        } catch (JsonProcessingException e) {
            log.debug("Skipping unparseable SSE chunk: {}", payload);
            return null;
        }
    }

    private String stripTag(String modelName) {
        if (modelName == null) return "";
        int idx = modelName.indexOf(':');
        return idx > 0 ? modelName.substring(0, idx) : modelName;
    }
}
