-- V1__init.sql
-- Initial schema for the EchoChamber engine.
-- Four tables: captured_requests, execution_configs, replay_jobs, execution_logs.
--
-- Notes:
-- * `captured_requests` is append-only. The application enforces this via a
--   write-only repository interface. The DB role used by the engine in
--   production should additionally have only INSERT + SELECT on this table.
-- * JSON-shaped columns (`headers`, `header_overrides`, `mutation_parameters`,
--   `response_headers`) are stored as TEXT here for portability; the JPA
--   adapter serialises/deserialises with Jackson.
-- * `execution_logs.job_id` is currently NOT NULL: the TICKET-002 domain model
--   `ExecutionLog.jobId` is a non-nullable `UUID`. The Implementation Plan
--   describes the column as nullable to allow ad-hoc executions outside a
--   batch job; revisit when/if the domain model is loosened.
-- * `execution_logs` has no `config_id` column: the domain `ExecutionLog`
--   model does not carry a config id (it is reachable via `job_id ->
--   replay_jobs.config_id`). Adding a denormalised column with no source of
--   truth would be misleading. Revisit if the domain model gains the field.
-- * `execution_configs.rate_limit_per_second` is DOUBLE PRECISION (not INT
--   as in the plan) because the domain `ExecutionConfig.rateLimitPerSecond`
--   is a `Double` (fractional rates are meaningful for token-bucket
--   limiters).

CREATE TABLE captured_requests (
    id           UUID         PRIMARY KEY,
    captured_at  TIMESTAMP    NOT NULL,
    method       VARCHAR(10)  NOT NULL,
    uri          TEXT         NOT NULL,
    authority    VARCHAR(255) NOT NULL,
    headers      TEXT         NOT NULL,
    body         TEXT
);

CREATE INDEX idx_captured_requests_captured_at ON captured_requests (captured_at);
CREATE INDEX idx_captured_requests_method      ON captured_requests (method);
CREATE INDEX idx_captured_requests_authority   ON captured_requests (authority);

CREATE TABLE execution_configs (
    id                     UUID         PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    base_url_override      TEXT         NOT NULL,
    header_overrides       TEXT         NOT NULL,
    max_concurrency        INT          NOT NULL DEFAULT 10,
    rate_limit_per_second  DOUBLE PRECISION NOT NULL DEFAULT 0,
    mutation_parameters    TEXT         NOT NULL,
    mutation_script        TEXT,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL
);

CREATE TABLE replay_jobs (
    id                  UUID        PRIMARY KEY,
    config_id           UUID        NOT NULL REFERENCES execution_configs (id),
    status              VARCHAR(20) NOT NULL,
    total_requests      INT         NOT NULL DEFAULT 0,
    processed_requests  INT         NOT NULL DEFAULT 0,
    failed_requests     INT         NOT NULL DEFAULT 0,
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP
);

CREATE INDEX idx_replay_jobs_config_id ON replay_jobs (config_id);
CREATE INDEX idx_replay_jobs_status    ON replay_jobs (status);

CREATE TABLE execution_logs (
    id                UUID        PRIMARY KEY,
    request_id        UUID        NOT NULL REFERENCES captured_requests (id),
    job_id            UUID        NOT NULL REFERENCES replay_jobs       (id),
    executed_at       TIMESTAMP   NOT NULL,
    status            VARCHAR(20) NOT NULL,
    response_status   INT,
    response_time_ms  BIGINT      NOT NULL,
    response_headers  TEXT        NOT NULL,
    response_body     TEXT
);

CREATE INDEX idx_execution_logs_request_id ON execution_logs (request_id);
CREATE INDEX idx_execution_logs_job_id     ON execution_logs (job_id);
