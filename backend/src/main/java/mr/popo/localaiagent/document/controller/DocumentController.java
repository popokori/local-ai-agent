package mr.popo.localaiagent.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.common.pagination.PageResponse;
import mr.popo.localaiagent.document.dto.DocumentDto;
import mr.popo.localaiagent.document.service.DocumentService;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Documents")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload d'un document dans une KB (ingestion async)")
    @PostMapping(value = "/kbs/{kbId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentDto upload(@AuthenticationPrincipal AppUserDetails principal,
                              @PathVariable Long kbId,
                              @RequestPart("file") MultipartFile file) {
        return documentService.upload(principal.getId(), kbId, file);
    }

    @Operation(summary = "Liste paginée des documents d'une KB")
    @GetMapping("/kbs/{kbId}/documents")
    public PageResponse<DocumentDto> list(@AuthenticationPrincipal AppUserDetails principal,
                                          @PathVariable Long kbId,
                                          Pageable pageable) {
        return documentService.list(principal.getId(), kbId, pageable);
    }

    @Operation(summary = "Détail + statut d'ingestion d'un document")
    @GetMapping("/documents/{id}")
    public DocumentDto get(@AuthenticationPrincipal AppUserDetails principal,
                           @PathVariable Long id) {
        return documentService.get(principal.getId(), id);
    }

    @Operation(summary = "Supprime un document (vector store + DB + fichier disque)")
    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable Long id) {
        documentService.delete(principal.getId(), id);
    }
}
