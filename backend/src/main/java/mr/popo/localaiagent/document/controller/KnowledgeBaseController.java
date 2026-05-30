package mr.popo.localaiagent.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.document.dto.CreateKbRequest;
import mr.popo.localaiagent.document.dto.KnowledgeBaseDto;
import mr.popo.localaiagent.document.service.KnowledgeBaseService;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Knowledge Bases", description = "Collections documentaires utilisateur")
@RestController
@RequestMapping("/api/v1/kbs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @Operation(summary = "Liste mes knowledge bases")
    @GetMapping
    public List<KnowledgeBaseDto> list(@AuthenticationPrincipal AppUserDetails principal) {
        return kbService.list(principal.getId());
    }

    @Operation(summary = "Crée une nouvelle KB")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeBaseDto create(@AuthenticationPrincipal AppUserDetails principal,
                                   @Valid @RequestBody CreateKbRequest request) {
        return kbService.create(principal.getId(), request);
    }

    @Operation(summary = "Détail d'une KB")
    @GetMapping("/{id}")
    public KnowledgeBaseDto get(@AuthenticationPrincipal AppUserDetails principal,
                                @PathVariable Long id) {
        return kbService.get(id, principal.getId());
    }

    @Operation(summary = "Supprime une KB (cascade docs + chunks)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable Long id) {
        kbService.delete(id, principal.getId());
    }
}
