package mr.popo.localaiagent.memory.dto;

import mr.popo.localaiagent.memory.domain.MemoryKind;

import java.time.OffsetDateTime;

public record MemoryEntryDto(
        Long id,
        MemoryKind kind,
        String summary,
        Long sourceSessionId,
        Float importance,
        OffsetDateTime createdAt,
        OffsetDateTime lastAccessedAt
) {}
