package mr.popo.localaiagent.message.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "client_request_id")
    private UUID clientRequestId;

    /** Préparé pour Phase 2+ (tools). Toujours null en Phase 1. */
    @Type(JsonType.class)
    @Column(name = "tool_calls_json", columnDefinition = "jsonb")
    private Map<String, Object> toolCallsJson;

    /** Préparé pour Phase 2+ (RAG). Toujours null en Phase 1. */
    @Type(JsonType.class)
    @Column(name = "sources_json", columnDefinition = "jsonb")
    private Map<String, Object> sourcesJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
