# TICKET-006 — Ingestion Service & Endpoint

## Summary
Implement the `/internal/ingest` endpoint that receives structured payloads from Part 1 (Go sidecar), maps them to a `CapturedRequest`, and persists via `StorageAdapter`.

## Acceptance Criteria
- [ ] `IngestionController` in `web/ingestion/` handles `POST /internal/ingest`
- [ ] `CapturedRequestDto` validates required fields (`method`, `uri`, `authority`, `headers`) with Bean Validation annotations
- [ ] `IngestionService` in `application/` maps DTO → domain model and calls `StorageAdapter.appendRequest`
- [ ] Returns `201 Created` with the persisted request ID on success
- [ ] Returns `400 Bad Request` with field-level errors on invalid payload
- [ ] Returns `401` (handled by `InternalAuthFilter` — not this controller)
- [ ] `IngestionService` has no knowledge of HTTP or DTOs

## Dependencies
- **Blocked by:** TICKET-003 (port interfaces)
- **Blocked by:** TICKET-004 (JPA adapter)
- **Blocked by:** TICKET-005 (auth filter)

## Technical Notes
- `body` field on `CapturedRequestDto` is nullable — not all requests have a body
- `capturedAt` is set by the engine at ingestion time, not supplied by Part 1
- `IngestionService` depends on `StorageAdapter` interface — never on `JpaStorageAdapter` directly

## Tests Required
- Unit test for `IngestionService`: mocked `StorageAdapter`, verify `appendRequest` is called with correctly mapped domain object
- Integration test (`@SpringBootTest` + Testcontainers):
  - Valid payload + correct token → 201, row in DB
  - Valid payload + missing token → 401, no row in DB
  - Missing required field → 400 with validation error body
  - Body is null → 201 (nullable body is accepted)
