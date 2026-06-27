package com.echochamber.engine.adapter.mutation

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.port.MutationHandler
import org.springframework.stereotype.Component

/**
 * Merges [ExecutionConfig.headerOverrides] on top of the request headers (config values win
 * on conflict, case-insensitive on name). Order 10.
 */
@Component
class HeaderOverrideMutationHandler : MutationHandler {

    override fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest {
        if (config.headerOverrides.isEmpty()) return request
        val merged = LinkedHashMap(request.headers)
        config.headerOverrides.forEach { (name, value) ->
            merged.keys.filter { it.equals(name, ignoreCase = true) }.toList().forEach { merged.remove(it) }
            merged[name] = value
        }
        request.headers = merged
        return request
    }

    override fun order(): Int = 10
}
