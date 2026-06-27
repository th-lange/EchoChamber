-- V2__add_users.sql
-- Admin console user accounts (TICKET-019). Passwords stored as BCrypt hashes only.

CREATE TABLE users (
    id                    UUID         PRIMARY KEY,
    username              VARCHAR(255) NOT NULL UNIQUE,
    password_hash         TEXT         NOT NULL,
    role                  VARCHAR(20)  NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    created_by            UUID
);

CREATE INDEX idx_users_role ON users (role);
