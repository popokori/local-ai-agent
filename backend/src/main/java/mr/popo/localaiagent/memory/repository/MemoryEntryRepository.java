package mr.popo.localaiagent.memory.repository;

import mr.popo.localaiagent.memory.domain.MemoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, Long> {

    /** Stream toutes les entrées d'un user pour le calcul cosinus en Java. */
    List<MemoryEntry> findAllByUserId(Long userId);
}
