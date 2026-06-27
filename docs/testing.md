# Testing

Run the suite:

```bash
./gradlew test                          # unit tests (integration tests skip without Docker)
./gradlew test -Drun.integration=true   # + Testcontainers integration tests (needs Docker)
```

Integration tests are gated behind `-Drun.integration=true` and use **Testcontainers with a
real PostgreSQL** — never an in-memory DB. CI (`.github/workflows/ci.yml`) runs both on every
push/PR.

## Required test type per layer

| Layer | Required test |
|---|---|
| Domain model | Unit — construction, immutability, equality |
| Port interface | Contract test — any implementation must satisfy the interface |
| Application service | Unit with faked ports |
| JPA / R2DBC adapter | Integration test via Testcontainers (real PostgreSQL) |
| Mutation handlers | Unit test per handler |
| `InternalAuthFilter` | Unit tests: valid token, missing token, wrong token |
| Controllers | `@WebMvcTest` / `@SpringBootTest` with `MockMvc` |
| Ingestion endpoint | Valid payload persisted; invalid payload rejected; `202` returned |
| Replay trigger / cancel | RBAC enforced; job created and `202` returned; cancel transitions |
| Auth & users | RBAC matrix; last-active-admin guard; password hashing |
| Audit | Entry written per action; a failing audit store never aborts the caller |

> Note: suspend (`/admin`, `/api`) controllers are async — `@WebMvcTest` slices dispatch the
> async result (`asyncDispatch`) to assert logic-based status codes.
