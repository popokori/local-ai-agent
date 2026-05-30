package mr.popo.localaiagent.memory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.llm.dto.ChatMessageDto;
import mr.popo.localaiagent.llm.service.LlmService;
import mr.popo.localaiagent.memory.domain.MemoryEntry;
import mr.popo.localaiagent.memory.domain.MemoryKind;
import mr.popo.localaiagent.memory.domain.UserFact;
import mr.popo.localaiagent.memory.repository.MemoryEntryRepository;
import mr.popo.localaiagent.memory.repository.UserFactRepository;
import mr.popo.localaiagent.rag.vector.VectorCodec;
import mr.popo.localaiagent.worker.client.PythonWorkerClient;
import mr.popo.localaiagent.worker.dto.EmbedResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extrait automatiquement des "faits" + un résumé épisodique d'un échange
 * USER/ASSISTANT, et les persiste dans la mémoire long terme.
 * <p>
 * Lancé en arrière-plan ({@code @Async}) à la fin de chaque tour, pour ne pas
 * pénaliser la latence perçue. Le coût en tokens reste modéré (un appel LLM
 * supplémentaire par échange).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryConsolidator {

    private final LlmService llmService;
    private final PythonWorkerClient workerClient;
    private final UserFactRepository factRepository;
    private final MemoryEntryRepository entryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Toggle d'activation de la consolidation auto. Quand false, le LLM n'extrait
     * plus de faits/résumés à chaque échange — la mémoire ne s'enrichit que via
     * les endpoints REST manuels POST /memory/facts.
     * <p>
     * Par défaut activé. À désactiver dans application-windows-local.yml ou via
     * env {@code AGENT_MEMORY_CONSOLIDATE=false} si on observe une pollution
     * (faits non pertinents, refus du LLM par over-fitting du contexte, etc.).
     */
    @Value("${app.agent.memory-consolidate-enabled:true}")
    private boolean autoConsolidateEnabled;

    private static final String EXTRACTION_PROMPT = """
            Tu es un extracteur. À partir de l'échange utilisateur/assistant suivant,
            renvoie STRICTEMENT un JSON conforme à ce schéma :

            {
              "facts":   [ {"key": "...", "value": "...", "confidence": 0.0-1.0 } ],
              "summary": "résumé court (1-2 phrases) de l'échange à la 3e personne",
              "importance": 0.0-1.0
            }

            Règles :
            - facts : 0 à 5 faits structurés sur l'utilisateur (métier, langue préférée,
              préférences techniques, projets en cours, etc.). N'inclus PAS de faits
              généraux ni d'opinions sur des tiers.
            - Si rien d'intéressant à apprendre sur l'utilisateur, renvoie facts: [].
            - importance reflète l'utilité future (0.2 = banal, 0.8 = précieux).
            - Pas de texte avant ni après le JSON. Pas de balises de code.

            Échange :
            """;

    @Async("applicationTaskExecutor")
    @Transactional
    public void consolidate(Long userId, Long sessionId, String userMessage, String assistantReply) {
        if (!autoConsolidateEnabled) {
            log.debug("Auto-consolidation disabled (session={}) — skipping", sessionId);
            return;
        }
        if (userId == null || userMessage == null || assistantReply == null) return;
        if (userMessage.isBlank() || assistantReply.isBlank()) return;

        String exchange = "User: " + truncate(userMessage, 1500)
                + "\nAssistant: " + truncate(assistantReply, 2000);

        String llmText;
        try {
            llmText = llmService.simpleChat(
                    List.of(ChatMessageDto.user(EXTRACTION_PROMPT + "\n" + exchange)),
                    null, null).firstContent();
        } catch (Exception ex) {
            log.warn("Memory consolidation LLM call failed: {}", ex.getMessage());
            return;
        }
        if (llmText == null || llmText.isBlank()) return;

        Extraction ex = parseExtraction(llmText);
        if (ex == null) {
            log.debug("Memory extraction returned no parseable JSON for session {}", sessionId);
            return;
        }

        // 1. Persiste les faits structurés (upsert par clé)
        for (Extraction.Fact f : ex.facts) {
            if (f.key == null || f.key.isBlank() || f.value == null || f.value.isBlank()) continue;
            UserFact fact = factRepository.findByUserIdAndFactKey(userId, normalizeKey(f.key))
                    .orElseGet(UserFact::new);
            fact.setUserId(userId);
            fact.setFactKey(normalizeKey(f.key));
            fact.setFactValue(f.value);
            fact.setConfidence(f.confidence != null ? f.confidence : 0.7f);
            fact.setSource("auto");
            factRepository.save(fact);
        }

        // 2. Persiste le résumé épisodique avec embedding
        if (ex.summary != null && !ex.summary.isBlank()) {
            try {
                EmbedResponse er = workerClient.embed(List.of(ex.summary), null);
                if (er != null && er.embeddings() != null && !er.embeddings().isEmpty()) {
                    float[] vec = toFloatArray(er.embeddings().get(0));
                    MemoryEntry entry = new MemoryEntry();
                    entry.setUserId(userId);
                    entry.setKind(MemoryKind.EPISODIC);
                    entry.setSummary(ex.summary);
                    entry.setSourceSessionId(sessionId);
                    entry.setImportance(ex.importance != null ? ex.importance : 0.5f);
                    entry.setEmbedding(VectorCodec.encode(vec));
                    entry.setEmbeddingDim(vec.length);
                    entryRepository.save(entry);
                }
            } catch (Exception ee) {
                log.warn("Episodic embedding failed: {}", ee.getMessage());
            }
        }

        log.debug("Consolidated session {}: {} fact(s), summary={}",
                sessionId, ex.facts.size(), ex.summary != null);
    }

    private static String normalizeKey(String k) {
        return k.trim().toLowerCase().replaceAll("[^a-z0-9._-]+", "_");
    }

    private Extraction parseExtraction(String text) {
        String s = text.trim();
        // Supprime les éventuelles balises ```...```
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) s = s.substring(firstNl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3).trim();
        }
        // Cherche le premier { jusqu'au dernier }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        String json = s.substring(start, end + 1);

        try {
            JsonNode root = objectMapper.readTree(json);
            Extraction out = new Extraction();
            JsonNode facts = root.path("facts");
            if (facts.isArray()) {
                for (JsonNode f : facts) {
                    Extraction.Fact fact = new Extraction.Fact();
                    fact.key = f.path("key").asText("");
                    fact.value = f.path("value").asText("");
                    fact.confidence = f.has("confidence") && !f.path("confidence").isNull()
                            ? (float) f.path("confidence").asDouble() : null;
                    out.facts.add(fact);
                }
            }
            out.summary = root.path("summary").asText("");
            if (root.has("importance") && !root.path("importance").isNull()) {
                out.importance = (float) root.path("importance").asDouble();
            }
            return out;
        } catch (Exception ex) {
            log.debug("Memory extraction parse failed: {}", ex.getMessage());
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private static class Extraction {
        List<Fact> facts = new ArrayList<>();
        String summary;
        Float importance;
        private static class Fact {
            String key;
            String value;
            Float confidence;
        }
    }

    /** Wrapper public utilisé par les tests / autres beans. */
    public Optional<Long> noOpProbe() {
        return Optional.empty();
    }
}
