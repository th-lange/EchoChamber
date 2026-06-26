package com.echochamber.engine.application

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.RequestOverride
import com.echochamber.engine.domain.port.MutationHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MutationEngineTest {

    private fun request() = MutableRequest(
        id = UUID.randomUUID(),
        capturedAt = Instant.now(),
        method = "get",
        uri = "https://example.com/api",
        authority = "example.com",
        headers = emptyMap(),
        body = null,
    )

    private fun config() = ExecutionConfig(
        id = UUID.randomUUID(),
        name = "c",
        targetBaseUrl = "https://example.com",
        mutationScript = null,
        mutationParameters = emptyMap(),
        maxConcurrency = 1,
        rateLimitPerSecond = 0.0,
        headerOverrides = emptyMap(),
    )

    private class RecordingHandler(
        private val order: Int,
        private val log: MutableList<Int>,
        private val transform: (MutableRequest) -> Unit = {},
    ) : MutationHandler {
        override fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest {
            log.add(order)
            transform(request)
            return request
        }
        override fun order(): Int = order
    }

    @Test
    fun `no handlers returns request unchanged`() {
        val engine = MutationEngine(emptyList())
        val req = request()
        assertEquals(req, engine.mutate(req, config()))
    }

    @Test
    fun `handlers run in ascending order`() {
        val log = mutableListOf<Int>()
        val engine = MutationEngine(listOf(RecordingHandler(30, log), RecordingHandler(10, log), RecordingHandler(20, log)))
        engine.mutate(request(), config())
        assertEquals(listOf(10, 20, 30), log)
    }

    @Test
    fun `equal order preserves declaration order (stable)`() {
        val log = mutableListOf<Int>()
        val a = object : MutationHandler {
            override fun mutate(r: MutableRequest, c: ExecutionConfig) = r.also { log.add(1) }
            override fun order() = 10
        }
        val b = object : MutationHandler {
            override fun mutate(r: MutableRequest, c: ExecutionConfig) = r.also { log.add(2) }
            override fun order() = 10
        }
        MutationEngine(listOf(a, b)).mutate(request(), config())
        assertEquals(listOf(1, 2), log)
    }

    @Test
    fun `handler exception propagates`() {
        val boom = object : MutationHandler {
            override fun mutate(r: MutableRequest, c: ExecutionConfig): MutableRequest = throw IllegalStateException("boom")
            override fun order() = 10
        }
        assertThrows<IllegalStateException> { MutationEngine(listOf(boom)).mutate(request(), config()) }
    }

    @Test
    fun `inline override is applied after the handler chain`() {
        // a handler sets a header; the override then removes it and sets the path
        val handler = RecordingHandler(10, mutableListOf()) { it.headers = mapOf("X-Added" to "1") }
        val engine = MutationEngine(listOf(handler))
        val result = engine.mutate(
            request(),
            config(),
            RequestOverride(headersRemove = listOf("X-Added"), pathOverride = "/v2"),
        )
        assertEquals(false, result.headers.keys.any { it.equals("X-Added", true) })
        assertEquals("https://example.com/v2", result.uri)
    }
}
