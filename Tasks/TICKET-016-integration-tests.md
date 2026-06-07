# TICKET-016 — Integration Tests (End-to-End)

## Summary
Full end-to-end integration test suite validating the complete ingest → mutate → replay → log round-trip against a real PostgreSQL instance.

## Acceptance Criteria
- [ ] All tests use Testcontainers with a real PostgreSQL container — no H2, no mocks for DB
- [ ] Test suite covers the complete happy path and key failure scenarios listed below
- [ ] Tests are grouped in a dedicated `integration` source set or marked with a `@Tag("integration")` annotation so they can be excluded from fast unit test runs
- [ ] All tests pass in CI with a cold Docker environment

## Dependencies
- **Blocked by:** TICKET-014 (action endpoints — the final piece of the main flow)

## Test Scenarios

### Ingest → Replay → Log round-trip
- [ ] `POST /internal/ingest` with valid payload → request persisted
- [ ] `POST /api/replayJobs/trigger` with `requestIds: [<above id>]` and a config → 202, job created
- [ ] Poll `GET /api/replayJobs/{id}` until status is `COMPLETED` (with timeout)
- [ ] `GET /api/executionLogs?replayJob.id={jobId}` returns one log entry with `SUCCESS`

### Mutation correctness
- [ ] Config with `headerOverrides: {"X-Test": "overridden"}` → execution log shows outbound request had the overridden header (verify via WireMock target server)
- [ ] Config with `baseUrlOverride` → request hits the overridden host, not the original authority
- [ ] Config with `mutationScript` that rewrites the body → WireMock captures the rewritten body

### Concurrency & rate limiting
- [ ] Ingest 20 requests; trigger replay with `maxConcurrency: 2` → at no point are more than 2 in-flight simultaneously (verify via WireMock request timestamps)
- [ ] `rateLimitPerSecond: 5` with 10 requests → total wall time ≥ 2 seconds

### Immutability
- [ ] After a replay that includes mutations, `GET /api/capturedRequests/{id}` still returns the original unmodified request
- [ ] `DELETE /api/capturedRequests/{id}` returns 405

### Failure handling
- [ ] Target server returns 500 for one request in a batch → job still completes, that log entry has `response_status: 500` and `status: SUCCESS` (HTTP error ≠ executor failure)
- [ ] Target server times out for one request → log entry has `status: TIMEOUT`, job continues
- [ ] Script mutation throws → log entry has `status: FAILURE`, rest of job continues

## Technical Notes
- Use WireMock (`com.github.tomakehurst:wiremock-jre8`) as the HTTP target server to capture and verify outbound requests
- Testcontainers `PostgreSQLContainer` should be shared across the test suite (`@Container` at class level with `companion object`) to avoid repeated container startups
