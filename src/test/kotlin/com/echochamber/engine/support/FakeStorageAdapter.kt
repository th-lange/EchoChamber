package com.echochamber.engine.support

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.port.StorageAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory [StorageAdapter] for unit tests — no database, no Docker. Reusable across
 * tickets (ingestion, replay service, etc.).
 */
class FakeStorageAdapter : StorageAdapter {

    val requests = CopyOnWriteArrayList<CapturedRequest>()
    val configs = ConcurrentHashMap<UUID, ExecutionConfig>()
    val jobs = ConcurrentHashMap<UUID, ReplayJob>()
    val logs = CopyOnWriteArrayList<ExecutionLog>()

    override suspend fun appendRequest(r: CapturedRequest): CapturedRequest {
        requests.add(r)
        return r
    }

    override suspend fun findRequest(id: UUID): CapturedRequest? = requests.firstOrNull { it.id == id }

    override suspend fun findRecentDuplicate(
        method: String,
        uri: String,
        authority: String,
        notBefore: Instant,
    ): CapturedRequest? = requests.firstOrNull {
        it.method == method && it.uri == uri && it.authority == authority && !it.capturedAt.isBefore(notBefore)
    }

    override fun findRequests(filter: ReplayFilter): Flow<CapturedRequest> = requests.asSequence()
        .filter { filter.method == null || it.method.equals(filter.method, ignoreCase = true) }
        .filter { filter.authority == null || it.authority == filter.authority }
        .toList()
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
        logs.add(l)
        return l
    }

    override suspend fun listJobs(limit: Int): List<ReplayJob> = jobs.values.toList().take(limit)

    override suspend fun listLogs(limit: Int): List<ExecutionLog> = logs.toList().take(limit)

    override suspend fun executionCountsByRequest(): Map<UUID, Long> =
        logs.groupingBy { it.requestId }.eachCount().mapValues { it.value.toLong() }
}
