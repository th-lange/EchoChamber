package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.port.StorageAdapter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

/**
 * Read-side queries backing the admin console (TICKET-020). Keeps the web layer off the
 * persistence ports directly — the console controller depends only on this service.
 */
@Service
class ConsoleService(
    private val storage: StorageAdapter,
) {
    /** Most recent captured requests (capped) for the "retriable requests" table. */
    suspend fun listRequests(limit: Int = 200): List<CapturedRequest> =
        storage.findRequests(ReplayFilter()).take(limit).toList().sortedByDescending { it.capturedAt }
}
