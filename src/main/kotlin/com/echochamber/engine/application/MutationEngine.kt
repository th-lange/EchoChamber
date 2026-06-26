package com.echochamber.engine.application

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.RequestOverride
import com.echochamber.engine.domain.mutation.RequestOverrideApplier
import com.echochamber.engine.domain.port.MutationHandler
import org.springframework.stereotype.Service

/**
 * Ordered chain runner for [MutationHandler]s (TICKET-008).
 *
 * Collects all handler beans, sorts them by [MutationHandler.order] ascending (stable, so
 * equal orders keep declaration order), and threads a [MutableRequest] through them. An
 * optional per-request [RequestOverride] (modify-before-retry) is applied **last** so an
 * operator's explicit edit wins over config-driven handlers.
 *
 * Exceptions from handlers are not swallowed — they propagate so the caller can mark the
 * replay `FAILURE`. Lives in `application/` and depends only on the domain port.
 */
@Service
class MutationEngine(
    handlers: List<MutationHandler>,
) {

    private val ordered: List<MutationHandler> = handlers.sortedBy { it.order() }

    fun mutate(
        request: MutableRequest,
        config: ExecutionConfig,
        override: RequestOverride? = null,
    ): MutableRequest {
        var current = request
        for (handler in ordered) {
            current = handler.mutate(current, config)
        }
        if (override != null && !override.isEmpty) {
            current = RequestOverrideApplier.apply(current, override)
        }
        return current
    }
}
