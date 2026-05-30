package mr.popo.localaiagent.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertFactRequest(
        @NotBlank @Size(max = 128) String factKey,
        @NotBlank @Size(max = 4000) String factValue,
        Float confidence
) {}
