# Database schema

Tables are managed by Flyway. Migrations are sequential and never use
`spring.jpa.hibernate.ddl-auto=update` — `ddl-auto` is `validate` in all environments, so
every entity column change ships with a matching migration.

| Migration | Adds |
|---|---|
| `V1__init.sql` | `captured_requests`, `execution_configs`, `replay_jobs`, `execution_logs` |
| `V2__add_users.sql` | `users` |
| `V3__add_audit_log.sql` | `audit_log` |
| `V4__add_replay_job_attribution.sql` | `replay_jobs.triggered_by`, `replay_jobs.triggered_by_username` |

## Tables

- **`captured_requests`** — append-only; the runtime DB role should have `INSERT + SELECT` only.
- **`execution_configs`** — mutable replay templates.
- **`replay_jobs`** — batch job tracking, incl. `triggered_by` attribution.
- **`execution_logs`** — one row per individual HTTP execution.
- **`users`** — admin console accounts; BCrypt hashes only.
- **`audit_log`** — append-only trail of admin actions.

JSON-shaped columns (`headers`, `header_overrides`, `mutation_parameters`,
`response_headers`) are stored as `TEXT` and (de)serialized with Jackson in the JPA adapter.
