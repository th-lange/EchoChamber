# TICKET-009 — Built-in Mutation Handlers

## Summary
Implement the three deterministic `MutationHandler` implementations that cover the common replay mutation cases: header overrides, base URL substitution, and placeholder replacement.

## Acceptance Criteria
- [ ] `HeaderOverrideMutationHandler` (`order = 10`) in `adapter/mutation/`:
  - Merges `ExecutionConfig.headerOverrides` on top of `MutableRequest.headers` (config values win on conflict)
- [ ] `BaseUrlMutationHandler` (`order = 20`) in `adapter/mutation/`:
  - If `ExecutionConfig.baseUrlOverride` is set, replaces scheme + authority in `MutableRequest.uri`
  - If not set, passes request through unchanged
- [ ] `PlaceholderReplacementMutationHandler` (`order = 30`) in `adapter/mutation/`:
  - Replaces `{{key}}` tokens in `MutableRequest.uri`, `MutableRequest.headers` values, and `MutableRequest.body` using `ExecutionConfig.mutationParameters`
  - Unknown placeholders are left as-is (no error)
- [ ] All three are `@Component` beans — auto-discovered by `MutationEngine`
- [ ] None imports any Spring web or persistence class

## Dependencies
- **Blocked by:** TICKET-008 (MutationEngine must exist to understand the contract)

## Technical Notes
- `BaseUrlMutationHandler` parses the URI with `java.net.URI` — handle malformed URIs gracefully (log warning, pass through)
- Placeholder regex: `\{\{(\w+)\}\}` — compile once as a constant, not per invocation

## Tests Required
- Unit test per handler — each tested in isolation with a crafted `MutableRequest` and `ExecutionConfig`:
  - `HeaderOverrideMutationHandler`: override existing header, add new header, no-op when overrides map is empty
  - `BaseUrlMutationHandler`: URL is rewritten correctly, no-op when override is null, malformed URI is passed through
  - `PlaceholderReplacementMutationHandler`: token in body replaced, token in header replaced, unknown token left as-is, multiple tokens in one body all replaced
