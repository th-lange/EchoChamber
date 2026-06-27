package com.echochamber.engine.adapter.http

import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.port.HttpExecutor
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeoutException

/**
 * [HttpExecutor] backed by Spring's reactive [WebClient].
 *
 * Contract (Agent.md): never throws for normal HTTP failures — every outcome is returned
 * as an [ExecutionResult]. A received HTTP response (including 4xx/5xx) is [ExecutionStatus.SUCCESS]
 * with the status code in `responseStatus`; a timeout is [ExecutionStatus.TIMEOUT]; any other
 * transport error is [ExecutionStatus.FAILURE]. `responseTimeMs` is measured around the call.
 */
@Component
class WebClientHttpExecutor(
    private val webClient: WebClient,
) : HttpExecutor {

    private val log = LoggerFactory.getLogger(WebClientHttpExecutor::class.java)

    override suspend fun execute(request: MutableRequest): ExecutionResult {
        val start = System.nanoTime()
        fun elapsedMs() = (System.nanoTime() - start) / 1_000_000

        return try {
            val base = webClient
                .method(HttpMethod.valueOf(request.method.uppercase(Locale.ROOT)))
                .uri(URI.create(request.uri))
                .headers { h -> request.headers.forEach { (k, v) -> h.set(k, v) } }

            val spec: WebClient.RequestHeadersSpec<*> =
                request.body?.let { base.bodyValue(it) } ?: base

            spec.awaitExchange { response ->
                val body = response.awaitBodyOrNull<String>()
                ExecutionResult(
                    status = ExecutionStatus.SUCCESS,
                    responseStatus = response.statusCode().value(),
                    responseTimeMs = elapsedMs(),
                    responseHeaders = response.headers().asHttpHeaders().toSingleValueMap(),
                    responseBody = body,
                )
            }
        } catch (e: Exception) {
            val timedOut = generateSequence(e as Throwable?) { it.cause }
                .any { it is TimeoutException || it::class.qualifiedName?.contains("Timeout", ignoreCase = true) == true }
            val status = if (timedOut) ExecutionStatus.TIMEOUT else ExecutionStatus.FAILURE
            log.debug("Replay HTTP {} for {} {}: {}", status, request.method, request.uri, e.toString())
            ExecutionResult(
                status = status,
                responseStatus = null,
                responseTimeMs = elapsedMs(),
                responseHeaders = emptyMap(),
                responseBody = null,
            )
        }
    }
}
