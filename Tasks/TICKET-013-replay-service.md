# TICKET-013 — Replay Service

## Summary
Implement `ReplayService` — the core async execution loop that loads requests, applies mutations, fires HTTP calls, and persists execution logs, all while respecting concurrency and rate limits defined in the `ExecutionConfig`.

## Acceptance Criteria
- [ ] `ReplayService` in `application/`
- [ ] `executeJob(jobId)` transitions the job to `RUNNING`, processes all requests, then transitions to `COMPLETED` or `FAILED`
- [ ] Resolves the request list either from explicit IDs or by applying a `ReplayFilter` via `StorageAdapter.findRequests`
- [ ] For each request:
  1. Copies it to `MutableRequest`
  2. Calls `MutationEngine.mutate(mutableRequest, config)`
  3. Calls `HttpExecutor.execute(mutableRequest)`
  4. Persists an `ExecutionLog` via `StorageAdapter.saveLog`
  5. Increments `processedRequests` (or `failedRequests`) on the `ReplayJob`
- [ ] Concurrency is bounded by `ExecutionConfig.maxConcurrency` using a Kotlin coroutine `Semaphore`
- [ ] Rate limiting is enforced by `ExecutionConfig.rateLimitPerSecond` — use Resilience4j `RateLimiter` with coroutine suspension, not `Thread.sleep`
- [ ] A single request failure (mutation error or HTTP timeout) does not abort the job — it logs the failure and continues
- [ ] Job progress (`processedRequests`, `failedRequests`) is persisted after each request completes

## Dependencies
- **Blocked by:** TICKET-008 (MutationEngine)
- **Blocked by:** TICKET-009 (built-in handlers must be registered)
- **Blocked by:** TICKET-010 (ScriptMutationHandler)
- **Blocked by:** TICKET-011 (HttpExecutor)
- **Blocked by:** TICKET-012 (ReplayJobScheduler calls this service)

## Technical Notes
- `ReplayService` depends on `StorageAdapter`, `MutationEngine`, and `HttpExecutor` interfaces — never on concrete adapters
- `updateJob` is called after every individual request to keep progress live — consider batching if this causes DB contention at high throughput
- If `maxConcurrency` is null or 0, default to 10
- If `rateLimitPerSecond` is null or 0, no rate limiting is applied

## Tests Required
- Unit tests with all dependencies mocked:
  - Single request: mutated, executed, log persisted, job marked `COMPLETED`
  - Mutation throws → log persisted with `FAILURE`, job continues, eventually `COMPLETED`
  - HTTP executor returns `TIMEOUT` → log persisted with `TIMEOUT`, counted in `failedRequests`
  - `maxConcurrency = 2` with 10 requests → at most 2 concurrent executor calls at any point (verify with a semaphore counter in mock)
  - Rate limit applied → requests dispatched within expected time window
  - Job transitions: `RUNNING` → `COMPLETED` on success; `RUNNING` → `FAILED` if scheduler-level error
