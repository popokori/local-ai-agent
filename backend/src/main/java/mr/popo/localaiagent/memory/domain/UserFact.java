package mr.popo.localaiagent.memory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_facts")
@Getter
@Setter
@NoArgsConstructor
public class UserFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fact_key", nullable = false, length = 128)
    private String factKey;

    @Column(name = "fact_value", nullable = false, columnDefinition = "TEXT")
    private String factValue;

    @Column(name = "confidence", nullable = false)
    private Float confidence = 0.7f;

    @Column(name = "source", length = 64)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
