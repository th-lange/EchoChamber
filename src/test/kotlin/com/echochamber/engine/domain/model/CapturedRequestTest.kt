package com.echochamber.engine.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CapturedRequestTest {

    private val id = UUID.randomUUID()
    private val now = Instant.now()

    private fun sample() = CapturedRequest(
        id = id,
        capturedAt = now,
        method = "GET",
        uri = "/api/test",
        authority = "example.com",
        headers = mapOf("Content-Type" to "application/json"),
        body = null
    )

    @Test
    fun `construction sets all fields correctly`() {
        val req = sample()
        assertEquals(id, req.id)
        assertEquals(now, req.capturedAt)
        assertEquals("GET", req.method)
        assertEquals("/api/test", req.uri)
        assertEquals("example.com", req.authority)
        assertEquals(mapOf("Content-Type" to "application/json"), req.headers)
        assertEquals(null, req.body)
    }

    @Test
    fun `equality is structural`() {
        assertEquals(sample(), sample())
    }

    @Test
    fun `copy produces independent instance with overridden field`() {
        val original = sample()
        val copied = original.copy(method = "POST")
        assertEquals("POST", copied.method)
        assertEquals("GET", original.method)
        assertNotSame(original, copied)
    }

    @Test
    fun `toMutable produces isolated copy`() {
        val original = sample()
        val mutable = original.toMutable()

        assertEquals(original.id, mutable.id)
        assertEquals(original.method, mutable.method)

        mutable.method = "DELETE"
        mutable.uri = "/changed"

        assertEquals("GET", original.method)
        assertEquals("/api/test", original.uri)
    }

    @Test
    fun `toMutable headers map is independent from original`() {
        val original = sample()
        val mutable = original.toMutable()

        mutable.headers = mutable.headers + ("X-Extra" to "value")

        assertEquals(1, original.headers.size)
        assertEquals(2, mutable.headers.size)
    }
}
