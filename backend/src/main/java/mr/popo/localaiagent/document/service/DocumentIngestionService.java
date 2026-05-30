package mr.popo.localaiagent.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.document.domain.Document;
import mr.popo.localaiagent.document.domain.DocumentStatus;
import mr.popo.localaiagent.document.domain.KnowledgeBase;
import mr.popo.localaiagent.document.repository.DocumentRepository;
import mr.popo.localaiagent.document.repository.KnowledgeBaseRepository;
import mr.popo.localaiagent.rag.vector.ChunkUpsert;
import mr.popo.localaiagent.rag.vector.VectorStoreService;
import mr.popo.localaiagent.worker.client.PythonWorkerClient;
import mr.popo.localaiagent.worker.dto.EmbedResponse;
import mr.popo.localaiagent.worker.dto.ParseResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline d'ingestion asynchrone : parse (worker Python) → chunk (Java) →
 * embed (worker) → persist chunks via {@link VectorStoreService}.
 * <p>
 * Tourne sur un virtual thread (executor {@code applicationTaskExecutor}).
 * Les changements de statut passent par {@link DocumentStatusUpdater} —
 * c'est un bean séparé pour que le proxy {@code @Transactional} de Spring
 * fonctionne (les appels {@code this.method()} ne traversent pas les proxys).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository kbRepository;
    private final PythonWorkerClient workerClient;
    private final VectorStoreService vectorStore;
    private final TextChunker chunker;
    private final DocumentStatusUpdater statusUpdater;

    @Async("applicationTaskExecutor")
    public void ingest(Long documentId) {
        Document doc;
        KnowledgeBase kb;
        Path filePath;
        try {
            doc = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalStateException("Document missing: " + documentId));
            kb = kbRepository.findById(doc.getKbId())
                    .orElseThrow(() -> new IllegalStateException("KB missing: " + doc.getKbId()));
            filePath = Path.of(doc.getStoragePath());
            statusUpdater.markStatus(documentId, DocumentStatus.PARSING, null);
        } catch (Exception ex) {
            log.error("Pre-ingest failed for doc {}", documentId, ex);
            statusUpdater.markStatus(documentId, DocumentStatus.FAILED, "pre-ingest: " + ex.getMessage());
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(filePath);
            ParseResponse parsed = workerClient.parse(bytes, doc.getFileName(), doc.getMimeType());
            if (parsed == null || parsed.pages() == null || parsed.pages().isEmpty()) {
                statusUpdater.markStatus(documentId, DocumentStatus.FAILED, "parse returned no pages");
                return;
            }

            List<ChunkInProgress> chunks = chunkAllPages(parsed);
            if (chunks.isEmpty()) {
                statusUpdater.markStatus(documentId, DocumentStatus.FAILED, "no text extracted");
                return;
            }

            // Embedding en un seul appel — BGE-M3 supporte des batches confortables.
            List<String> texts = chunks.stream().map(ChunkInProgress::text).toList();
            EmbedResponse embed = workerClient.embed(texts, kb.getEmbeddingModel());
            if (embed == null || embed.embeddings() == null || embed.embeddings().size() != texts.size()) {
                statusUpdater.markStatus(documentId, DocumentStatus.FAILED, "embed size mismatch");
                return;
            }

            List<ChunkUpsert> upserts = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                ChunkInProgress c = chunks.get(i);
                upserts.add(new ChunkUpsert(c.ordinal(), c.text(), c.page(), null,
                        toFloatArray(embed.embeddings().get(i))));
            }
            vectorStore.upsertChunks(doc.getOwnerId(), doc.getKbId(), documentId, upserts);

            statusUpdater.markIndexed(documentId, parsed.pageCount(), chunks.size());
            log.info("Ingested document {} : {} pages, {} chunks", documentId,
                    parsed.pageCount(), chunks.size());
        } catch (IOException ex) {
            statusUpdater.markStatus(documentId, DocumentStatus.FAILED, "read file: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Ingestion failed for doc {}", documentId, ex);
            statusUpdater.markStatus(documentId, DocumentStatus.FAILED,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private List<ChunkInProgress> chunkAllPages(ParseResponse parsed) {
        List<ChunkInProgress> all = new ArrayList<>();
        int ord = 0;
        for (ParseResponse.ParsedPage page : parsed.pages()) {
            for (String c : chunker.chunk(page.text())) {
                all.add(new ChunkInProgress(ord++, c, page.n()));
            }
        }
        return all;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private record ChunkInProgress(int ordinal, String text, Integer page) {}
}
