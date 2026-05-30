package mr.popo.localaiagent.document.service;

import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.document.domain.DocumentStatus;
import mr.popo.localaiagent.document.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Petit bean dédié aux mises à jour de statut d'un Document. Existe parce que
 * Spring AOP ne proxifie pas les appels {@code this.method()} : les
 * {@code @Transactional} sur méthodes auto-invoquées depuis
 * {@link DocumentIngestionService#ingest(Long)} ne s'appliqueraient pas. En
 * passant par ce bean injecté, le proxy transactionnel est sollicité.
 */
@Service
@RequiredArgsConstructor
public class DocumentStatusUpdater {

    private final DocumentRepository documentRepository;

    @Transactional
    public void markStatus(Long documentId, DocumentStatus status, String error) {
        documentRepository.findById(documentId).ifPresent(d -> {
            d.setStatus(status);
            d.setError(error);
        });
    }

    @Transactional
    public void markIndexed(Long documentId, Integer pageCount, int chunkCount) {
        documentRepository.findById(documentId).ifPresent(d -> {
            d.setStatus(DocumentStatus.INDEXED);
            d.setPageCount(pageCount);
            d.setChunkCount(chunkCount);
            d.setIndexedAt(OffsetDateTime.now());
            d.setError(null);
        });
    }
}
