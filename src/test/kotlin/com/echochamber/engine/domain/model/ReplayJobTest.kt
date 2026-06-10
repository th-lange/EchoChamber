package com.echochamber.engine.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ReplayJobTest {

    private fun sample() = ReplayJob(
        id = UUID.randomUUID(),
        configId = UUID.randomUUID(),
        status = ReplayJobStatus.PENDING,
        totalRequests = 100,
        processedRequests = 0,
        failedRequests = 0,
        startedAt = null,
        completedAt = null
    )

    @Test
    fun `construction sets all fields correctly`() {
        val job = sample()
        assertEquals(ReplayJobStatus.PENDING, job.status)
        assertEquals(100, job.totalRequests)
        assertEquals(0, job.processedRequests)
        assertEquals(0, job.failedRequests)
        assertNull(job.startedAt)
        assertNull(job.completedAt)
    }

    @Test
    fun `equality is structural`() {
        val id = UUID.randomUUID()
        val configId = UUID.randomUUID()
        val a = sample().copy(id = id, configId = configId)
        val b = sample().copy(id = id, configId = configId)
        assertEquals(a, b)
    }

    @Test
    fun `copy produces independent instance with overridden field`() {
        val original = sample()
        val copied = original.copy(status = ReplayJobStatus.RUNNING)
        assertEquals(ReplayJobStatus.RUNNING, copied.status)
        assertEquals(ReplayJobStatus.PENDING, original.status)
        assertNotSame(original, copied)
    }

    @Test
    fun `status transitions PENDING to RUNNING to COMPLETED`() {
        val job = sample()
        val running = job.copy(status = ReplayJobStatus.RUNNING, startedAt = Instant.now())
        val completed = running.copy(status = ReplayJobStatus.COMPLETED, completedAt = Instant.now())

        assertEquals(ReplayJobStatus.PENDING, job.status)
        assertEquals(ReplayJobStatus.RUNNING, running.status)
        assertEquals(ReplayJobStatus.COMPLETED, completed.status)
        assertNull(job.startedAt)
        assertNull(running.completedAt)
    }

    @Test
    fun `status transitions PENDING to RUNNING to FAILED`() {
        val job = sample()
        val running = job.copy(status = ReplayJobStatus.RUNNING, startedAt = Instant.now())
        val failed = running.copy(status = ReplayJobStatus.FAILED, completedAt = Instant.now())

        assertEquals(ReplayJobStatus.FAILED, failed.status)
    }
}
