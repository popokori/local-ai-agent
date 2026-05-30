package mr.popo.localaiagent.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.chat.dto.ChatSessionDto;
import mr.popo.localaiagent.chat.dto.CreateChatRequest;
import mr.popo.localaiagent.chat.dto.UpdateChatRequest;
import mr.popo.localaiagent.chat.service.ChatService;
import mr.popo.localaiagent.common.pagination.PageResponse;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chats", description = "Sessions de conversation")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Liste paginée des sessions de l'utilisateur")
    @GetMapping
    public PageResponse<ChatSessionDto> list(@AuthenticationPrincipal AppUserDetails principal,
                                             Pageable pageable) {
        return chatService.list(principal.getId(), pageable);
    }

    @Operation(summary = "Crée une nouvelle session")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionDto create(@AuthenticationPrincipal AppUserDetails principal,
                                 @Valid @RequestBody CreateChatRequest request) {
        return chatService.create(principal.getId(), request);
    }

    @Operation(summary = "Détail d'une session")
    @GetMapping("/{id}")
    public ChatSessionDto get(@AuthenticationPrincipal AppUserDetails principal,
                              @PathVariable Long id) {
        return chatService.get(id, principal.getId());
    }

    @Operation(summary = "Renommer / changer mode / changer modèle")
    @PatchMapping("/{id}")
    public ChatSessionDto update(@AuthenticationPrincipal AppUserDetails principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody UpdateChatRequest request) {
        return chatService.update(id, principal.getId(), request);
    }

    @Operation(summary = "Supprime une session (cascade messages)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable Long id) {
        chatService.delete(id, principal.getId());
    }
}
