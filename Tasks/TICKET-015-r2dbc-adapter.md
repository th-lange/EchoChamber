# TICKET-015 — R2DBC Storage Adapter

## Summary
Implement `R2dbcStorageAdapter` as a drop-in reactive alternative to `JpaStorageAdapter`. The domain and application layers are unaware of which adapter is active.

## Acceptance Criteria
- [ ] `R2dbcStorageAdapter` in `adapter/persistence/r2dbc/` implementing `StorageAdapter`
- [ ] All `suspend` functions use R2DBC coroutine extensions — no `.block()` calls
- [ ] `Flow<CapturedRequest>` returned from `findRequests` is a true reactive stream (not a collected list wrapped in `flowOf`)
- [ ] Activated via a Spring profile (`r2dbc`) or configuration property — JPA adapter remains the default
- [ ] `CapturedRequest` rows: `INSERT` only — no `UPDATE` or `DELETE` operations present in the adapter
- [ ] All four tables supported with the same Flyway schema (no separate migration needed)

## Dependencies
- **Blocked by:** TICKET-003 (StorageAdapter interface)
- **Blocked by:** TICKET-004 (Flyway migrations define the schema this adapter queries)

## Technical Notes
- Add `spring-boot-starter-data-r2dbc` and `r2dbc-postgresql` to `build.gradle.kts` (behind the `r2dbc` profile or as optional)
- JSON columns (`headers`, `headerOverrides`, etc.) require a custom `io.r2dbc.postgresql.codec.Json` codec — register it on the `ConnectionFactory`
- Entity mapping is done manually (no Spring Data R2DBC magic for complex types) or via `@Table` R2DBC entities in `adapter/persistence/r2dbc/entity/`

## Tests Required
- Integration test (`@SpringBootTest` with `r2dbc` profile + Testcontainers PostgreSQL):
  - All `StorageAdapter` operations verified against a real DB (same test scenarios as TICKET-004)
  - `findRequests` with a filter returns a live `Flow` (verify streaming, not buffered)
  - Confirm no `UPDATE`/`DELETE` on `captured_requests` is possible through this adapter
