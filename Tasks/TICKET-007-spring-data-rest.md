# TICKET-007 — Spring Data REST & HAL Explorer

## Summary
Wire Spring Data REST to auto-expose entity CRUD and browse endpoints from the JPA repositories. Mount HAL Explorer. No manual controller code for CRUD.

## Acceptance Criteria
- [ ] `spring-boot-starter-data-rest` and `spring-data-rest-hal-explorer` are on the classpath (already declared in TICKET-001)
- [ ] `spring.data.rest.base-path=/api` is set
- [ ] All four JPA repositories annotated with `@RepositoryRestResource`
- [ ] `CapturedRequestJpaRepository` is exported read-only:
  - `save` and `delete` methods overridden with `@RestResource(exported = false)`
- [ ] `ReplayJobJpaRepository` — read-only export (no create/delete via REST)
- [ ] `ExecutionLogJpaRepository` — read-only export
- [ ] `ExecutionConfigJpaRepository` — full CRUD exported
- [ ] HAL Explorer accessible at `/api/explorer`
- [ ] Paginated list endpoints return standard HAL `_embedded` + `_links` + `page` structure

## Dependencies
- **Blocked by:** TICKET-004 (JPA repositories must exist)

## Technical Notes
- Spring Data REST will expose paths derived from entity names by default. Override with `@RepositoryRestResource(path = "capturedRequests")` etc. for consistent naming.
- `CapturedRequest` never receives an update or delete via any REST path — verify this with a test.
- No `ExecutionConfigController` is written — Spring Data REST handles it entirely.

## Tests Required
- Integration test (`@SpringBootTest` + Testcontainers):
  - `GET /api/executionConfigs` returns paginated HAL response
  - `POST /api/executionConfigs` creates a record
  - `DELETE /api/executionConfigs/{id}` removes the record
  - `DELETE /api/capturedRequests/{id}` returns `405 Method Not Allowed`
  - `PUT /api/capturedRequests/{id}` returns `405 Method Not Allowed`
  - `GET /api/explorer` returns 200
