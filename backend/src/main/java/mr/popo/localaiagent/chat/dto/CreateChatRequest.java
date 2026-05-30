package mr.popo.localaiagent.chat.dto;

import jakarta.validation.constraints.Size;
import mr.popo.localaiagent.chat.domain.ChatMode;

public record CreateChatRequest(
        @Size(max = 255) String title,
        ChatMode mode,
        @Size(max = 128) String modelName,
        Long knowledgeBaseId
) {}
