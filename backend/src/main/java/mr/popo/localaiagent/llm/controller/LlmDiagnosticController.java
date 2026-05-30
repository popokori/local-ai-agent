package mr.popo.localaiagent.llm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.llm.dto.ChatCompletionResponse;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;
import mr.popo.localaiagent.llm.dto.LlmHealthDto;
import mr.popo.localaiagent.llm.dto.LlmTestRequest;
import mr.popo.localaiagent.llm.dto.LlmTestResponse;
import mr.popo.localaiagent.llm.service.LlmService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints diagnostic du serveur LLM — utiles pour valider la stack sans
 * passer par une session de chat (gain de temps énorme en debug).
 * <p>
 * Restreints à ADMIN/EXPERT (prod) ou tout user authentifié (dev), via
 * {@link mr.popo.localaiagent.security.config.SecurityConfig}.
 */
@Tag(name = "LLM Diagnostic")
@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LlmDiagnosticController {

    private final LlmService llmService;

    @Operation(summary = "Ping le serveur LLM et vérifie qu'un modèle est chargé")
    @GetMapping("/health")
    public LlmHealthDto health() {
        return llmService.health();
    }

    @Operation(summary = "Requête courte non-stream pour valider le pipeline")
    @PostMapping("/test")
    public LlmTestResponse test(@Valid @RequestBody(required = false) LlmTestRequest request) {
        String prompt = (request != null && request.prompt() != null) ? request.prompt() : "ping";
        String model = (request != null) ? request.model() : null;

        List<ChatMessageDto> messages = List.of(
                ChatMessageDto.system("You are a concise assistant. Reply in at most one short sentence."),
                ChatMessageDto.user(prompt));

        long start = System.currentTimeMillis();
        ChatCompletionResponse response = llmService.simpleChat(messages, model);
        long latency = System.currentTimeMillis() - start;

        Integer tokensOut = response.usage() != null ? response.usage().completionTokens() : null;
        return new LlmTestResponse(response.model(), response.firstContent(), tokensOut, latency);
    }
}
