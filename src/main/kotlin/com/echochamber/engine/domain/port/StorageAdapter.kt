package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/**
 * Persistence port for the engine.
 *
 * Adapters implementing this interface (JPA, R2DBC, in-memory, etc.) are the only places
 * persistence concerns are allowed. The contract is intentionally append-only for
 * [CapturedRequest]: there is no `updateRequest` or `deleteRequest`. Captured requests
 * are immutable once stored.
 *
 * The only coroutines import permitted in `domain/` is [Flow], used here to stream
 * filtered captured requests without loading them all into memory.
 */
interface StorageAdapter {

    // --- Captured requests (append-only) ---

    suspend fun appendRequest(r: CapturedRequest): CapturedRequest

    suspend fun findRequest(id: UUID): CapturedRequest?

    /**
     * Idempotency support for ingest: returns a previously stored request matching the
     * `(method, uri, authority)` tuple whose `capturedAt` is at or after [notBefore], or
     * `null` if none. Used to drop near-duplicate captures within a short window. This is a
     * read-only query and does not violate the append-only contract.
     */
    suspend fun findRecentDuplicate(
        method: String,
        uri: String,
        authority: String,
        notBefore: Instant,
    ): CapturedRequest?

    fun findRequests(filter: ReplayFilter): Flow<CapturedRequest>

    // --- Execution configs (mutable) ---

    suspend fun saveConfig(c: ExecutionConfig): ExecutionConfig

    suspend fun findConfig(id: UUID): ExecutionConfig?

    suspend fun deleteConfig(id: UUID)

    // --- Replay jobs ---

    suspend fun saveJob(j: ReplayJob): ReplayJob

    suspend fun updateJob(j: ReplayJob): ReplayJob

    suspend fun getJob(id: UUID): ReplayJob?

    // --- Execution logs (append-only) ---

    suspend fun saveLog(l: ExecutionLog): ExecutionLog

    // --- Console read queries ---

    /** Most recent replay jobs (capped), for the retry-history view. */
    suspend fun listJobs(limit: Int): List<ReplayJob>

    /** Most recent execution logs (capped), for the retry-history view. */
    suspend fun listLogs(limit: Int): List<ExecutionLog>

    /** Number of executions recorded per captured request id (i.e. how many times retried). */
    suspend fun executionCountsByRequest(): Map<UUID, Long>
}
