package mr.popo.localaiagent.memory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.common.exception.ResourceNotFoundException;
import mr.popo.localaiagent.memory.domain.MemoryEntry;
import mr.popo.localaiagent.memory.domain.MemoryKind;
import mr.popo.localaiagent.memory.domain.UserFact;
import mr.popo.localaiagent.memory.dto.MemoryEntryDto;
import mr.popo.localaiagent.memory.dto.UpsertFactRequest;
import mr.popo.localaiagent.memory.dto.UserFactDto;
import mr.popo.localaiagent.memory.repository.MemoryEntryRepository;
import mr.popo.localaiagent.memory.repository.UserFactRepository;
import mr.popo.localaiagent.rag.vector.VectorCodec;
import mr.popo.localaiagent.worker.client.PythonWorkerClient;
import mr.popo.localaiagent.worker.dto.EmbedResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final UserFactRepository factRepository;
    private final MemoryEntryRepository entryRepository;
    private final PythonWorkerClient workerClient;

    // ─── Profile ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<UserFactDto> listFacts(Long userId) {
        return factRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(MemoryServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public UserFactDto upsertFact(Long userId, UpsertFactRequest request, String source) {
        UserFact fact = factRepository.findByUserIdAndFactKey(userId, request.factKey())
                .orElseGet(UserFact::new);
        fact.setUserId(userId);
        fact.setFactKey(request.factKey());
        fact.setFactValue(request.factValue());
        if (request.confidence() != null) fact.setConfidence(request.confidence());
        if (source != null) fact.setSource(source);
        fact = factRepository.save(fact);
        return toDto(fact);
    }

    @Override
    @Transactional
    public void deleteFact(Long userId, Long factId) {
        UserFact fact = factRepository.findByIdAndUserId(factId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("UserFact", factId));
        factRepository.delete(fact);
    }

    // ─── Entries ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntryDto> listEntries(Long userId) {
        return entryRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(MemoryEntry::getCreatedAt).reversed())
                .map(MemoryServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteEntry(Long userId, Long entryId) {
        MemoryEntry e = entryRepository.findById(entryId).orElse(null);
        if (e == null || !e.getUserId().equals(userId)) {
            throw ResourceNotFoundException.of("MemoryEntry", entryId);
        }
        entryRepository.delete(e);
    }

    @Override
    @Transactional
    public List<MemoryEntryDto> recall(Long userId, String query, int topK) {
        List<MemoryEntry> all = entryRepository.findAllByUserId(userId);
        if (all.isEmpty() || query == null || query.isBlank()) return List.of();

        EmbedResponse embed = workerClient.embed(List.of(query), null);
        if (embed == null || embed.embeddings() == null || embed.embeddings().isEmpty()) return List.of();
        float[] q = toFloatArray(embed.embeddings().get(0));
        float qNorm = norm(q);
        if (qNorm == 0f) return List.of();

        record Scored(MemoryEntry entry, double score) {}
        List<Scored> scored = new ArrayList<>(all.size());
        for (MemoryEntry e : all) {
            float[] v = VectorCodec.decode(e.getEmbedding());
            if (v.length != q.length) continue;
            scored.add(new Scored(e, cosine(q, qNorm, v)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        List<Scored> top = scored.size() <= topK ? scored : scored.subList(0, topK);

        OffsetDateTime now = OffsetDateTime.now();
        for (Scored s : top) s.entry().setLastAccessedAt(now);

        return top.stream().map(s -> toDto(s.entry())).toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private static UserFactDto toDto(UserFact f) {
        return new UserFactDto(f.getId(), f.getFactKey(), f.getFactValue(),
                f.getConfidence(), f.getSource(), f.getCreatedAt(), f.getUpdatedAt());
    }

    private static MemoryEntryDto toDto(MemoryEntry e) {
        return new MemoryEntryDto(e.getId(), e.getKind(), e.getSummary(),
                e.getSourceSessionId(), e.getImportance(), e.getCreatedAt(), e.getLastAccessedAt());
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private static double cosine(float[] q, float qNorm, float[] v) {
        double dot = 0.0, vSq = 0.0;
        for (int i = 0; i < q.length; i++) {
            dot += (double) q[i] * v[i];
            vSq += (double) v[i] * v[i];
        }
        double vNorm = Math.sqrt(vSq);
        if (vNorm == 0.0) return 0.0;
        return dot / ((double) qNorm * vNorm);
    }

    private static float norm(float[] v) {
        double s = 0.0;
        for (float x : v) s += (double) x * x;
        return (float) Math.sqrt(s);
    }
}
