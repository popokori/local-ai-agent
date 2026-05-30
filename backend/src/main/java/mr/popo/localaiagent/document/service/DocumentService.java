package mr.popo.localaiagent.document.service;

import mr.popo.localaiagent.common.pagination.PageResponse;
import mr.popo.localaiagent.document.dto.DocumentDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentDto upload(Long ownerId, Long kbId, MultipartFile file);

    PageResponse<DocumentDto> list(Long ownerId, Long kbId, Pageable pageable);

    DocumentDto get(Long ownerId, Long documentId);

    void delete(Long ownerId, Long documentId);
}
