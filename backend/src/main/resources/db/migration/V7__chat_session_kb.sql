-- V7 : lier les sessions de chat à une KB optionnelle (RAG auto si présente)

ALTER TABLE chat_sessions
    ADD COLUMN knowledge_base_id BIGINT REFERENCES knowledge_bases(id) ON DELETE SET NULL;

CREATE INDEX idx_chat_sessions_kb ON chat_sessions(knowledge_base_id);
