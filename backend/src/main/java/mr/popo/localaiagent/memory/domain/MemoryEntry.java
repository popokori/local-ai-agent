package mr.popo.localaiagent.memory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "memory_entries")
@Getter
@Setter
@NoArgsConstructor
public class MemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private MemoryKind kind;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source_session_id")
    private Long sourceSessionId;

    @Column(name = "importance", nullable = false)
    private Float importance = 0.5f;

    /** Embedding float32 little-endian. Voir VectorCodec. */
    @Column(name = "embedding", nullable = false, columnDefinition = "BYTEA")
    private byte[] embedding;

    @Column(name = "embedding_dim", nullable = false)
    private Integer embeddingDim;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_accessed_at")
    private OffsetDateTime lastAccessedAt;
}
