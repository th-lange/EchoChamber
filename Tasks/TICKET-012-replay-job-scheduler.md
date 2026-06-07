# TICKET-012 — Replay Job Scheduler

## Summary
Implement `ReplayJobScheduler` — the component that creates a `ReplayJob` record and launches a background coroutine scope to process it asynchronously.

## Acceptance Criteria
- [ ] `ReplayJobScheduler` in `application/`
- [ ] `scheduleJob(configId, requestIds?, filter?)` creates a `ReplayJob` with status `PENDING` via `StorageAdapter.saveJob`, then immediately returns the job
- [ ] A background coroutine is launched (using a `CoroutineScope` with `Dispatchers.Default + SupervisorJob()`) to drive `ReplayService.executeJob(jobId)`
- [ ] The background scope is a Spring-managed bean with `@PreDestroy` cancellation — job processing stops cleanly on application shutdown
- [ ] If a job's coroutine fails with an unhandled exception, the job status is set to `FAILED` and the error is logged — it does not crash other running jobs (`SupervisorJob` ensures isolation)
- [ ] The scheduler does not block the HTTP thread that triggered the job

## Dependencies
- **Blocked by:** TICKET-004 (StorageAdapter / JPA)
- **Blocked by:** TICKET-003 (domain models, ReplayJob)

## Technical Notes
- The `CoroutineScope` should be declared as a Spring `@Bean` so it can be injected and cancelled on shutdown
- `scheduleJob` is the only public method the web layer calls — `ReplayService` is internal to the application layer
- `requestIds` and `filter` are mutually exclusive — validate and throw `IllegalArgumentException` if both are provided

## Tests Required
- Unit tests with mocked `StorageAdapter` and mocked `ReplayService`:
  - `scheduleJob` persists a `PENDING` job and returns immediately
  - Background job is launched after the call returns
  - Both `requestIds` and `filter` provided → `IllegalArgumentException`
  - Neither `requestIds` nor `filter` provided → `IllegalArgumentException`
  - Background coroutine failure → job status updated to `FAILED`, exception does not propagate to other jobs
