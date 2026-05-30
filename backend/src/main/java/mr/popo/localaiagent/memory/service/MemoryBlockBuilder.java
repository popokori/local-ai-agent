package mr.popo.localaiagent.memory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.memory.dto.MemoryEntryDto;
import mr.popo.localaiagent.memory.dto.UserFactDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Compose le bloc de mémoire injecté dans le system prompt :
 *   - section "Profil" (UserFacts triés par date de mise à jour)
 *   - section "Souvenirs pertinents" (entrées épisodiques rappelées via cosinus)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryBlockBuilder {

    private final MemoryService memoryService;

    @Value("${app.memory.recall-top-k:3}")
    private int recallTopK;

    @Value("${app.memory.max-profile-facts:12}")
    private int maxProfileFacts;

    public String build(Long userId, String userMessage) {
        if (userId == null) return null;
        StringBuilder sb = new StringBuilder();

        List<UserFactDto> facts = memoryService.listFacts(userId);
        if (!facts.isEmpty()) {
            sb.append("## Profil utilisateur\n");
            int n = 0;
            for (UserFactDto f : facts) {
                if (n++ >= maxProfileFacts) break;
                sb.append("- ").append(f.factKey()).append(" : ").append(f.factValue()).append("\n");
            }
        }

        try {
            List<MemoryEntryDto> recalled = memoryService.recall(userId, userMessage, recallTopK);
            if (!recalled.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("## Souvenirs pertinents (échanges passés)\n");
                for (MemoryEntryDto e : recalled) {
                    sb.append("- ").append(e.summary()).append("\n");
                }
            }
        } catch (Exception ex) {
            log.debug("Memory recall skipped: {}", ex.getMessage());
        }

        return sb.length() == 0 ? null : sb.toString();
    }
}
