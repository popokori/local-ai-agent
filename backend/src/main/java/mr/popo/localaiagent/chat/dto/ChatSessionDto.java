package mr.popo.localaiagent.chat.dto;

import mr.popo.localaiagent.chat.domain.ChatMode;

import java.time.OffsetDateTime;

public record ChatSessionDto(
        Long id,
        String title,
        ChatMode mode,
        String modelName,
        Long knowledgeBaseId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastMessageAt
) {}
