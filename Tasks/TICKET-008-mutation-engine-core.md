# TICKET-008 — Mutation Engine Core

## Summary
Implement `MutationEngine` — the ordered chain runner that collects all `MutationHandler` beans and applies them in sequence to a `MutableRequest`.

## Acceptance Criteria
- [ ] `MutationEngine` in `application/` (not `adapter/`)
- [ ] Injects `List<MutationHandler>` from Spring context, sorted by `handler.order()` ascending
- [ ] Iterates the sorted list and calls `mutate(request, config)` on each, threading the result forward
- [ ] Returns the final `MutableRequest` after all handlers have run
- [ ] If any handler throws, the exception propagates and the replay is marked `FAILURE` by the caller — `MutationEngine` does not swallow exceptions
- [ ] `MutationEngine` has no knowledge of the database, HTTP client, or Spring web layer

## Dependencies
- **Blocked by:** TICKET-003 (MutationHandler interface)

## Technical Notes
- Sorting must be stable — two handlers with the same `order()` should maintain declaration order as a tiebreaker
- `MutationEngine` is a Spring `@Service`

## Tests Required
- Unit tests with mock `MutationHandler` implementations:
  - Zero handlers → original `MutableRequest` returned unchanged
  - Single handler → handler called once, result returned
  - Two handlers with different orders → lower order runs first
  - Two handlers with same order → both run, order is consistent
  - Handler that throws → exception propagates from `MutationEngine`
