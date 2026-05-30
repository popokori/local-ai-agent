package mr.popo.localaiagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<ChatMessageDto> messages,
        Boolean stream,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        List<String> stop
) {
    public ChatCompletionRequest(String model, List<ChatMessageDto> messages, Boolean stream,
                                 Double temperature, Integer maxTokens) {
        this(model, messages, stream, temperature, maxTokens, null);
    }
}
