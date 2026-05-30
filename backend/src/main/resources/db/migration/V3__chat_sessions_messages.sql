-- V3 : sessions de chat + messages

CREATE TABLE chat_sessions (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255),
    mode            VARCHAR(32) NOT NULL DEFAULT 'NORMAL',  -- NORMAL | EXPERT | FACT_CHECK
    model_name      VARCHAR(128),
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_chat_sessions_owner ON chat_sessions(owner_id);
CREATE INDEX idx_chat_sessions_owner_last_message ON chat_sessions(owner_id, last_message_at DESC NULLS LAST);

CREATE TABLE messages (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role                VARCHAR(16) NOT NULL,    -- USER | ASSISTANT | SYSTEM | TOOL
    content             TEXT NOT NULL,
    tokens_in           INTEGER,
    tokens_out          INTEGER,
    latency_ms          INTEGER,
    client_request_id   UUID,
    -- Champs préparés pour Phase 2/3 (RAG, tools) :
    tool_calls_json     JSONB,
    sources_json        JSONB,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_session_created ON messages(session_id, created_at);
CREATE UNIQUE INDEX uq_messages_session_client_request
    ON messages(session_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
