package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.port.HttpExecutor
import com.echochamber.engine.domain.port.MutationHandler
import com.echochamber.engine.support.FakeStorageAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class ReplayServiceTest {

    private val storage = FakeStorageAdapter()

    private fun config(maxConcurrency: Int = 10, rate: Double = 0.0): ExecutionConfig {
        val c = ExecutionConfig(
            id = UUID.randomUUID(),
            name = "c",
            targetBaseUrl = "",
            mutationScript = null,
            mutationParameters = emptyMap(),
            maxConcurrency = maxConcurrency,
            rateLimitPerSecond = rate,
            headerOverrides = emptyMap(),
        )
        storage.configs[c.id] = c
        return c
    }

    private fun job(configId: UUID): ReplayJob {
        val j = ReplayJob(UUID.randomUUID(), configId, ReplayJobStatus.PENDING, 0, 0, 0, null, null)
        storage.jobs[j.id] = j
        return j
    }

    private fun seedRequests(n: Int): List<UUID> = (1..n).map {
        val r = CapturedRequest(UUID.randomUUID(), Instant.now(), "GET", "http://t/$it", "t", emptyMap(), null)
        storage.requests.add(r)
        r.id
    }

    private fun okExecutor() = object : HttpExecutor {
        override suspend fun execute(request: MutableRequest) =
            ExecutionResult(ExecutionStatus.SUCCESS, 200, 1, emptyMap(), "ok")
    }

    @Test
    fun `single request is mutated, executed, logged, job completed`() = runBlocking {
        val cfg = config()
        val j = job(cfg.id)
        val ids = seedRequests(1)
        val service = ReplayService(storage, MutationEngine(emptyList()), okExecutor())

        service.executeJob(j.id, ReplaySelection(requestIds = ids))

        val done = storage.jobs[j.id]!!
        assertEquals(ReplayJobStatus.COMPLETED, done.status)
        assertEquals(1, done.processedRequests)
        assertEquals(0, done.failedRequests)
        assertEquals(1, storage.logs.size)
        assertEquals(ExecutionStatus.SUCCESS, storage.logs.first().status)
    }

    @Test
    fun `mutation error is logged as FAILURE and job continues to COMPLETED`() = runBlocking {
        val cfg = config()
        val j = job(cfg.id)
        val ids = seedRequests(1)
        val throwing = object : MutationHandler {
            override fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest = throw IllegalStateException("boom")
            override fun order() = 10
        }
        val service = ReplayService(storage, MutationEngine(listOf(throwing)), okExecutor())

        service.executeJob(j.id, ReplaySelection(requestIds = ids))

        val done = storage.jobs[j.id]!!
        assertEquals(ReplayJobStatus.COMPLETED, done.status)
        assertEquals(1, done.failedRequests)
        assertEquals(ExecutionStatus.FAILURE, storage.logs.first().status)
    }

    @Test
    fun `timeout result is counted as failed`() = runBlocking {
        val cfg = config()
        val j = job(cfg.id)
        val ids = seedRequests(1)
        val timeoutExec = object : HttpExecutor {
            override suspend fun execute(request: MutableRequest) =
                ExecutionResult(ExecutionStatus.TIMEOUT, null, 0, emptyMap(), null)
        }
        val service = ReplayService(storage, MutationEngine(emptyList()), timeoutExec)

        service.executeJob(j.id, ReplaySelection(requestIds = ids))

        assertEquals(1, storage.jobs[j.id]!!.failedRequests)
        assertEquals(ExecutionStatus.TIMEOUT, storage.logs.first().status)
    }

    @Test
    fun `concurrency never exceeds maxConcurrency`() = runBlocking {
        val cfg = config(maxConcurrency = 2)
        val j = job(cfg.id)
        val ids = seedRequests(10)
        val inFlight = AtomicInteger(0)
        val maxObserved = AtomicInteger(0)
        val trackingExec = object : HttpExecutor {
            override suspend fun execute(request: MutableRequest): ExecutionResult {
                val c = inFlight.incrementAndGet()
                maxObserved.updateAndGet { maxOf(it, c) }
                delay(40)
                inFlight.decrementAndGet()
                return ExecutionResult(ExecutionStatus.SUCCESS, 200, 1, emptyMap(), null)
            }
        }
        val service = ReplayService(storage, MutationEngine(emptyList()), trackingExec)

        service.executeJob(j.id, ReplaySelection(requestIds = ids))

        assertTrue(maxObserved.get() <= 2, "max concurrent was ${maxObserved.get()}")
        assertEquals(10, storage.jobs[j.id]!!.processedRequests)
    }

    @Test
    fun `rate limit spreads requests across time`() = runBlocking {
        val cfg = config(maxConcurrency = 10, rate = 2.0)
        val j = job(cfg.id)
        val ids = seedRequests(4)
        val service = ReplayService(storage, MutationEngine(emptyList()), okExecutor())

        val start = System.currentTimeMillis()
        service.executeJob(j.id, ReplaySelection(requestIds = ids))
        val elapsed = System.currentTimeMillis() - start

        // 2 permits/sec -> 4 requests need at least one ~1s refresh wait.
        assertTrue(elapsed >= 800, "elapsed was ${elapsed}ms")
        assertEquals(4, storage.jobs[j.id]!!.processedRequests)
    }

    @Test
    fun `missing config marks job FAILED`() = runBlocking {
        val j = job(UUID.randomUUID()) // config id not in storage
        val service = ReplayService(storage, MutationEngine(emptyList()), okExecutor())

        service.executeJob(j.id, ReplaySelection(requestIds = emptyList()))

        assertEquals(ReplayJobStatus.FAILED, storage.jobs[j.id]!!.status)
    }
}
