package mr.popo.localaiagent.document.repository;

import mr.popo.localaiagent.document.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Document> findAllByKbIdAndOwnerIdOrderByCreatedAtDesc(Long kbId, Long ownerId, Pageable pageable);
}
