# TICKET-005 — Internal Auth Filter

## Summary
Implement `InternalAuthFilter` to guard all `/internal/**` routes with a static Bearer token. Requests without a valid token never reach a controller.

## Acceptance Criteria
- [ ] `InternalAuthFilter` extends `OncePerRequestFilter` (WebMVC) or implements `WebFilter` (WebFlux)
- [ ] Filter only activates for paths matching `/internal/**`
- [ ] Token is read exclusively from the environment variable `INTERNAL_INGEST_TOKEN` — never hardcoded
- [ ] Missing `Authorization` header → `401 Unauthorized`
- [ ] Header present but value does not match → `401 Unauthorized`
- [ ] Correct token → request passes through to controller
- [ ] Filter is registered as a Spring `@Component` — no manual `FilterRegistrationBean` required

## Dependencies
- **Blocked by:** TICKET-001

## Technical Notes
- Expected header format: `Authorization: Bearer <token>`
- Token comparison must use a constant-time equality check to prevent timing attacks (`MessageDigest.isEqual` on byte arrays)
- Do not log the token value at any log level

## Tests Required
- Unit tests for filter logic (mock `HttpServletRequest` / `ServerWebExchange`):
  - Missing `Authorization` header → 401
  - `Authorization: Bearer wrong-token` → 401
  - `Authorization: Bearer correct-token` → chain proceeds
  - Path `/api/configs` (not internal) → filter skips entirely
- Integration test: `POST /internal/ingest` without token returns 401 end-to-end
