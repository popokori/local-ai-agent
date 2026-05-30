package mr.popo.localaiagent.message.repository;

import mr.popo.localaiagent.message.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);

    List<Message> findTop40BySessionIdOrderByCreatedAtDesc(Long sessionId);

    Optional<Message> findBySessionIdAndClientRequestId(Long sessionId, UUID clientRequestId);
}
