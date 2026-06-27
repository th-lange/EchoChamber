-- V4__add_replay_job_attribution.sql
-- Attribution: who triggered each replay job (TICKET-021).

ALTER TABLE replay_jobs ADD COLUMN triggered_by          UUID;
ALTER TABLE replay_jobs ADD COLUMN triggered_by_username VARCHAR(255);
