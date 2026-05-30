package mr.popo.localaiagent.rag.vector;

import java.util.List;

/**
 * Abstraction du stockage vectoriel. Phase 2 ne livre qu'une impl :
 * {@link PostgresVectorStore} (brute-force cosine en Java sur les chunks
 * de la KB filtrée par ownerId).
 * <p>
 * Impls futures envisagées sans changement d'API :
 *   - QdrantVectorStore  (cluster Qdrant via REST/gRPC, payload filter ownerId)
 *   - PgVectorStore      (extension pgvector + index HNSW, plus rapide)
 *
 * <h3>Règles de sécurité (cf. DESIGN_RULES.md §1 §2)</h3>
 * <ul>
 *   <li>{@code search(...)} et {@code upsert(...)} portent toujours
 *       {@code ownerId} et {@code kbId} — pas optionnels.</li>
 *   <li>Toute impl DOIT filtrer par {@code ownerId} avant le calcul vectoriel.
 *       Un appel qui ignorerait ce filtre est un bug de sécurité.</li>
 * </ul>
 */
public interface VectorStoreService {

    /**
     * Insère ou remplace les chunks d'un document dans le store.
     * Implémentations : upsert atomique par {@code documentId}.
     */
    void upsertChunks(Long ownerId, Long kbId, Long documentId, List<ChunkUpsert> chunks);

    /**
     * Recherche top-k chunks les plus similaires à {@code queryVector}
     * dans la KB {@code kbId} appartenant à {@code ownerId}.
     */
    List<VectorHit> search(Long ownerId, Long kbId, float[] queryVector, int topK);

    /** Supprime tous les chunks d'un document (réindexation, delete). */
    void deleteByDocumentId(Long ownerId, Long documentId);
}
