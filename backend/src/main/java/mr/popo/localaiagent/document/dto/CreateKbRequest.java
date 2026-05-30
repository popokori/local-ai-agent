package mr.popo.localaiagent.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mr.popo.localaiagent.science.api.Domain;

public record CreateKbRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        Domain domain
) {}
