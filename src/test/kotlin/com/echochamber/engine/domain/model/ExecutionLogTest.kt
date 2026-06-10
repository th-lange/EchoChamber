package com.echochamber.engine.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ExecutionLogTest {

    private fun sample() = ExecutionLog(
        id = UUID.randomUUID(),
        jobId = UUID.randomUUID(),
        requestId = UUID.randomUUID(),
        status = ExecutionStatus.SUCCESS,
        responseStatus = 200,
        responseTimeMs = 42L,
        responseHeaders = mapOf("Content-Type" to "application/json"),
        responseBody = """{"ok":true}""",
        executedAt = Instant.now()
    )

    @Test
    fun `construction sets all fields correctly`() {
        val log = sample()
        assertEquals(ExecutionStatus.SUCCESS, log.status)
        assertEquals(200, log.responseStatus)
        assertEquals(42L, log.responseTimeMs)
        assertEquals(mapOf("Content-Type" to "application/json"), log.responseHeaders)
        assertEquals("""{"ok":true}""", log.responseBody)
    }

    @Test
    fun `equality is structural`() {
        val id = UUID.randomUUID()
        val jobId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val now = Instant.now()
        val a = sample().copy(id = id, jobId = jobId, requestId = requestId, executedAt = now)
        val b = sample().copy(id = id, jobId = jobId, requestId = requestId, executedAt = now)
        assertEquals(a, b)
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
