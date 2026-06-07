# TICKET-010 — Script Mutation Handler (GraalVM)

## Summary
Implement `ScriptMutationHandler` — the JS scripting handler that evaluates `ExecutionConfig.mutationScript` against the `MutableRequest` using GraalVM Polyglot in a sandboxed, time-limited context.

## Acceptance Criteria
- [ ] `ScriptMutationHandler` (`order = 100`) in `adapter/mutation/`
- [ ] Only runs if `ExecutionConfig.mutationScript` is non-null and non-blank; otherwise passes through unchanged
- [ ] Evaluates JS using `org.graalvm.polyglot.Context` built with `allowAllAccess(false)` and no host class access
- [ ] Execution is time-limited (hard cap: 2 seconds CPU time). Scripts exceeding the limit are interrupted and result in a handler-level exception (caller marks execution as `FAILURE`)
- [ ] The script receives a plain JS object representation of `MutableRequest` (headers, body, uri, method)
- [ ] The script must `return` the modified request object; the handler maps it back to `MutableRequest`
- [ ] Script runtime errors (syntax errors, thrown exceptions) are caught, logged at WARN level with the script excerpt, and re-thrown as a typed `ScriptMutationException`
- [ ] No host Java class is accessible from within the script

## Dependencies
- **Blocked by:** TICKET-008

## Technical Notes
- GraalVM `Context` should be created fresh per execution (not shared/reused) to prevent state leakage between replays
- Use `Context.newBuilder("js").allowAllAccess(false).build()`
- Pass `MutableRequest` into the script as a `ProxyObject` or as a JSON string that the script parses — JSON string is simpler and safer
- The script environment may expose a utility `hmac(body, key)` function if needed in future iterations — leave a documented hook but do not implement now

## Tests Required
- Unit tests:
  - `mutationScript` is null → request returned unchanged, no GraalVM context created
  - Valid script that modifies a header → `MutableRequest` reflects the change
  - Valid script that rewrites body → `MutableRequest.body` updated
  - Script that exceeds 2s CPU → `ScriptMutationException` thrown
  - Script with syntax error → `ScriptMutationException` thrown, error logged
  - Script that attempts `java.lang.Runtime.getRuntime().exec("ls")` → throws (host access denied)
  - Script that returns `null` → `ScriptMutationException` thrown (invalid return type)
