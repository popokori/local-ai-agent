-- V6 : KnowledgeBases + Documents + Chunks (RAG, Phase 2)
-- Phase 2 stocke les embeddings en BYTEA directement dans PostgreSQL
-- (impl VectorStoreService = PostgresVectorStore). Une future impl Qdrant
-- ou pgvector pourra réutiliser les mêmes entités sans changer le schéma
-- relationnel (seul le storage des vecteurs migrera).

CREATE TABLE knowledge_bases (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    description     TEXT,
    domain          VARCHAR(32) NOT NULL DEFAULT 'GENERIC',
    embedding_model VARCHAR(64) NOT NULL DEFAULT 'bge-m3',
    embedding_dim   INTEGER NOT NULL DEFAULT 1024,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kb_owner_slug UNIQUE (owner_id, slug)
);

CREATE INDEX idx_kb_owner ON knowledge_bases(owner_id);

CREATE TABLE documents (
    id              BIGSERIAL PRIMARY KEY,
    kb_id           BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name       VARCHAR(512) NOT NULL,
    mime_type       VARCHAR(128),
    size_bytes      BIGINT NOT NULL,
    sha256          VARCHAR(64),
    storage_path    VARCHAR(1024) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    error           TEXT,
    page_count      INTEGER,
    chunk_count     INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    indexed_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_doc_kb ON documents(kb_id);
CREATE INDEX idx_doc_owner ON documents(owner_id);
CREATE INDEX idx_doc_status ON documents(status);

CREATE TABLE document_chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    -- owner_id et kb_id dénormalisés pour filtrage rapide par sécurité
    -- (cf. DESIGN_RULES.md §1, §2). Toute recherche vectorielle DOIT filtrer
    -- par (owner_id, kb_id) avant de calculer le cosinus.
    kb_id           BIGINT NOT NULL,
    owner_id        BIGINT NOT NULL,
    ordinal         INTEGER NOT NULL,
    text            TEXT NOT NULL,
    page_number     INTEGER,
    token_count     INTEGER,
    -- Embedding stocké en BYTEA : 1024 floats * 4 octets = 4 KiB par chunk.
    -- Format : float32 little-endian, dimension validée par embedding_dim.
    embedding       BYTEA NOT NULL,
    embedding_dim   INTEGER NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chunk_owner_kb ON document_chunks(owner_id, kb_id);
CREATE INDEX idx_chunk_document ON document_chunks(document_id);
