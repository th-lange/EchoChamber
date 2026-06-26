package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.port.HttpExecutor
import com.echochamber.engine.support.FakeStorageAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ReplayJobSchedulerTest {

    private val storage = FakeStorageAdapter()

    private fun config(): ExecutionConfig {
        val c = ExecutionConfig(UUID.randomUUID(), "c", "", null, emptyMap(), 5, 0.0, emptyMap())
        storage.configs[c.id] = c
        return c
    }

    private fun seedRequests(n: Int): List<UUID> = (1..n).map {
        val r = CapturedRequest(UUID.randomUUID(), Instant.now(), "GET", "http://t/$it", "t", emptyMap(), null)
        storage.requests.add(r)
        r.id
    }

    private suspend fun awaitStatus(id: UUID, expected: ReplayJobStatus, timeoutMs: Long = 3000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (storage.jobs[id]?.status == expected) return true
            delay(20)
        }
        return false
    }

    @Test
    fun `scheduleJob creates a PENDING job and runs it to COMPLETED asynchronously`() = runBlocking {
        val cfg = config()
        val ids = seedRequests(2)
        val scheduler = ReplayJobScheduler(
            storage,
            ReplayService(storage, MutationEngine(emptyList()), okExecutor()),
        )

        val job = scheduler.scheduleJob(cfg.id, ReplaySelection(requestIds = ids))

        assertNotNull(storage.jobs[job.id])
        assertTrue(awaitStatus(job.id, ReplayJobStatus.COMPLETED), "job did not complete")
        assertEquals(2, storage.jobs[job.id]!!.processedRequests)
    }

    @Test
    fun `cancelJob returns false for an unknown job`() = runBlocking {
        val scheduler = ReplayJobScheduler(storage, ReplayService(storage, MutationEngine(emptyList()), okExecutor()))
        assertFalse(scheduler.cancelJob(UUID.randomUUID()))
    }

    @Test
    fun `cancelJob aborts a running job which ends FAILED`() = runBlocking {
        val cfg = config()
        val ids = seedRequests(20)
        val slowExec = object : HttpExecutor {
            override suspend fun execute(request: MutableRequest): ExecutionResult {
                delay(500)
                return ExecutionResult(ExecutionStatus.SUCCESS, 200, 1, emptyMap(), null)
            }
        }
        val scheduler = ReplayJobScheduler(storage, ReplayService(storage, MutationEngine(emptyList()), slowExec))

        val job = scheduler.scheduleJob(cfg.id, ReplaySelection(requestIds = ids))
        // let it start
        awaitStatus(job.id, ReplayJobStatus.RUNNING)
        val cancelled = scheduler.cancelJob(job.id)

        assertTrue(cancelled)
        assertTrue(awaitStatus(job.id, ReplayJobStatus.FAILED), "cancelled job did not end FAILED")
    }

    private fun okExecutor() = object : HttpExecutor {
        override suspend fun execute(request: MutableRequest) =
            ExecutionResult(ExecutionStatus.SUCCESS, 200, 1, emptyMap(), null)
    }
}
