package mr.popo.localaiagent.memory.service;

import mr.popo.localaiagent.memory.dto.MemoryEntryDto;
import mr.popo.localaiagent.memory.dto.UpsertFactRequest;
import mr.popo.localaiagent.memory.dto.UserFactDto;

import java.util.List;

/**
 * Mémoire long terme — 3 couches :
 * <ul>
 *   <li><b>Profile</b> : faits structurés clé/valeur (langue, métier, préférences…).</li>
 *   <li><b>Episodic</b> : résumés des sessions passées (recall via cosinus).</li>
 *   <li><b>Semantic</b> : connaissances libres apprises (recall via cosinus).</li>
 * </ul>
 * Les couches 2 et 3 partagent la table {@code memory_entries}, distinguées par {@code kind}.
 */
public interface MemoryService {

    // ─── Profile (UserFact) ───────────────────────────────────────────

    List<UserFactDto> listFacts(Long userId);

    UserFactDto upsertFact(Long userId, UpsertFactRequest request, String source);

    void deleteFact(Long userId, Long factId);

    // ─── Entries (épisodique + sémantique) ───────────────────────────

    List<MemoryEntryDto> listEntries(Long userId);

    void deleteEntry(Long userId, Long entryId);

    /**
     * Récupère les top-K entrées les plus pertinentes pour la requête courante.
     * Met à jour {@code lastAccessedAt} comme effet de bord.
     */
    List<MemoryEntryDto> recall(Long userId, String query, int topK);
}
