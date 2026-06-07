# TICKET-014 — Action Endpoints (Trigger & Cancel)

## Summary
Implement the two custom action endpoints as a `@RepositoryRestController` sitting alongside Spring Data REST. These are the only hand-written controllers in the admin surface.

## Acceptance Criteria
- [ ] `ReplayController` in `web/replay/` annotated with `@RepositoryRestController`
- [ ] `POST /api/replayJobs/trigger`:
  - Accepts `ReplayTriggerDto` (configId, optional requestIds, optional filter)
  - Validates that exactly one of `requestIds` or `filter` is provided
  - Calls `ReplayJobScheduler.scheduleJob`
  - Returns `202 Accepted` with a HAL resource representing the created `ReplayJob` (including `_links.self`)
- [ ] `POST /api/replayJobs/{id}/cancel`:
  - Looks up the job; returns `404` if not found
  - If job is `PENDING` or `RUNNING`, transitions it to `FAILED` and signals the scheduler to abort the coroutine
  - If job is already `COMPLETED` or `FAILED`, returns `409 Conflict`
  - Returns `200 OK` with updated job resource on success
- [ ] `ReplayTriggerDto` validated with Bean Validation
- [ ] No CRUD logic in this controller — only the two action endpoints

## Dependencies
- **Blocked by:** TICKET-007 (Spring Data REST must be wired first — this controller extends it)
- **Blocked by:** TICKET-012 (ReplayJobScheduler)
- **Blocked by:** TICKET-013 (ReplayService must be wired for the scheduler to drive)

## Technical Notes
- `@RepositoryRestController` inherits the `/api` base path from Spring Data REST automatically
- Return type should be `ResponseEntity<EntityModel<ReplayJobDto>>` to produce proper HAL links
- Cancellation: `ReplayJobScheduler` must expose a `cancelJob(id)` method that finds and cancels the coroutine job by ID — use a `ConcurrentHashMap<UUID, Job>` to track active coroutines

## Tests Required
- Integration tests (`@SpringBootTest` + Testcontainers):
  - Valid trigger with `requestIds` → 202, job created in DB with `PENDING` status
  - Valid trigger with `filter` → 202, job created
  - Trigger with both `requestIds` and `filter` → 400
  - Trigger with neither → 400
  - Cancel `PENDING` job → 200, job status becomes `FAILED`
  - Cancel `COMPLETED` job → 409
  - Cancel non-existent job → 404
