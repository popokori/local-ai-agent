package mr.popo.localaiagent.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.audit.domain.AuditAction;
import mr.popo.localaiagent.audit.service.AuditService;
import mr.popo.localaiagent.common.pagination.PageResponse;
import mr.popo.localaiagent.message.dto.MessageDto;
import mr.popo.localaiagent.message.dto.SendMessageRequest;
import mr.popo.localaiagent.message.service.MessageService;
import mr.popo.localaiagent.security.userdetails.AppUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "Messages")
@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;
    private final AuditService auditService;

    @Operation(summary = "Historique paginé des messages d'une session")
    @GetMapping
    public PageResponse<MessageDto> history(@AuthenticationPrincipal AppUserDetails principal,
                                            @PathVariable Long chatId,
                                            Pageable pageable) {
        return messageService.history(chatId, principal.getId(), pageable);
    }

    @Operation(summary = "Envoie un message et stream la réponse en SSE",
            description = """
                    Retourne un flux SSE avec les événements :
                      - event: token  data: {"text": "..."}
                      - event: final  data: {"messageId": 42}
                      - event: error  data: {"message": "..."}
                    """)
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@AuthenticationPrincipal AppUserDetails principal,
                           @PathVariable Long chatId,
                           @Valid @RequestBody SendMessageRequest request) {
        auditService.log(AuditAction.MESSAGE, principal.getId(), chatId,
                Map.of("clientRequestId",
                        request.clientRequestId() == null ? "null" : request.clientRequestId().toString()),
                true, null);
        return messageService.sendAndStream(chatId, principal.getId(), request);
    }
}
