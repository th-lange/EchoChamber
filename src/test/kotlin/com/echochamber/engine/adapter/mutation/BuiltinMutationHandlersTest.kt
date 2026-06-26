package com.echochamber.engine.adapter.mutation

import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.MutableRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BuiltinMutationHandlersTest {

    private fun request(
        uri: String = "https://prod.example.com/api/resource?q=1",
        headers: Map<String, String> = mapOf("Accept" to "application/json"),
        body: String? = null,
    ) = MutableRequest(
        id = UUID.randomUUID(),
        capturedAt = Instant.now(),
        method = "GET",
        uri = uri,
        authority = "prod.example.com",
        headers = headers,
        body = body,
    )

    private fun config(
        targetBaseUrl: String = "",
        headerOverrides: Map<String, String> = emptyMap(),
        mutationParameters: Map<String, String> = emptyMap(),
    ) = ExecutionConfig(
        id = UUID.randomUUID(),
        name = "c",
        targetBaseUrl = targetBaseUrl,
        mutationScript = null,
        mutationParameters = mutationParameters,
        maxConcurrency = 1,
        rateLimitPerSecond = 0.0,
        headerOverrides = headerOverrides,
    )

    // ----- HeaderOverrideMutationHandler -----

    @Test
    fun `header override replaces existing (case-insensitive) and adds new`() {
        val r = HeaderOverrideMutationHandler().mutate(
            request(headers = mapOf("Accept" to "text/html")),
            config(headerOverrides = mapOf("accept" to "application/json", "X-Env" to "staging")),
        )
        assertEquals("application/json", r.headers.entries.first { it.key.equals("accept", true) }.value)
        assertEquals("staging", r.headers["X-Env"])
        assertEquals(2, r.headers.size)
    }

    @Test
    fun `header override is a no-op when overrides empty`() {
        val original = request()
        val r = HeaderOverrideMutationHandler().mutate(original, config())
        assertEquals(original.headers, r.headers)
    }

    // ----- BaseUrlMutationHandler -----

    @Test
    fun `base url rewrites scheme and authority keeping path and query`() {
        val r = BaseUrlMutationHandler().mutate(request(), config(targetBaseUrl = "http://staging.internal:8080"))
        assertEquals("http://staging.internal:8080/api/resource?q=1", r.uri)
        assertEquals("staging.internal:8080", r.authority)
    }

    @Test
    fun `base url blank is a no-op`() {
        val r = BaseUrlMutationHandler().mutate(request(), config(targetBaseUrl = ""))
        assertEquals("https://prod.example.com/api/resource?q=1", r.uri)
    }

    @Test
    fun `base url malformed request uri passes through`() {
        val r = BaseUrlMutationHandler().mutate(
            request(uri = "::: not a uri :::"),
            config(targetBaseUrl = "http://staging.internal"),
        )
        assertEquals("::: not a uri :::", r.uri)
    }

    // ----- PlaceholderReplacementMutationHandler -----

    @Test
    fun `placeholders replaced in uri header and body, unknown left as-is`() {
        val r = PlaceholderReplacementMutationHandler().mutate(
            request(
                uri = "https://prod.example.com/u/{{userId}}",
                headers = mapOf("X-Tenant" to "{{tenant}}"),
                body = """{"id":"{{userId}}","x":"{{missing}}"}""",
            ),
            config(mutationParameters = mapOf("userId" to "42", "tenant" to "acme")),
        )
        assertEquals("https://prod.example.com/u/42", r.uri)
        assertEquals("acme", r.headers["X-Tenant"])
        assertTrue(r.body!!.contains(""""id":"42""""))
        assertTrue(r.body!!.contains("{{missing}}"))
    }

    @Test
    fun `placeholder handler is a no-op when no parameters`() {
        val original = request(uri = "https://prod.example.com/u/{{userId}}")
        val r = PlaceholderReplacementMutationHandler().mutate(original, config())
        assertEquals("https://prod.example.com/u/{{userId}}", r.uri)
    }

    @Test
    fun `handler orders are 10 20 30`() {
        assertEquals(10, HeaderOverrideMutationHandler().order())
        assertEquals(20, BaseUrlMutationHandler().order())
        assertEquals(30, PlaceholderReplacementMutationHandler().order())
    }
}
