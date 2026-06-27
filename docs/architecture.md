# Architecture

EchoChamber follows strict DDD / clean architecture. Layer boundaries are hard and are
enforced by the rules in [../Agent.md](../Agent.md).

## Component & data flow

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

## Layer structure

```
domain/          ← pure Kotlin, no framework imports
  model/         ← immutable domain entities (val-only data classes)
  port/          ← interfaces: StorageAdapter, MutationHandler, HttpExecutor,
                   UserStore, AuditStore, PasswordHasher
  mutation/      ← pure helpers: RequestOverrideApplier, BodyPatch

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

## Hard rules

- `domain/` never imports a framework class.
- `application/` never imports a web or persistence class.
- `web/` never calls a repository or adapter directly — only application services.
- Domain models never leave `application/` or `adapter/` as HTTP responses — always map to a DTO first.

## Not yet implemented

These are designed but not built — see the open tickets:

- **R2DBC storage adapter** (TICKET-015) — a reactive `StorageAdapter`.
- **GraalVM `ScriptMutationHandler`** (TICKET-010) — sandboxed JS mutation step.
- **Drop rules** (TICKET-017) and **retention/TTL** (TICKET-018).
