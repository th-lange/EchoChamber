# Domain model

All domain models are pure Kotlin (`data class`, no framework imports).

### CapturedRequest
Immutable — all `val`. Never updated or deleted after ingestion (the only permitted deletion
is TTL retention, TICKET-018). The storage layer enforces `INSERT + SELECT` only.

Fields: `id`, `capturedAt` (stamped on receipt), `method`, `uri`, `authority`, `headers`, `body`.

### ExecutionConfig
A named replay template:
- `targetBaseUrl` — swap the target scheme + host
- `headerOverrides` — add/replace headers
- `mutationParameters` — key/value map for `{{placeholder}}` substitution
- `mutationScript` — optional JS source (run by the GraalVM handler once TICKET-010 lands)
- `maxConcurrency` — parallel request limit
- `rateLimitPerSecond` — token-bucket cap (fractional allowed)

### ReplayJob
Tracks a batch execution: `status` (`PENDING → RUNNING → COMPLETED / FAILED`), counts of
total / processed / failed requests, `startedAt`/`completedAt`, and `triggeredBy` /
`triggeredByUsername` (attribution).

### ExecutionLog
One record per individual execution: `status` (`SUCCESS / FAILURE / TIMEOUT`),
`responseStatus`, `responseTimeMs`, response headers and body, `executedAt`. Append-only.

### RequestOverride
The inline modify-before-retry payload, applied to a `MutableRequest` copy:
`targetUrl`, `pathOverride`, `headersSet`, `headersRemove`, `bodyPatches`.

### User
An admin console account: `username`, `passwordHash` (BCrypt — never exposed in a DTO),
`role` (`VIEWER` / `OPERATOR` / `ADMIN`), `enabled`, `mustChangePassword`, audit timestamps,
`createdBy`.

### AuditEntry
An append-only audit record: `actorUserId`, `actorUsername`, `action`, `targetType`,
`targetId`, `detail`, `occurredAt`.
