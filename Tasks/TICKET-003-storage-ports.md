# TICKET-003 — Port Interfaces

## Summary
Define the three domain port interfaces that decouple the application from any infrastructure. These are the only contracts adapters must implement.

## Acceptance Criteria
- [ ] `StorageAdapter` interface in `domain/port/` with `suspend` functions:
  - `appendRequest(r: CapturedRequest): CapturedRequest`
  - `findRequest(id: UUID): CapturedRequest?`
  - `findRequests(filter: ReplayFilter): Flow<CapturedRequest>`
  - `saveConfig(c: ExecutionConfig): ExecutionConfig`
  - `findConfig(id: UUID): ExecutionConfig?`
  - `deleteConfig(id: UUID)`
  - `saveJob(j: ReplayJob): ReplayJob`
  - `updateJob(j: ReplayJob): ReplayJob`
  - `getJob(id: UUID): ReplayJob?`
  - `saveLog(l: ExecutionLog): ExecutionLog`
  - No `updateRequest` or `deleteRequest` method exists
- [ ] `MutationHandler` interface in `domain/port/`:
  - `fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest`
  - `fun order(): Int`
- [ ] `HttpExecutor` interface in `domain/port/`:
  - `suspend fun execute(request: MutableRequest): ExecutionResult`
- [ ] `ExecutionResult` value object in `domain/model/`
- [ ] No framework imports in any of these files

## Dependencies
- **Blocked by:** TICKET-002

## Technical Notes
- `Flow<CapturedRequest>` is from `kotlinx.coroutines.flow` — this is the only coroutines import allowed in `domain/`
- `ExecutionResult` fields: `status: ExecutionStatus`, `responseStatus: Int?`, `responseTimeMs: Long`, `responseHeaders: Map<String, String>`, `responseBody: String?`

## Tests Required
- No runtime tests needed for pure interfaces
- Verify via a compile-time test (a dummy in-memory implementation) that the interface contract is implementable without any framework dependency
