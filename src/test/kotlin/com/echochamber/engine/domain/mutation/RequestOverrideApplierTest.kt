package com.echochamber.engine.domain.mutation

import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.RequestOverride
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RequestOverrideApplierTest {

    private fun request(
        uri: String = "https://old.example.com/api/resource?q=1",
        headers: Map<String, String> = mapOf("Authorization" to "secret", "Accept" to "application/json"),
        body: String? = """{"userId":1,"keep":"yes"}""",
    ) = MutableRequest(
        id = UUID.randomUUID(),
        capturedAt = Instant.now(),
        method = "POST",
        uri = uri,
        authority = "old.example.com",
        headers = headers,
        body = body,
    )

    @Test
    fun `targetUrl replaces scheme and authority but keeps path and query`() {
        val r = RequestOverrideApplier.apply(request(), RequestOverride(targetUrl = "http://new-host:9090"))
        assertEquals("http://new-host:9090/api/resource?q=1", r.uri)
        assertEquals("new-host:9090", r.authority)
    }

    @Test
    fun `pathOverride replaces the path and preserves query`() {
        val r = RequestOverrideApplier.apply(request(), RequestOverride(pathOverride = "/v2/resource"))
        assertEquals("https://old.example.com/v2/resource?q=1", r.uri)
    }

    @Test
    fun `headersSet overrides case-insensitively and headersRemove drops`() {
        val r = RequestOverrideApplier.apply(
            request(),
            RequestOverride(headersSet = mapOf("accept" to "text/plain"), headersRemove = listOf("authorization")),
        )
        assertFalse(r.headers.keys.any { it.equals("authorization", true) })
        assertEquals("text/plain", r.headers.entries.first { it.key.equals("accept", true) }.value)
    }

    @Test
    fun `bodyPatches replaces a top-level json field value`() {
        val r = RequestOverrideApplier.apply(request(), RequestOverride(bodyPatches = mapOf("userId" to "42")))
        assertTrue(r.body!!.contains(""""userId":"42""""), "got: ${r.body}")
        assertTrue(r.body!!.contains(""""keep":"yes""""))
    }

    @Test
    fun `empty override is a no-op`() {
        val original = request()
        val r = RequestOverrideApplier.apply(original, RequestOverride())
        assertEquals("https://old.example.com/api/resource?q=1", r.uri)
    }
}
