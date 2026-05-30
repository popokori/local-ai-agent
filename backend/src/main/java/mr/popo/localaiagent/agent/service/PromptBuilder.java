package mr.popo.localaiagent.agent.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.chat.domain.ChatMode;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Construit la liste de messages envoyée au LLM. Blocs optionnels (dans l'ordre) :
 * <ol>
 *   <li>prompt normal (toujours)</li>
 *   <li>note de mode (si EXPERT/FACT_CHECK)</li>
 *   <li>disclaimer médical (toujours sauf {@code app.agent.medical-disclaimer-enabled=false})</li>
 *   <li>mémoire long terme (faits + résumés pertinents, si présents)</li>
 *   <li>contexte RAG (si KB attachée + hits)</li>
 *   <li>protocole ReAct + catalogue d'outils (si tools enregistrés)</li>
 * </ol>
 * Suivent l'historique fenêtré, puis le message utilisateur courant.
 */
@Slf4j
@Component
public class PromptBuilder {

    @Value("${app.agent.medical-disclaimer-enabled:true}")
    private boolean disclaimerEnabled;

    @Value("${app.rag.max-context-chars:6000}")
    private int maxContextChars;

    private String normalPrompt;
    private String medicalDisclaimer;

    @PostConstruct
    void load() throws IOException {
        normalPrompt = readResource("prompts/system_normal.md");
        medicalDisclaimer = readResource("prompts/system_medical_disclaimer.md");
    }

    public List<ChatMessageDto> build(ChatMode mode,
                                      List<ChatMessageDto> history,
                                      String userMessage) {
        return build(mode, history, userMessage, null, null, null);
    }

    public List<ChatMessageDto> build(ChatMode mode,
                                      List<ChatMessageDto> history,
                                      String userMessage,
                                      String ragContextBlock) {
        return build(mode, history, userMessage, ragContextBlock, null, null);
    }

    public List<ChatMessageDto> build(ChatMode mode,
                                      List<ChatMessageDto> history,
                                      String userMessage,
                                      String ragContextBlock,
                                      String toolProtocolBlock) {
        return build(mode, history, userMessage, ragContextBlock, toolProtocolBlock, null);
    }

    /**
     * @param toolProtocolBlock instructions ReAct + liste des outils ; null si pas de tools.
     * @param memoryBlock       bloc texte "faits + souvenirs" injecté avant le contexte RAG.
     */
    public List<ChatMessageDto> build(ChatMode mode,
                                      List<ChatMessageDto> history,
                                      String userMessage,
                                      String ragContextBlock,
                                      String toolProtocolBlock,
                                      String memoryBlock) {
        List<ChatMessageDto> messages = new ArrayList<>();
        messages.add(ChatMessageDto.system(buildSystemPrompt(mode, ragContextBlock, toolProtocolBlock, memoryBlock)));
        if (history != null) messages.addAll(history);
        messages.add(ChatMessageDto.user(userMessage));
        return messages;
    }

    private String buildSystemPrompt(ChatMode mode, String ragContextBlock,
                                     String toolProtocolBlock, String memoryBlock) {
        StringBuilder sb = new StringBuilder(normalPrompt);
        if (mode != null && mode != ChatMode.NORMAL) {
            appendModeNote(sb, mode);
        }
        if (disclaimerEnabled) {
            sb.append("\n\n---\n").append(medicalDisclaimer);
        }
        if (memoryBlock != null && !memoryBlock.isBlank()) {
            sb.append("\n\n---\n").append(memoryBlock);
        }
        if (ragContextBlock != null && !ragContextBlock.isBlank()) {
            String trimmed = ragContextBlock.length() > maxContextChars
                    ? ragContextBlock.substring(0, maxContextChars) + "\n[contexte tronqué]"
                    : ragContextBlock;
            sb.append("\n\n---\n").append(trimmed);
        }
        if (toolProtocolBlock != null && !toolProtocolBlock.isBlank()) {
            sb.append(toolProtocolBlock);
        }
        return sb.toString();
    }

    private static void appendModeNote(StringBuilder sb, ChatMode mode) {
        switch (mode) {
            case EXPERT -> sb.append("""

                    ## Mode EXPERT
                    L'utilisateur souhaite une réponse détaillée et rigoureuse :
                    - raisonne explicitement étape par étape avant de conclure ;
                    - inclus les hypothèses, unités, et limites éventuelles ;
                    - cite tes sources lorsque tu en as ;
                    - reste concis : pas de répétition, pas de paraphrase inutile.
                    """);
            case FACT_CHECK -> sb.append("""

                    ## Mode FACT_CHECK
                    Avant de fournir la réponse, vérifie chaque affirmation factuelle
                    significative. Si tu n'es pas certain d'un fait, annote-le explicitement
                    par "[incertain]". Sinon, annote "[vérifié]" quand tu disposes d'une source.
                    """);
            default -> { /* NORMAL : rien à ajouter */ }
        }
    }

    private String readResource(String path) throws IOException {
        try (var is = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }
}
