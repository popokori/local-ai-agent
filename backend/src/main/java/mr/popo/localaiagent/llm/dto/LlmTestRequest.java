package mr.popo.localaiagent.llm.dto;

import jakarta.validation.constraints.Size;

public record LlmTestRequest(
        @Size(max = 2000) String prompt,
        @Size(max = 128) String model
) {}
