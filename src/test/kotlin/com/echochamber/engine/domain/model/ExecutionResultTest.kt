package com.echochamber.engine.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class ExecutionResultTest {

    private fun sample() = ExecutionResult(
        status = ExecutionStatus.SUCCESS,
        responseStatus = 200,
        responseTimeMs = 42L,
        responseHeaders = mapOf("Content-Type" to "application/json"),
        responseBody = "{\"ok\":true}"
    )

    @Test
    fun `construction sets all fields correctly`() {
        val r = sample()
        assertEquals(ExecutionStatus.SUCCESS, r.status)
        assertEquals(200, r.responseStatus)
        assertEquals(42L, r.responseTimeMs)
        assertEquals(mapOf("Content-Type" to "application/json"), r.responseHeaders)
        assertEquals("{\"ok\":true}", r.responseBody)
    }

    @Test
    fun `nullable fields accept null`() {
        val r = ExecutionResult(
            status = ExecutionStatus.TIMEOUT,
            responseStatus = null,
            responseTimeMs = 2000L,
            responseHeaders = emptyMap(),
            responseBody = null
        )
        assertEquals(ExecutionStatus.TIMEOUT, r.status)
        assertEquals(null, r.responseStatus)
        assertEquals(null, r.responseBody)
    }

    @Test
    fun `equality is structural`() {
        assertEquals(sample(), sample())
    }

    @Test
    fun `copy produces independent instance with overridden field`() {
        val original = sample()
        val copied = original.copy(status = ExecutionStatus.FAILURE)
        assertEquals(ExecutionStatus.FAILURE, copied.status)
        assertEquals(ExecutionStatus.SUCCESS, original.status)
        assertNotSame(original, copied)
    }
}
