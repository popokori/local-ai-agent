package mr.popo.localaiagent.document.repository;

import mr.popo.localaiagent.document.domain.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Optional<KnowledgeBase> findByIdAndOwnerId(Long id, Long ownerId);

    List<KnowledgeBase> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByOwnerIdAndSlug(Long ownerId, String slug);
}
