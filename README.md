# EchoChamber

A **Kotlin / Spring Boot** HTTP request capture-and-replay engine. EchoChamber is Part 2 of a two-part system: a Go sidecar (Part 1) captures live HTTP traffic and forwards it here; EchoChamber stores those requests immutably and lets you replay them against any target, optionally mutating headers, URLs, body placeholders, or running arbitrary JavaScript transforms via a sandboxed GraalVM engine.

---

## Why it exists

Testing against real production traffic is fundamentally different from synthetic load tests or hand-crafted fixtures — real requests expose edge cases, auth flows, and payload shapes that are hard to replicate artificially.

EchoChamber was built to close that gap. A lightweight Go sidecar (Part 1) sits in-path of live ingress traffic and captures every request without adding latency to the hot path. EchoChamber (Part 2) stores those captures immutably and turns them into a replayable library. When you need to validate a new service version, test a migration, reproduce a production bug, or run a realistic load test, you trigger a replay job — pointing the same real traffic at a different target, mutated however you need: different base URL, swapped headers, substituted user IDs, or a full custom JavaScript transform to recompute signatures or derived fields.

The core problems it solves:

- **Regression testing with production fidelity** — replay the actual requests that hit your service yesterday against the new version today.
- **Environment migration validation** — redirect captured traffic to a staging or shadow environment to verify behaviour before cutting over.
- **Incident reproduction** — any captured request can be replayed in isolation, with mutations, to reproduce and diagnose a production failure.
- **Controlled load testing** — replay at configurable concurrency and rate limits using traffic that reflects real usage patterns, not synthetic scripts.

---

## What it does

| Capability | Detail |
|---|---|
| **Ingestion** | Receives captured HTTP requests from the Go sidecar over a secured internal endpoint and stores them in an append-only PostgreSQL table |
| **Mutation** | Transforms a captured request before replay using an ordered chain of handlers — header overrides, base-URL swap, placeholder substitution, or a custom JS script |
| **Replay** | Asynchronously re-fires captured requests against a configured target, with concurrency control and per-second rate limiting |
| **Execution tracking** | Records every individual execution (status, response code, latency, headers, body) and tracks overall job progress |
| **Admin console** | A server-rendered console at `/admin` to list captured requests, retry them with inline modifications (target URL/path, headers, body fields), view retry history, and manage users — plus Spring Data REST + HAL Explorer for zero-code CRUD |
| **Authentication & roles** | Form-login admin accounts with three roles — `VIEWER` (read), `OPERATOR` (+retry/modify), `ADMIN` (+user management & audit). The `/internal/**` ingest path stays on a separate static Bearer-token filter |
| **Audit trail** | Every retry, cancel, and user-management action is recorded (who, what, when) in an append-only audit log; replay jobs are attributed to the triggering user |

---

## Getting started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- JDK 21+ (only needed if running outside Docker)

### 1. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set strong values for:
- `INTERNAL_INGEST_TOKEN` — Bearer token the sidecar uses for `/internal/ingest`.
- `ADMIN_BOOTSTRAP_USER` / `ADMIN_BOOTSTRAP_PASSWORD` — the initial `ADMIN` account, created on first boot if no users exist (first login forces a password change).

The database defaults (`localhost:5432`, db/user/password all `echochamber`) work out of the box with Docker Compose.

### 2. Start with Docker Compose

```bash
docker compose up --build
```

This starts the `db` (**PostgreSQL 15**) and `app` (**EchoChamber**, `http://localhost:8080`) services on a shared `reexec-net` network. The SnapReq sidecar is a profile-gated service — `docker compose --profile fullstack up` adds it once it has a Dockerfile.

### 3. Run locally (without Docker)

Start only the database, then run the app via Gradle:

```bash
docker compose up -d db
./gradlew bootRun
```

### Key endpoints

All `/admin/**` and `/api/**` endpoints require an authenticated session (log in at `/login`); `/internal/**` uses the Bearer token instead.

| URL | Description |
|---|---|
| `http://localhost:8080/login` | Admin console login |
| `http://localhost:8080/admin` | Admin console — requests, retry-with-modify, history, users, audit |
| `http://localhost:8080/swagger-ui.html` | Swagger UI — interactive API docs |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/api/explorer` | HAL Explorer — browse all Spring Data REST resources |
| `http://localhost:8080/api/capturedRequests` | Browse captured requests (read-only) |
| `http://localhost:8080/api/executionConfigs` | Manage replay configs |
| `http://localhost:8080/api/replayJobs` | Browse replay jobs (read-only) |
| `http://localhost:8080/api/auditLog` | Browse the audit log (ADMIN only) |
| `http://localhost:8080/internal/ingest` | Ingestion endpoint (requires Bearer token) |

### Trigger a replay job (example)

Requires an authenticated `OPERATOR`/`ADMIN` session. Provide exactly one of `requestIds` or `filter`; `override` is the optional modify-before-retry payload.

```bash
curl -X POST http://localhost:8080/api/replayJobs/trigger \
  -H "Content-Type: application/json" \
  --cookie "JSESSIONID=<session>" \
  -d '{
    "configId": "00000000-0000-0000-0000-000000000000",
    "requestIds": ["11111111-1111-1111-1111-111111111111"],
    "override": {
      "targetUrl": "https://staging.example.com",
      "headersSet": {"X-Env": "staging"},
      "bodyPatches": {"userId": "42"}
    }
  }'
```

### Ingest a captured request (example)

Always returns `202 Accepted`. `capturedAt` is stamped server-side; `authority` is required and `body` is `null` (present, not omitted) when there is no body.

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

---

## Architecture

The project follows strict DDD / clean architecture. Layer boundaries are enforced by the [Agent.md](Agent.md) rules.

```mermaid
graph TD
    Part1[Part 1: Go Sidecar]

    subgraph Ingestion
        IC[IngestionController]
        IS[IngestionService]
    end

    subgraph Domain
        RR[CapturedRequest]
        EC[ExecutionConfig]
        RJ[ReplayJob]
        EL[ExecutionLog]
    end

    subgraph Replay
        RJS[ReplayJobScheduler]
        RS[ReplayService]
        ME[MutationEngine]
        HC[HttpExecutor]
    end

    subgraph Storage
        RA[RequestRepository]
        CA[ConfigRepository]
        JA[JobRepository]
        LA[LogRepository]
        DB[(Database)]
    end

    Part1 -->|POST /internal/ingest| IC
    IC --> IS
    IS --> RA

    RJS -->|Launches Coroutine| RS
    RS --> CA
    RS --> ME
    ME --> HC
    HC --> LA
    RS --> JA

    RA --> DB
    CA --> DB
    JA --> DB
    LA --> DB

    style Part1 fill:#625F9B,stroke:none,color:#FFF
    style IC fill:#DB488B,stroke:none,color:#FFF
    style IS fill:#DB488B,stroke:none,color:#FFF
    style RS fill:#DB488B,stroke:none,color:#FFF
    style RJS fill:#DB488B,stroke:none,color:#FFF
    style ME fill:#63EBB6,stroke:none,color:#000
    style HC fill:#63EBB6,stroke:none,color:#000
    style RR fill:#FFED6E,stroke:none,color:#000
    style EC fill:#FFED6E,stroke:none,color:#000
    style RJ fill:#FFED6E,stroke:none,color:#000
    style EL fill:#FFED6E,stroke:none,color:#000
    style RA fill:#B2ACAB,stroke:none,color:#000
    style CA fill:#B2ACAB,stroke:none,color:#000
    style JA fill:#B2ACAB,stroke:none,color:#000
    style LA fill:#B2ACAB,stroke:none,color:#000
    style DB fill:#FFED6E,stroke:none,color:#000
```

### Layer structure

```
domain/          ← pure Kotlin, no framework imports
  model/         ← immutable domain entities (val-only data classes)
  port/          ← interfaces: StorageAdapter, MutationHandler, HttpExecutor

application/     ← orchestration only; imports domain, nothing else
  IngestionService
  MutationEngine
  ReplayService
  ReplayJobScheduler
  UserService          ← create/disable/role/password, last-active-admin guard
  AuditService         ← best-effort audit writes
  ConsoleService       ← read-side queries for the console
  BootstrapAdminInitializer

adapter/         ← implements domain ports; may import Spring, JPA, WebClient
  persistence/
    jpa/         ← JpaStorageAdapter, JpaUserStore, JpaAuditStore (blocking on Dispatchers.IO)
  http/          ← WebClientHttpExecutor
  mutation/      ← HeaderOverride, BaseUrl, PlaceholderReplacement handlers
  security/      ← BCryptPasswordHasher

web/             ← Spring controllers + DTOs only; calls application services
  filter/        ← InternalAuthFilter (Bearer token on /internal/**)
  security/      ← SecurityConfig (two chains), DbUserDetailsService
  ingestion/     ← POST /internal/ingest
  replay/        ← POST /api/replayJobs/trigger, POST /api/replayJobs/{id}/cancel
  user/          ← /api/users (ADMIN-only)
  console/       ← server-rendered /admin console (Thymeleaf)
```

> **Not yet implemented:** the R2DBC storage adapter (TICKET-015) and the GraalVM
> `ScriptMutationHandler` (TICKET-010) are designed but not built — see the open tickets.

**Hard rules:**
- `domain/` never imports a framework class.
- `application/` never imports a web or persistence class.
- `web/` never calls a repository or adapter directly.
- Domain models never leave `application/` or `adapter/` as HTTP responses — always map to a DTO first.

---

## Domain model

### CapturedRequest
Immutable (`data class`, all `val`). Never updated or deleted after ingestion. The storage layer enforces `INSERT + SELECT` only.

### ExecutionConfig
A named replay template that defines:
- `baseUrlOverride` — swap the target host
- `headerOverrides` — add/replace headers
- `mutationParameters` — key/value map for placeholder substitution
- `mutationScript` — optional JavaScript run inside a sandboxed GraalVM context
- `maxConcurrency` — parallel request limit
- `rateLimitPerSecond` — token-bucket cap

### ReplayJob
Tracks a batch execution: status (`PENDING → RUNNING → COMPLETED / FAILED`), counts of total / processed / failed requests, timestamps.

### ExecutionLog
One record per individual execution: HTTP status, response time in ms, response headers and body, execution status (`SUCCESS / FAILURE / TIMEOUT`).

---

## Mutation pipeline

The `MutationEngine` runs an ordered chain of `MutationHandler` implementations. Each handler receives a `MutableRequest` copy — the original `CapturedRequest` is never touched.

| Handler | Order | What it does |
|---|---|---|
| `HeaderOverrideMutationHandler` | 10 | Adds or replaces headers from `ExecutionConfig.headerOverrides` |
| `BaseUrlMutationHandler` | 20 | Replaces the scheme + authority with `ExecutionConfig.baseUrlOverride` |
| `PlaceholderReplacementMutationHandler` | 30 | Substitutes `{{key}}` tokens in the URI, headers, and body using `mutationParameters` |
| `ScriptMutationHandler` _(planned, TICKET-010)_ | 100 | Will run `mutationScript` inside a GraalVM Polyglot JS context (`allowAllAccess(false)`, 2 s CPU limit) — not yet implemented |

After the config-driven chain, an optional per-request **inline override** (modify-before-retry) is applied last — target URL/path, header set/remove, and JSON body-field patches — so an operator's explicit edit wins over config handlers. The original `CapturedRequest` is never touched.

New mutation rules: implement `MutationHandler`, declare `@Component`, return the desired `order()` value.

---

## Replay execution

`ReplayService` drives the async execution loop:

1. Load `CapturedRequest` records matching the job's filter.
2. For each request, run the `MutationEngine` to produce a `MutableRequest`.
3. Fire the mutated request via `HttpExecutor` (Spring WebClient under the hood).
4. Persist an `ExecutionLog` with the result.
5. Update the `ReplayJob` progress counters.

Concurrency is bounded by a Kotlin `Semaphore(maxConcurrency)`. Rate limiting uses Resilience4j — not `Thread.sleep`. `ReplayJobScheduler` creates a `PENDING` job, then launches a background coroutine via `SupervisorJob` to run `ReplayService`.

---

## API surface

### Ingestion (internal)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/internal/ingest` | Bearer `INTERNAL_INGEST_TOKEN` | Accept a captured request from the Go sidecar |

### Spring Data REST (auto-exposed, HAL format)

| Path | Description |
|---|---|
| `GET /api/capturedRequests` | Browse captured requests (read-only) |
| `GET /api/executionConfigs` | List execution configs |
| `POST /api/executionConfigs` | Create a config |
| `PUT /api/executionConfigs/{id}` | Update a config |
| `DELETE /api/executionConfigs/{id}` | Delete a config |
| `GET /api/replayJobs` | Browse replay jobs (read-only) |
| `GET /api/executionLogs` | Browse execution logs (read-only) |
| `GET /api/explorer` | HAL Explorer UI |

| `GET /api/auditLog` | Browse the audit log (read-only, **ADMIN**) |

### Action endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/replayJobs/trigger` | OPERATOR/ADMIN | Start an async replay job (with optional inline override); returns `202 Accepted` |
| `POST` | `/api/replayJobs/{id}/cancel` | OPERATOR/ADMIN | Abort a running job (`409` if already terminal, `404` if unknown) |

### User management (ADMIN)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users` | List users (never returns password hashes) |
| `POST` | `/api/users` | Create a user with a temporary password |
| `POST` | `/api/users/{id}/disable` · `/enable` | Toggle an account (last active ADMIN is protected) |
| `POST` | `/api/users/{id}/role` · `/reset-password` | Change role / reset password |

---

## Database schema

Tables managed by Flyway (`V1__init.sql` … `V4__add_replay_job_attribution.sql`):

- **`captured_requests`** — append-only; DB role has `INSERT + SELECT` only.
- **`execution_configs`** — mutable replay templates.
- **`replay_jobs`** — batch job tracking, incl. `triggered_by` attribution (V4).
- **`execution_logs`** — one row per individual HTTP execution.
- **`users`** — admin console accounts; BCrypt hashes only (V2).
- **`audit_log`** — append-only trail of admin actions (V3).

`spring.jpa.hibernate.ddl-auto` is set to `validate` in all environments. Schema changes always go through a new Flyway migration file.

---

## Security

EchoChamber uses **two independent authentication mechanisms**, wired as separate Spring Security filter chains:

- **`/internal/**` (machine-to-machine):** `InternalAuthFilter` validates a static Bearer token (`INTERNAL_INGEST_TOKEN`, read from env, never hardcoded) and returns `401` before any controller logic. This chain is not routed through the form login.
- **`/admin/**` and `/api/**` (humans):** Spring Security form login backed by the `users` table (BCrypt). Role-based access — `VIEWER` (read), `OPERATOR` (+retry/modify), `ADMIN` (+users & audit). The initial ADMIN is bootstrapped from `ADMIN_BOOTSTRAP_USER`/`ADMIN_BOOTSTRAP_PASSWORD`; new accounts must change their password on first login, and the last active ADMIN cannot be disabled or demoted.

Additional guarantees:

- Password hashes are never serialized in any DTO or REST resource.
- Admin actions (retry, cancel, user changes) are recorded in the append-only `audit_log`.
- Request bodies are never logged at `INFO` level — `DEBUG` only, gated behind a PII feature flag.
- _Planned:_ the GraalVM `ScriptMutationHandler` will run scripts with `allowAllAccess(false)` and a 2 s CPU limit (TICKET-010).

---

## Technology stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| Framework | Spring Boot |
| Persistence (blocking) | Spring Data JPA + PostgreSQL |
| Persistence (reactive) | Spring Data R2DBC _(planned, TICKET-015)_ |
| Schema migrations | Flyway |
| HTTP client | Spring WebClient |
| Concurrency | Kotlin Coroutines + `kotlinx-coroutines-reactor` |
| Rate limiting | Resilience4j |
| Security | Spring Security (form login + RBAC), BCrypt |
| Scripting sandbox | GraalVM Polyglot (JS) _(planned, TICKET-010)_ |
| Admin UI | Custom Thymeleaf console + Spring Data REST / HAL Explorer |
| Integration tests | Testcontainers (real PostgreSQL, no H2) |

---

## Testing requirements

Every layer has a required test type:

| Layer | Required test |
|---|---|
| Domain model | Unit — construction, immutability, equality |
| Port interface | Contract test — any implementation must satisfy the interface |
| Application service | Unit with mocked ports |
| JPA / R2DBC adapter | Integration test via Testcontainers (real PostgreSQL) |
| Mutation handlers | Unit test per handler |
| `ScriptMutationHandler` | Unit tests: valid script, script throws, sandbox escape, CPU timeout |
| `InternalAuthFilter` | Unit tests: valid token, missing token, wrong token |
| Controllers | `@SpringBootTest` + `MockMvc` / `WebTestClient` |
| Ingestion endpoint | Valid payload persisted; invalid payload rejected; auth rejected |
| Replay trigger | Job created, `202` returned, job progresses asynchronously |

Integration tests must use Testcontainers — never mock the database.

---

## Governance

All coding standards, layer rules, security guardrails, and a self-correction checklist are defined in [Agent.md](Agent.md). 
Read it before writing any code.
