package mr.popo.localaiagent.chat.repository;

import mr.popo.localaiagent.chat.domain.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("""
            SELECT s FROM ChatSession s
            WHERE s.ownerId = :ownerId
            ORDER BY s.lastMessageAt DESC NULLS LAST, s.createdAt DESC
            """)
    Page<ChatSession> findAllByOwner(@Param("ownerId") Long ownerId, Pageable pageable);
}
