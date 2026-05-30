package mr.popo.localaiagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Message au format OpenAI ({"role": "user|assistant|system", "content": "..."}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageDto(String role, String content) {

    public static ChatMessageDto system(String content) {
        return new ChatMessageDto("system", content);
    }

    public static ChatMessageDto user(String content) {
        return new ChatMessageDto("user", content);
    }

    public static ChatMessageDto assistant(String content) {
        return new ChatMessageDto("assistant", content);
    }
}
