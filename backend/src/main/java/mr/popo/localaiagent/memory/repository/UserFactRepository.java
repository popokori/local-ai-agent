package mr.popo.localaiagent.memory.repository;

import mr.popo.localaiagent.memory.domain.UserFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFactRepository extends JpaRepository<UserFact, Long> {

    Optional<UserFact> findByIdAndUserId(Long id, Long userId);

    List<UserFact> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<UserFact> findByUserIdAndFactKey(Long userId, String factKey);
}
