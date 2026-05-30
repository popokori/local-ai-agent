package mr.popo.localaiagent.document.mapper;

import mr.popo.localaiagent.document.domain.Document;
import mr.popo.localaiagent.document.domain.KnowledgeBase;
import mr.popo.localaiagent.document.dto.DocumentDto;
import mr.popo.localaiagent.document.dto.KnowledgeBaseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    KnowledgeBaseDto toDto(KnowledgeBase kb);

    DocumentDto toDto(Document doc);
}
