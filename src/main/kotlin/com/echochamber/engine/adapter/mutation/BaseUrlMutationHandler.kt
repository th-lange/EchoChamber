package com.echochamber.engine.adapter.mutation

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.port.MutationHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Replaces the scheme + authority of the request URI with [ExecutionConfig.targetBaseUrl]
 * (path + query preserved), so captured production traffic can be replayed at a different
 * target. Blank config or a malformed URI passes the request through unchanged. Order 20.
 */
@Component
class BaseUrlMutationHandler : MutationHandler {

    private val log = LoggerFactory.getLogger(BaseUrlMutationHandler::class.java)

    override fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest {
        if (config.targetBaseUrl.isBlank()) return request
        return try {
            val base = URI.create(config.targetBaseUrl)
            val current = URI.create(request.uri)
            val scheme = base.scheme ?: current.scheme
            val authority = base.authority ?: current.authority
            request.uri = buildString {
                append(scheme).append("://").append(authority).append(current.rawPath ?: "")
                current.rawQuery?.let { append("?").append(it) }
            }
            request.authority = authority
            request
        } catch (e: IllegalArgumentException) {
            log.warn("Malformed URI in base-url mutation (config='{}', uri='{}'): {}", config.targetBaseUrl, request.uri, e.message)
            request
        }
    }

    override fun order(): Int = 20
}
