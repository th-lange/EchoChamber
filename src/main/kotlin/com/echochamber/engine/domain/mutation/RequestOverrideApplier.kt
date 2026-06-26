package com.echochamber.engine.domain.mutation

import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.RequestOverride
import java.net.URI

/**
 * Applies a [RequestOverride] to a [MutableRequest] in place. Pure domain logic — no
 * framework dependencies. Applied last in the mutation chain so an operator's explicit
 * edit wins over config-driven handlers.
 */
object RequestOverrideApplier {

    fun apply(request: MutableRequest, override: RequestOverride): MutableRequest {
        if (override.isEmpty) return request

        if (override.targetUrl != null || override.pathOverride != null) {
            val current = URI.create(request.uri)
            var scheme = current.scheme
            var authority = current.authority
            var path = current.rawPath
            val query = current.rawQuery

            override.targetUrl?.let { target ->
                val t = URI.create(target)
                t.scheme?.let { scheme = it }
                t.authority?.let { authority = it }
            }
            override.pathOverride?.let { path = if (it.startsWith("/")) it else "/$it" }

            request.uri = buildString {
                append(scheme).append("://").append(authority).append(path ?: "")
                if (query != null) append("?").append(query)
            }
            request.authority = authority
        }

        if (override.headersSet.isNotEmpty() || override.headersRemove.isNotEmpty()) {
            val merged = LinkedHashMap(request.headers)
            override.headersRemove.forEach { name ->
                merged.keys.filter { it.equals(name, ignoreCase = true) }.toList().forEach { merged.remove(it) }
            }
            override.headersSet.forEach { (name, value) ->
                merged.keys.filter { it.equals(name, ignoreCase = true) }.toList().forEach { merged.remove(it) }
                merged[name] = value
            }
            request.headers = merged
        }

        if (override.bodyPatches.isNotEmpty()) {
            request.body = BodyPatch.applyFieldPatches(request.body, override.bodyPatches)
        }

        return request
    }
}
