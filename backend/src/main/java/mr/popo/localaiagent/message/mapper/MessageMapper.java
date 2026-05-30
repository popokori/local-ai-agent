package mr.popo.localaiagent.message.mapper;

import mr.popo.localaiagent.message.domain.Message;
import mr.popo.localaiagent.message.dto.MessageDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    MessageDto toDto(Message message);
}
