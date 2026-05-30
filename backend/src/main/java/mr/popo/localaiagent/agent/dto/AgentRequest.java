package mr.popo.localaiagent.agent.dto;

import mr.popo.localaiagent.chat.domain.ChatMode;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;

import java.util.List;

public record AgentRequest(
        Long sessionId,
        Long userId,
        ChatMode mode,
        String userMessage,
        List<ChatMessageDto> history,
        String modelOverride,
        Long knowledgeBaseId
) {}
