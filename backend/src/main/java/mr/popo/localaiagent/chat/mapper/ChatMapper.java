package mr.popo.localaiagent.chat.mapper;

import mr.popo.localaiagent.chat.domain.ChatSession;
import mr.popo.localaiagent.chat.dto.ChatSessionDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    ChatSessionDto toDto(ChatSession session);
}
