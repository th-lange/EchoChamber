# Security

EchoChamber uses **two independent authentication mechanisms**, wired as separate Spring
Security filter chains:

- **`/internal/**` (machine-to-machine):** `InternalAuthFilter` validates a static Bearer
  token (`INTERNAL_INGEST_TOKEN`, read from env, never hardcoded) and returns `401` before
  any controller logic. **When the token is unset/empty/blank, ingest auth is disabled** and
  requests pass through (convenient for local dev; SnapReq must then send no token). This
  chain is stateless and is **not** routed through the form login;
  its global servlet auto-registration is disabled so the filter neither double-runs nor is
  bypassed.
- **`/admin/**` and `/api/**` (humans):** Spring Security form login backed by the `users`
  table (BCrypt). `DbUserDetailsService` bridges the `UserStore` to Spring Security.

## Roles

| Role | Capabilities |
|---|---|
| `VIEWER` | Read lists and retry history |
| `OPERATOR` | + trigger retries and modify-before-retry |
| `ADMIN` | + manage users and view the audit log |

## Accounts

- The initial ADMIN is bootstrapped from `ADMIN_BOOTSTRAP_USER` / `ADMIN_BOOTSTRAP_PASSWORD`
  on first boot (only if no users exist).
- New accounts are created with a temporary password and must change it on first login
  (a `mustChangePassword` flag + interceptor redirect to `/admin/password`).
- The **last active ADMIN** cannot be disabled, deleted, or demoted.

## Guarantees

- Password hashes are never serialized in any DTO or REST resource; the `users` repository is
  not exposed over Spring Data REST.
- Admin actions (retry, cancel, user create/disable/role/password) are recorded in the
  append-only `audit_log`; replay jobs are attributed to the triggering user. Audit writes are
  best-effort and never abort the primary action.
- Request bodies are never logged at `INFO` level — `DEBUG` only, gated behind a PII flag.
- _Planned (TICKET-010):_ the GraalVM `ScriptMutationHandler` will run scripts with
  `allowAllAccess(false)`, no host class access, and a 2 s CPU limit.
