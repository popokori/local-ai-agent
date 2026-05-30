package mr.popo.localaiagent.document.dto;

import mr.popo.localaiagent.document.domain.DocumentStatus;

import java.time.OffsetDateTime;

public record DocumentDto(
        Long id,
        Long kbId,
        String fileName,
        String mimeType,
        Long sizeBytes,
        DocumentStatus status,
        String error,
        Integer pageCount,
        Integer chunkCount,
        OffsetDateTime createdAt,
        OffsetDateTime indexedAt
) {}
