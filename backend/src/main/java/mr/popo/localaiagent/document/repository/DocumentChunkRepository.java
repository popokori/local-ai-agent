package mr.popo.localaiagent.document.repository;

import mr.popo.localaiagent.document.domain.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Stream tous les chunks d'une KB en filtrant par sécurité sur ownerId.
     * Utilisé par {@code PostgresVectorStore.search(...)} pour le calcul cosinus.
     */
    List<DocumentChunk> findAllByOwnerIdAndKbId(Long ownerId, Long kbId);

    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.documentId = :documentId")
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
