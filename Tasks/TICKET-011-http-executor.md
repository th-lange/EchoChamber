# TICKET-011 — HTTP Executor

## Summary
Implement `WebClientHttpExecutor` — the `HttpExecutor` adapter that fires the mutated request over HTTP and returns a timed `ExecutionResult`.

## Acceptance Criteria
- [ ] `WebClientHttpExecutor` in `adapter/http/` implements `HttpExecutor`
- [ ] Uses Spring `WebClient` (non-blocking) to send the request
- [ ] Respects `MutableRequest.method`, `uri`, `headers`, and `body`
- [ ] Records wall-clock duration from dispatch to response receipt in milliseconds
- [ ] Maps response to `ExecutionResult`: status enum, HTTP status code, response time, response headers, response body (as String)
- [ ] A connection timeout or read timeout → `ExecutionResult` with `status = TIMEOUT`
- [ ] A non-2xx HTTP response is still a successful execution (`SUCCESS`) — the caller decides whether to treat it as a failure
- [ ] Any unhandled exception → `ExecutionResult` with `status = FAILURE` and a log at WARN level
- [ ] `WebClient` instance is injected as a Spring bean — not constructed inside the adapter

## Dependencies
- **Blocked by:** TICKET-003 (HttpExecutor interface)

## Technical Notes
- Timeout configuration (`connect-timeout`, `read-timeout`) should be configurable via `application.yml` under `echochamber.executor`
- Do not follow redirects by default — let the caller decide
- Response body is read fully and stored as a String. For large bodies, consider a size cap (configurable, default 10MB) to avoid OOM

## Tests Required
- Unit test with a mock `WebClient` (using `MockWebServer` from OkHttp or WireMock):
  - 200 response → `SUCCESS`, correct status code and body captured
  - 500 response → `SUCCESS` (HTTP error is not an executor failure), status code 500 captured
  - Connection timeout → `TIMEOUT`
  - Read timeout → `TIMEOUT`
  - Network error → `FAILURE`
  - Response time is measured and positive
