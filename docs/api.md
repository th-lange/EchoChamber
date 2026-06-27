# API reference

All `/admin/**` and `/api/**` endpoints require an authenticated session (log in at
`/login`). `/internal/**` uses a static Bearer token instead. See [security.md](security.md).

## Ingestion (internal)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/internal/ingest` | Bearer `INTERNAL_INGEST_TOKEN` | Accept a captured request from the Go sidecar. Always returns `202 Accepted`; `capturedAt` is server-stamped; near-duplicates within a 1 s window are dropped |

```bash
curl -X POST http://localhost:8080/internal/ingest \
  -H "Authorization: Bearer <INTERNAL_INGEST_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "GET",
    "uri": "https://example.com/api/resource",
    "authority": "example.com",
    "headers": {"Accept": "application/json"},
    "body": null
  }'
```

## Spring Data REST (auto-exposed, HAL format)

| Path | Description |
|---|---|
| `GET /api/capturedRequests` | Browse captured requests (read-only) |
| `GET /api/executionConfigs` | List execution configs |
| `POST /api/executionConfigs` | Create a config (OPERATOR/ADMIN) |
| `PUT /api/executionConfigs/{id}` | Update a config (OPERATOR/ADMIN) |
| `DELETE /api/executionConfigs/{id}` | Delete a config (OPERATOR/ADMIN) |
| `GET /api/replayJobs` | Browse replay jobs (read-only) |
| `GET /api/executionLogs` | Browse execution logs (read-only) |
| `GET /api/auditLog` | Browse the audit log (read-only, **ADMIN**) |
| `GET /api/explorer` | HAL Explorer UI |

## Action endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/replayJobs/trigger` | OPERATOR/ADMIN | Start an async replay job (with optional inline override); returns `202 Accepted` |
| `POST` | `/api/replayJobs/{id}/cancel` | OPERATOR/ADMIN | Abort a job (`409` if already terminal, `404` if unknown) |

Trigger payload — provide exactly one of `requestIds` or `filter`; `override` is optional:

```bash
curl -X POST http://localhost:8080/api/replayJobs/trigger \
  -H "Content-Type: application/json" \
  --cookie "JSESSIONID=<session>" \
  -d '{
    "configId": "00000000-0000-0000-0000-000000000000",
    "requestIds": ["11111111-1111-1111-1111-111111111111"],
    "override": {
      "targetUrl": "https://staging.example.com",
      "pathOverride": "/v2/resource",
      "headersSet": {"X-Env": "staging"},
      "headersRemove": ["Authorization"],
      "bodyPatches": {"userId": "42"}
    }
  }'
```

## User management (ADMIN)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users` | List users (never returns password hashes) |
| `POST` | `/api/users` | Create a user with a temporary password |
| `POST` | `/api/users/{id}/disable` · `/enable` | Toggle an account (last active ADMIN is protected) |
| `POST` | `/api/users/{id}/role` | Change role (last active ADMIN cannot be demoted) |
| `POST` | `/api/users/{id}/reset-password` | Reset password (forces change on next login) |

## Admin console (server-rendered)

| Path | Description |
|---|---|
| `GET /login` | Login page |
| `GET /admin/requests` | Retriable requests + per-row retry-with-modify form |
| `GET /admin/history` | Retry history (links to the read-only HAL resources) |
| `GET /admin/users` | User management (ADMIN) |
| `GET /admin/audit` | Audit log view (ADMIN) |
| `GET /admin/password` | Forced password change |
