package mr.popo.localaiagent.memory.dto;

import java.time.OffsetDateTime;

public record UserFactDto(
        Long id,
        String factKey,
        String factValue,
        Float confidence,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
