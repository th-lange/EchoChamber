package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import kotlinx.coroutines.flow.Flow
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
}
