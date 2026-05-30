-- V4 : audit logs (logs structurés des actions sensibles)

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    session_id   BIGINT REFERENCES chat_sessions(id) ON DELETE SET NULL,
    action       VARCHAR(32) NOT NULL,    -- LOGIN | LOGIN_FAILED | LOGOUT | MESSAGE | LLM_CALL
    payload_json JSONB,
    success      BOOLEAN NOT NULL,
    duration_ms  INTEGER,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_created ON audit_logs(action, created_at DESC);
