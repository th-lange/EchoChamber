package com.echochamber.engine.adapter.mutation

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.mutation.BodyPatch
import com.echochamber.engine.domain.port.MutationHandler
import org.springframework.stereotype.Component

/**
 * Replaces `{{key}}` placeholder tokens in the URI, header values, and body using
 * [ExecutionConfig.mutationParameters]. Unknown placeholders are left as-is. Delegates the
 * token substitution to the shared [BodyPatch] helper. Order 30.
 */
@Component
class PlaceholderReplacementMutationHandler : MutationHandler {

    override fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest {
        val params = config.mutationParameters
        if (params.isEmpty()) return request
        request.uri = BodyPatch.applyPlaceholders(request.uri, params) ?: request.uri
        request.headers = request.headers.mapValues { (_, v) -> BodyPatch.applyPlaceholders(v, params) ?: v }
        request.body = BodyPatch.applyPlaceholders(request.body, params)
        return request
    }

    override fun order(): Int = 30
}
