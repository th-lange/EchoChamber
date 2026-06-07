# TICKET-002 — Domain Models

## Summary
Define all immutable domain models in `domain/model/`. No framework imports allowed in this package.

## Acceptance Criteria
- [ ] `CapturedRequest` — `data class`, all `val`, fields: `id`, `capturedAt`, `method`, `uri`, `authority`, `headers`, `body`
- [ ] `ExecutionConfig` — `data class`, all `val`, fields matching schema (including `mutationScript`, `mutationParameters`, `maxConcurrency`, `rateLimitPerSecond`)
- [ ] `ReplayJob` — `data class`, all `val`, fields: `id`, `configId`, `status` (enum), `totalRequests`, `processedRequests`, `failedRequests`, `startedAt`, `completedAt`
- [ ] `ExecutionLog` — `data class`, all `val`
- [ ] `MutableRequest` — mutable copy of a `CapturedRequest` used during replay; `var` fields allowed
- [ ] `ReplayFilter` — value object for query-based replay selection
- [ ] `ReplayJobStatus` — enum: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`
- [ ] `ExecutionStatus` — enum: `SUCCESS`, `FAILURE`, `TIMEOUT`
- [ ] No import from `org.springframework`, `javax.persistence`, or any adapter package

## Dependencies
- **Blocked by:** TICKET-001

## Technical Notes
- `CapturedRequest.headers` should be typed as `Map<String, String>` in the domain model, not raw JSON
- `MutableRequest` is created via a copy factory: `fun CapturedRequest.toMutable(): MutableRequest`
- `ReplayJob.status` transitions: `PENDING → RUNNING → COMPLETED | FAILED`

## Tests Required
- Unit tests for each model: construction, equality, `copy()` behaviour
- Verify `CapturedRequest` fields cannot be reassigned (Kotlin `val` — compiler enforces this, but document with a test that demonstrates `toMutable()` isolation)
