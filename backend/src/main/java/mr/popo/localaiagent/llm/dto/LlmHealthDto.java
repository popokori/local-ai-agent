package mr.popo.localaiagent.llm.dto;

public record LlmHealthDto(
        String provider,
        String baseUrl,
        boolean reachable,
        boolean modelLoaded,
        String model,
        String version,
        long latencyMs,
        String error
) {}
