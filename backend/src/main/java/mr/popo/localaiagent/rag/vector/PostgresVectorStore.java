package mr.popo.localaiagent.rag.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.document.domain.DocumentChunk;
import mr.popo.localaiagent.document.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Impl par défaut de {@link VectorStoreService} en Phase 2 : stockage des
 * embeddings en BYTEA dans PostgreSQL, calcul cosinus en Java au moment de
 * la recherche.
 * <p>
 * Avantages : zéro dépendance externe (pas de Qdrant, pas de pgvector),
 * cohérence transactionnelle stricte (les chunks et leurs vecteurs vivent
 * dans la même DB).
 * <p>
 * Limites : O(n) par recherche sur la KB (pas d'index ANN). Acceptable
 * jusqu'à ~50 000 chunks par KB ; au-delà, migrer vers Qdrant ou pgvector.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresVectorStore implements VectorStoreService {

    private final DocumentChunkRepository chunkRepository;

    @Override
    @Transactional
    public void upsertChunks(Long ownerId, Long kbId, Long documentId, List<ChunkUpsert> chunks) {
        // Upsert = delete + insert. Garantit qu'une réindexation ne laisse pas
        // d'orphelins. Filtré par documentId, donc pas de fuite cross-doc.
        chunkRepository.deleteByDocumentId(documentId);

        List<DocumentChunk> entities = new ArrayList<>(chunks.size());
        for (ChunkUpsert c : chunks) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setKbId(kbId);
            chunk.setOwnerId(ownerId);
            chunk.setOrdinal(c.ordinal());
            chunk.setText(c.text());
            chunk.setPageNumber(c.pageNumber());
            chunk.setTokenCount(c.tokenCount());
            chunk.setEmbedding(VectorCodec.encode(c.embedding()));
            chunk.setEmbeddingDim(c.embedding().length);
            entities.add(chunk);
        }
        chunkRepository.saveAll(entities);
        log.debug("Upserted {} chunks for doc={} (owner={}, kb={})",
                entities.size(), documentId, ownerId, kbId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorHit> search(Long ownerId, Long kbId, float[] queryVector, int topK) {
        // Sécurité : owner_id filtre obligatoire (cf. DESIGN_RULES.md §1 §2).
        List<DocumentChunk> chunks = chunkRepository.findAllByOwnerIdAndKbId(ownerId, kbId);
        if (chunks.isEmpty()) return List.of();

        float queryNorm = norm(queryVector);
        if (queryNorm == 0f) return List.of();

        List<VectorHit> hits = new ArrayList<>(chunks.size());
        for (DocumentChunk c : chunks) {
            float[] vec = VectorCodec.decode(c.getEmbedding());
            if (vec.length != queryVector.length) continue; // dimension mismatch → skip
            double score = cosine(queryVector, queryNorm, vec);
            hits.add(new VectorHit(c.getId(), c.getDocumentId(), c.getKbId(),
                    c.getOrdinal(), c.getText(), c.getPageNumber(), score));
        }
        hits.sort(Comparator.comparingDouble(VectorHit::score).reversed());
        return hits.size() <= topK ? hits : hits.subList(0, topK);
    }

    @Override
    @Transactional
    public void deleteByDocumentId(Long ownerId, Long documentId) {
        // La sécurité ownerId est garantie en amont (DocumentService vérifie
        // l'ownership via findByIdAndOwnerId avant d'appeler ici).
        chunkRepository.deleteByDocumentId(documentId);
    }

    /** Cosinus = (q · v) / (||q|| · ||v||). queryNorm pré-calculé. */
    private static double cosine(float[] q, float qNorm, float[] v) {
        double dot = 0.0;
        double vSq = 0.0;
        for (int i = 0; i < q.length; i++) {
            dot += (double) q[i] * v[i];
            vSq += (double) v[i] * v[i];
        }
        double vNorm = Math.sqrt(vSq);
        if (vNorm == 0.0) return 0.0;
        return dot / ((double) qNorm * vNorm);
    }

    private static float norm(float[] v) {
        double sq = 0.0;
        for (float x : v) sq += (double) x * x;
        return (float) Math.sqrt(sq);
    }
}
