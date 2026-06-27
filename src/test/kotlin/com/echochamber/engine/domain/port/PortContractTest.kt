package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.model.toMutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Contract / compile-time test.
 *
 * This file exists to prove two things at build time:
 *
 * 1. The three domain ports ([StorageAdapter], [MutationHandler], [HttpExecutor]) are
 *    implementable using only pure-Kotlin / JDK / kotlinx.coroutines.flow types — i.e.
 *    no framework dependency leaks through the interfaces.
 * 2. Each contract behaves as documented when exercised against a trivial in-memory
 *    implementation.
 *
 * The dummy implementations below intentionally use only `java.util` collections, JDK
 * types, and `kotlinx.coroutines.flow`. If anyone adds a framework type to a port
 * signature, this file will fail to compile.
 */
class PortContractTest {

    // ---------------------------------------------------------------------------
    // Dummy implementations — pure Kotlin only.
    // ---------------------------------------------------------------------------

    private class InMemoryStorageAdapter : StorageAdapter {
        private val requests = ConcurrentHashMap<UUID, CapturedRequest>()
        private val configs = ConcurrentHashMap<UUID, ExecutionConfig>()
        private val jobs = ConcurrentHashMap<UUID, ReplayJob>()
        private val logs = ConcurrentHashMap<UUID, ExecutionLog>()

        override suspend fun appendRequest(r: CapturedRequest): CapturedRequest {
            requests[r.id] = r
            return r
        }

        override suspend fun findRequest(id: UUID): CapturedRequest? = requests[id]

        override suspend fun findRecentDuplicate(
            method: String,
            uri: String,
            authority: String,
            notBefore: Instant,
        ): CapturedRequest? = requests.values.firstOrNull {
            it.method == method && it.uri == uri && it.authority == authority && !it.capturedAt.isBefore(notBefore)
        }

        override fun findRequests(filter: ReplayFilter): Flow<CapturedRequest> =
            requests.values
                .filter { r -> filter.method == null || r.method == filter.method }
                .filter { r -> filter.authority == null || r.authority == filter.authority }
                .asFlow()

        override suspend fun saveConfig(c: ExecutionConfig): ExecutionConfig {
            configs[c.id] = c
            return c
        }

        override suspend fun findConfig(id: UUID): ExecutionConfig? = configs[id]

        override suspend fun deleteConfig(id: UUID) {
            configs.remove(id)
        }

        override suspend fun saveJob(j: ReplayJob): ReplayJob {
            jobs[j.id] = j
            return j
        }

        override suspend fun updateJob(j: ReplayJob): ReplayJob {
            jobs[j.id] = j
            return j
        }

        override suspend fun getJob(id: UUID): ReplayJob? = jobs[id]

        override suspend fun saveLog(l: ExecutionLog): ExecutionLog {
            logs[l.id] = l
            return l
        }

        override suspend fun listJobs(limit: Int): List<ReplayJob> = jobs.values.toList().take(limit)

        override suspend fun listLogs(limit: Int): List<ExecutionLog> = logs.values.toList().take(limit)

        override suspend fun executionCountsByRequest(): Map<UUID, Long> =
            logs.values.groupingBy { it.requestId }.eachCount().mapValues { it.value.toLong() }
    }

    private class UppercaseMethodHandler(private val order: Int) : MutationHandler {
        override fun mutate(request: MutableRequest, config: ExecutionConfig): MutableRequest {
            request.method = request.method.uppercase()
            return request
        }

        override fun order(): Int = order
    }

    private class StubHttpExecutor : HttpExecutor {
        override suspend fun execute(request: MutableRequest): ExecutionResult =
            ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                responseStatus = 200,
                responseTimeMs = 1,
                responseHeaders = mapOf("Content-Type" to "application/json"),
                responseBody = "{\"ok\":true}"
            )
    }

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private fun sampleRequest(
        id: UUID = UUID.randomUUID(),
        method: String = "GET",
        authority: String = "example.com"
    ) = CapturedRequest(
        id = id,
        capturedAt = Instant.parse("2026-01-01T00:00:00Z"),
        method = method,
        uri = "/api/test",
        authority = authority,
        headers = mapOf("Accept" to "application/json"),
        body = null
    )

    private fun sampleConfig(id: UUID = UUID.randomUUID()) = ExecutionConfig(
        id = id,
        name = "test",
        targetBaseUrl = "http://localhost",
        mutationScript = null,
        mutationParameters = emptyMap(),
        maxConcurrency = 1,
        rateLimitPerSecond = 1.0,
        headerOverrides = emptyMap()
    )

    private fun sampleJob(id: UUID = UUID.randomUUID(), configId: UUID = UUID.randomUUID()) = ReplayJob(
        id = id,
        configId = configId,
        status = ReplayJobStatus.PENDING,
        totalRequests = 0,
        processedRequests = 0,
        failedRequests = 0,
        startedAt = null,
        completedAt = null
    )

    // ---------------------------------------------------------------------------
    // StorageAdapter contract
    // ---------------------------------------------------------------------------

    @Test
    fun `StorageAdapter can be implemented with no framework dependencies`() = runBlocking {
        val storage: StorageAdapter = InMemoryStorageAdapter()

        val req = sampleRequest()
        storage.appendRequest(req)
        assertEquals(req, storage.findRequest(req.id))
        assertNull(storage.findRequest(UUID.randomUUID()))
    }

    @Test
    fun `findRequests returns a Flow honouring the filter`() = runBlocking {
        val storage: StorageAdapter = InMemoryStorageAdapter()
        storage.appendRequest(sampleRequest(method = "GET", authority = "a.example.com"))
        storage.appendRequest(sampleRequest(method = "POST", authority = "a.example.com"))
        storage.appendRequest(sampleRequest(method = "GET", authority = "b.example.com"))

        val onlyGets = storage.findRequests(ReplayFilter(method = "GET")).toList()
        assertEquals(2, onlyGets.size)

        val onlyA = storage.findRequests(ReplayFilter(authority = "a.example.com")).toList()
        assertEquals(2, onlyA.size)
    }

    @Test
    fun `config save find delete round-trip`() = runBlocking {
        val storage: StorageAdapter = InMemoryStorageAdapter()
        val config = sampleConfig()

        storage.saveConfig(config)
        assertEquals(config, storage.findConfig(config.id))

        storage.deleteConfig(config.id)
        assertNull(storage.findConfig(config.id))
    }

    @Test
    fun `job save update get round-trip`() = runBlocking {
        val storage: StorageAdapter = InMemoryStorageAdapter()
        val job = sampleJob()

        storage.saveJob(job)
        assertEquals(job, storage.getJob(job.id))

        val updated = job.copy(status = ReplayJobStatus.RUNNING, totalRequests = 10)
        storage.updateJob(updated)
        assertEquals(updated, storage.getJob(job.id))
    }

    @Test
    fun `saveLog persists and returns the log`() = runBlocking {
        val storage: StorageAdapter = InMemoryStorageAdapter()
        val log = ExecutionLog(
            id = UUID.randomUUID(),
            jobId = UUID.randomUUID(),
            requestId = UUID.randomUUID(),
            status = ExecutionStatus.SUCCESS,
            responseStatus = 200,
            responseTimeMs = 5,
            responseHeaders = emptyMap(),
            responseBody = null,
            executedAt = Instant.parse("2026-01-01T00:00:00Z")
        )

        val saved = storage.saveLog(log)
        assertEquals(log, saved)
    }

    // ---------------------------------------------------------------------------
    // MutationHandler contract
    // ---------------------------------------------------------------------------

    @Test
    fun `MutationHandler can be implemented with no framework dependencies`() {
        val handler: MutationHandler = UppercaseMethodHandler(order = 50)
        val original = sampleRequest(method = "get").toMutable()

        val mutated = handler.mutate(original, sampleConfig())

        assertEquals("GET", mutated.method)
        assertEquals(50, handler.order())
    }

    // ---------------------------------------------------------------------------
    // HttpExecutor contract
    // ---------------------------------------------------------------------------

    @Test
    fun `HttpExecutor can be implemented with no framework dependencies`() = runBlocking {
        val executor: HttpExecutor = StubHttpExecutor()
        val result = executor.execute(sampleRequest().toMutable())

        assertNotNull(result)
        assertEquals(ExecutionStatus.SUCCESS, result.status)
        assertEquals(200, result.responseStatus)
    }
}
