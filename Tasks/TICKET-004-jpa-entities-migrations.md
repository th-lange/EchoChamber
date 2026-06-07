# TICKET-004 — JPA Entities & Flyway Migrations

## Summary
Create the JPA `@Entity` classes for all four tables and write the initial Flyway migration. These are adapter-layer concerns — no domain model may reference them.

## Acceptance Criteria
- [ ] `@Entity` classes in `adapter/persistence/jpa/entity/`:
  - `CapturedRequestEntity`
  - `ExecutionConfigEntity`
  - `ReplayJobEntity`
  - `ExecutionLogEntity`
- [ ] Spring Data JPA repositories in `adapter/persistence/jpa/repository/`:
  - `CapturedRequestJpaRepository` — `save` and `find` only (no delete)
  - `ExecutionConfigJpaRepository` — full CRUD
  - `ReplayJobJpaRepository` — save, update, find
  - `ExecutionLogJpaRepository` — save, find
- [ ] `JpaStorageAdapter` in `adapter/persistence/jpa/` implementing `StorageAdapter`:
  - Maps between entity and domain model in both directions
  - All blocking calls dispatched to `Dispatchers.IO`
- [ ] Flyway migration `V1__init.sql` creates all four tables exactly matching the schema in `Part2-ImplementationPlan.md`
- [ ] `spring.jpa.hibernate.ddl-auto=validate` — Hibernate must not create or alter tables

## Dependencies
- **Blocked by:** TICKET-003

## Technical Notes
- `headers`, `headerOverrides`, `mutationParameters`, `responseHeaders` stored as `TEXT` (serialised JSON). Use Jackson for serialisation in the entity mapper.
- `ReplayJobEntity` status stored as `VARCHAR` — map to `ReplayJobStatus` enum in the domain mapper.
- `CapturedRequestJpaRepository` must not expose `deleteById` or `delete` — override with `@RestResource(exported = false)` if Spring Data REST is wired later.

## Tests Required
- Integration test (`@DataJpaTest` + Testcontainers PostgreSQL) for `JpaStorageAdapter`:
  - `appendRequest` persists correctly and is retrievable
  - `appendRequest` called twice produces two separate rows (no upsert)
  - Attempting to call any update/delete on `captured_requests` via the adapter throws or is unavailable
  - `saveJob` → `updateJob` → `getJob` reflects status changes
  - `saveLog` is linked correctly to its `request_id` and `job_id`
