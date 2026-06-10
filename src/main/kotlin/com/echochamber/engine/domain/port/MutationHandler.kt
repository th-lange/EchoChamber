package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest

/**
 * Strategy port for a single mutation step applied to a [MutableRequest] before replay.
 *
 * Handlers are run in ascending [order] by the `MutationEngine`. Each handler must return
 * the request it received (possibly mutated). Returning a `copy()` is also valid.
 *
 * Implementations live in the `adapter/mutation/` package and must not leak framework
 * types through this interface.
 */
interface MutationHandler {

    /**
     * Apply this handler's transformation. The returned instance is passed to the next
     * handler in the chain.
     */
    fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest

    /**
     * Lower values run earlier. Built-in handlers reserve 0..99; user-supplied handlers
     * should pick a value >= 100 unless they explicitly want to interleave with built-ins.
     */
    fun order(): Int
}
