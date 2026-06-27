-- V3__add_audit_log.sql
-- Append-only audit trail of admin actions (TICKET-021).

CREATE TABLE audit_log (
    id              UUID        PRIMARY KEY,
    actor_user_id   UUID,
    actor_username  VARCHAR(255) NOT NULL,
    action          VARCHAR(40)  NOT NULL,
    target_type     VARCHAR(60),
    target_id       VARCHAR(255),
    detail          TEXT,
    occurred_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at);
CREATE INDEX idx_audit_log_action      ON audit_log (action);
