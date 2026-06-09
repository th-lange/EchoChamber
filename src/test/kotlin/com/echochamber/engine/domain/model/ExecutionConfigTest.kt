package com.echochamber.engine.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.util.UUID

class ExecutionConfigTest {

    private fun sample() = ExecutionConfig(
        id = UUID.randomUUID(),
        name = "Test Config",
        targetBaseUrl = "https://target.example.com",
        mutationScript = null,
        mutationParameters = mapOf("key" to "value"),
        maxConcurrency = 10,
        rateLimitPerSecond = 5.0,
        headerOverrides = mapOf("X-Custom" to "header")
    )

    @Test
    fun `construction sets all fields correctly`() {
        val config = sample()
        assertEquals("Test Config", config.name)
        assertEquals("https://target.example.com", config.targetBaseUrl)
        assertEquals(null, config.mutationScript)
        assertEquals(mapOf("key" to "value"), config.mutationParameters)
        assertEquals(10, config.maxConcurrency)
        assertEquals(5.0, config.rateLimitPerSecond)
        assertEquals(mapOf("X-Custom" to "header"), config.headerOverrides)
    }

    @Test
    fun `equality is structural`() {
        val id = UUID.randomUUID()
        val a = sample().copy(id = id)
        val b = sample().copy(id = id)
        assertEquals(a, b)
    }

    @Test
    fun `copy produces independent instance with overridden field`() {
        val original = sample()
        val copied = original.copy(maxConcurrency = 20)
        assertEquals(20, copied.maxConcurrency)
        assertEquals(10, original.maxConcurrency)
        assertNotSame(original, copied)
    }
}
