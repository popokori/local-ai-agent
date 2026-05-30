-- V8 : mémoire long terme — profil structuré + entrées épisodiques/sémantiques

-- 1. Faits structurés sur l'utilisateur (clé/valeur)
CREATE TABLE user_facts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fact_key    VARCHAR(128) NOT NULL,
    fact_value  TEXT NOT NULL,
    confidence  REAL NOT NULL DEFAULT 0.7,
    source      VARCHAR(64),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_facts UNIQUE (user_id, fact_key)
);

CREATE INDEX idx_user_facts_user ON user_facts(user_id);

-- 2. Entrées de mémoire — épisodiques (résumés de session) et sémantiques (faits libres)
CREATE TABLE memory_entries (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind                VARCHAR(32) NOT NULL,   -- EPISODIC | SEMANTIC
    summary             TEXT NOT NULL,
    source_session_id   BIGINT REFERENCES chat_sessions(id) ON DELETE SET NULL,
    importance          REAL NOT NULL DEFAULT 0.5,
    embedding           BYTEA NOT NULL,
    embedding_dim       INTEGER NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_memory_user_kind ON memory_entries(user_id, kind);
CREATE INDEX idx_memory_user_importance ON memory_entries(user_id, importance DESC);
