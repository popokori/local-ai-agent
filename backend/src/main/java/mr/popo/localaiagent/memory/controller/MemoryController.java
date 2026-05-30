package mr.popo.localaiagent.memory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.memory.dto.MemoryEntryDto;
import mr.popo.localaiagent.memory.dto.UpsertFactRequest;
import mr.popo.localaiagent.memory.dto.UserFactDto;
import mr.popo.localaiagent.memory.service.MemoryService;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Memory", description = "Mémoire long terme : profil + entrées")
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MemoryController {

    private final MemoryService memoryService;

    @Operation(summary = "Liste mes faits structurés")
    @GetMapping("/facts")
    public List<UserFactDto> listFacts(@AuthenticationPrincipal AppUserDetails p) {
        return memoryService.listFacts(p.getId());
    }

    @Operation(summary = "Ajoute ou met à jour un fait")
    @PostMapping("/facts")
    public UserFactDto upsertFact(@AuthenticationPrincipal AppUserDetails p,
                                  @Valid @RequestBody UpsertFactRequest request) {
        return memoryService.upsertFact(p.getId(), request, "manual");
    }

    @Operation(summary = "Supprime un fait (oubli explicite)")
    @DeleteMapping("/facts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFact(@AuthenticationPrincipal AppUserDetails p, @PathVariable Long id) {
        memoryService.deleteFact(p.getId(), id);
    }

    @Operation(summary = "Liste mes entrées de mémoire (épisodique + sémantique)")
    @GetMapping("/entries")
    public List<MemoryEntryDto> listEntries(@AuthenticationPrincipal AppUserDetails p) {
        return memoryService.listEntries(p.getId());
    }

    @Operation(summary = "Supprime une entrée de mémoire")
    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(@AuthenticationPrincipal AppUserDetails p, @PathVariable Long id) {
        memoryService.deleteEntry(p.getId(), id);
    }
}
