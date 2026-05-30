package mr.popo.localaiagent.llm.dto;

public record LlmTestResponse(
        String model,
        String reply,
        Integer tokensOut,
        long latencyMs
) {}
