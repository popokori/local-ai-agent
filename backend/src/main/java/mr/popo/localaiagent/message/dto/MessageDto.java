package mr.popo.localaiagent.message.dto;

import mr.popo.localaiagent.message.domain.MessageRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageDto(
        Long id,
        Long sessionId,
        MessageRole role,
        String content,
        Integer tokensIn,
        Integer tokensOut,
        Integer latencyMs,
        UUID clientRequestId,
        OffsetDateTime createdAt
) {}
