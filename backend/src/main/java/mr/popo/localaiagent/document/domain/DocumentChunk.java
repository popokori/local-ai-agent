package mr.popo.localaiagent.document.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /** Dénormalisé depuis Document.kbId pour filtrer rapidement. */
    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    /** Dénormalisé depuis Document.ownerId pour filtrer rapidement (sécurité). */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "ordinal", nullable = false)
    private Integer ordinal;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "token_count")
    private Integer tokenCount;

    /** Float32 little-endian sérialisé en bytes. Cf. VectorCodec.
     *  Pas de @Lob : sur PG cela génère un Large Object (OID bigint) ; on veut bytea. */
    @Column(name = "embedding", nullable = false, columnDefinition = "BYTEA")
    private byte[] embedding;

    @Column(name = "embedding_dim", nullable = false)
    private Integer embeddingDim;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
