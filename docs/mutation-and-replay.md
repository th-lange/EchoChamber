# Mutation & replay

## Mutation pipeline

The `MutationEngine` runs an ordered chain of `MutationHandler` implementations. Each handler
receives a `MutableRequest` copy — the original `CapturedRequest` is never touched.

| Handler | Order | What it does |
|---|---|---|
| `HeaderOverrideMutationHandler` | 10 | Adds or replaces headers from `ExecutionConfig.headerOverrides` (case-insensitive) |
| `BaseUrlMutationHandler` | 20 | Replaces scheme + authority with `ExecutionConfig.targetBaseUrl` (path + query preserved) |
| `PlaceholderReplacementMutationHandler` | 30 | Substitutes `{{key}}` tokens in the URI, headers, and body using `mutationParameters` |
| `ScriptMutationHandler` _(planned, TICKET-010)_ | 100 | Will run `mutationScript` inside a GraalVM Polyglot JS context (`allowAllAccess(false)`, 2 s CPU limit) — not yet implemented |

After the config-driven chain, an optional per-request **inline override**
(modify-before-retry) is applied **last** — target URL/path, header set/remove, and JSON
body-field patches — so an operator's explicit edit wins over config handlers.

New mutation rules: implement `MutationHandler`, annotate `@Component`, return the desired
`order()` value. The engine sorts handlers by `order()` (stable) and discovers them automatically.

## Replay execution

`ReplayService` drives the async execution loop:

1. Resolve the request set — explicit `requestIds` or a `ReplayFilter`.
2. For each request, copy to a `MutableRequest` and run the `MutationEngine` (+ inline override).
3. Fire the mutated request via `HttpExecutor` (Spring WebClient under the hood).
4. Persist an `ExecutionLog` with the result.
5. Update the `ReplayJob` progress counters.

- Concurrency is bounded by a Kotlin coroutine `Semaphore(maxConcurrency)` (default 10).
- Rate limiting uses Resilience4j (coroutine-suspending) — never `Thread.sleep`; `≤ 0` means unlimited.
- A single request failure (mutation error or HTTP timeout) is logged and counted but never aborts the job.

`ReplayJobScheduler` creates a `PENDING` job, then launches a background coroutine on a
`SupervisorJob + Dispatchers.Default` scope to run `ReplayService`. Active jobs are tracked
by id so they can be cancelled.
