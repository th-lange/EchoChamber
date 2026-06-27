# EchoChamber

A **Kotlin / Spring Boot** HTTP request capture-and-replay engine. EchoChamber is Part 2 of a
two-part system: a Go sidecar (Part 1) captures live HTTP traffic and forwards it here;
EchoChamber stores those requests immutably and lets you replay them against any target —
optionally mutating headers, URLs, and body fields — through an authenticated admin console.

---

## Basics

### Why it exists

Testing against real production traffic exposes edge cases, auth flows, and payload shapes
that synthetic load tests and hand-crafted fixtures miss. A lightweight Go sidecar captures
live ingress traffic without adding latency to the hot path; EchoChamber turns those captures
into a replayable library. When you need to validate a new service version, test a migration,
reproduce a production bug, or run a realistic load test, you trigger a replay job — pointing
real traffic at a different target, mutated however you need.

It solves:

- **Regression testing with production fidelity** — replay yesterday's real requests against today's build.
- **Environment migration validation** — redirect captured traffic to staging/shadow before cutover.
- **Incident reproduction** — replay any captured request in isolation, with mutations.
- **Controlled load testing** — replay at configurable concurrency and rate limits using real traffic.

### What it does

| Capability | Detail |
|---|---|
| **Ingestion** | Receives captured requests from the sidecar over a secured internal endpoint; stores them in an append-only PostgreSQL table |
| **Mutation** | Transforms a request before replay via an ordered handler chain — header overrides, base-URL swap, placeholder substitution |
| **Replay** | Asynchronously re-fires captured requests at a target, with concurrency control and per-second rate limiting |
| **Execution tracking** | Records every execution (status, code, latency, headers, body) and overall job progress |
| **Admin console** | Server-rendered `/admin` console to list requests, retry with inline modifications, view history, and manage users — plus HAL Explorer for zero-code CRUD |
| **Auth & roles** | Form-login accounts with `VIEWER` / `OPERATOR` / `ADMIN` roles; `/internal/**` ingest uses a separate Bearer token |
| **Audit trail** | Every retry, cancel, and user action is recorded (who, what, when); jobs are attributed to the triggering user |

For the full technical reference, see [`docs/`](docs/README.md).

---

## Getting started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- JDK 21+ (only needed to run outside Docker)

### 1. Configure environment (optional)

`docker compose up` works out of the box with built-in development defaults — no `.env`
required. To override them (and you should before exposing the service anywhere beyond local
dev), copy the template and edit:

```bash
cp .env.example .env
```

- `INTERNAL_INGEST_TOKEN` — Bearer token the sidecar uses for `/internal/ingest`. **Empty by default, which disables ingest auth** (fine for local dev); set a value to require it.
- `ADMIN_BOOTSTRAP_USER` / `ADMIN_BOOTSTRAP_PASSWORD` — the initial `ADMIN` account, created
  on first boot if no users exist (default `admin` / `admin`; first login forces a password change).

### 2. Run with Docker Compose

```bash
docker compose up --build
```

Starts `db` (PostgreSQL 15) and `app` (EchoChamber on `http://localhost:8080`) on a shared
`reexec-net` bridge network. SnapReq runs from its own compose project and attaches to the
same network (declared `external: true` on its side) to reach EchoChamber at
`http://app:8080/internal/ingest`.

### 3. Run locally (without Docker)

```bash
docker compose up -d db
./gradlew bootRun
```

### Key endpoints

Log in at `/login`; all `/admin/**` and `/api/**` endpoints require a session. `/internal/**`
uses the Bearer token instead.

| URL | Description |
|---|---|
| `http://localhost:8080/admin` | Admin console (requests, retry-with-modify, history, users, audit) |
| `http://localhost:8080/api/explorer` | HAL Explorer — browse Spring Data REST resources |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/internal/ingest` | Ingestion endpoint (Bearer token) |

See [docs/api.md](docs/api.md) for the full surface and request/response examples.

---

## Development

### Build & test

```bash
./gradlew build                          # compile + unit tests
./gradlew test                           # unit tests (integration tests skip without Docker)
./gradlew test -Drun.integration=true    # + Testcontainers integration tests (needs Docker)
```

CI (`.github/workflows/ci.yml`) runs unit + integration tests on every push/PR.

### Project layout

```
domain/        pure Kotlin — models, ports, mutation helpers
application/    orchestration — services (ingestion, mutation, replay, users, audit)
adapter/       port implementations — JPA persistence, WebClient, mutation handlers, security
web/           controllers, DTOs, security config, the /admin console
```

Boundaries are strict — see [docs/architecture.md](docs/architecture.md).

### Contributing

All coding standards, layer rules, security guardrails, and a self-correction checklist are
defined in [Agent.md](Agent.md) — read it before writing any code. Work is tracked as tickets
with one pull request per ticket.

---

## Documentation

| Doc | Contents |
|---|---|
| [Architecture](docs/architecture.md) | Layers, component/data flow, hard rules |
| [Domain model](docs/domain-model.md) | Core entities and value objects |
| [Mutation & replay](docs/mutation-and-replay.md) | Mutation chain and async replay loop |
| [API reference](docs/api.md) | Ingestion, REST resources, action & user endpoints, console |
| [Security](docs/security.md) | The two auth chains, roles, accounts, guarantees |
| [Database](docs/database.md) | Schema and Flyway migrations |
| [Testing](docs/testing.md) | Running tests and per-layer requirements |
| [Agent.md](Agent.md) | Coding standards & governance |
