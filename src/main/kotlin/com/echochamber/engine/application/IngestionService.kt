package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.port.StorageAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Application service for ingesting captured requests from SnapReq.
 *
 * Responsibilities (Agent.md §4):
 * - Stamp `capturedAt` server-side on receipt (SnapReq does not send a timestamp).
 * - Generate the request id.
 * - Drop near-duplicate captures within a short idempotency window so retried sends from
 *   SnapReq don't create duplicate rows.
 *
 * Lives in `application/` and depends only on the [StorageAdapter] port — no web or
 * persistence imports. A no-op seam for drop-rule evaluation (TICKET-017) belongs here,
 * after this point but before persistence.
 */
@Service
class IngestionService(
    private val storage: StorageAdapter,
) {

    private val log = LoggerFactory.getLogger(IngestionService::class.java)

    /** Outcome of an ingest attempt. The endpoint returns 202 regardless of which. */
    enum class Outcome { STORED, DUPLICATE }

    suspend fun ingest(
        method: String,
        uri: String,
        authority: String,
        headers: Map<String, String>,
        body: String?,
    ): Outcome {
        val now = Instant.now()
        val duplicate = storage.findRecentDuplicate(method, uri, authority, now.minusMillis(IDEMPOTENCY_WINDOW_MS))
        if (duplicate != null) {
            log.debug("Dropping duplicate ingest within {}ms window: {} {}", IDEMPOTENCY_WINDOW_MS, method, uri)
            return Outcome.DUPLICATE
        }

        val request = CapturedRequest(
            id = UUID.randomUUID(),
            capturedAt = now,
            method = method,
            uri = uri,
            authority = authority,
            headers = headers,
            body = body,
        )
        storage.appendRequest(request)
        return Outcome.STORED
    }

    private companion object {
        const val IDEMPOTENCY_WINDOW_MS = 1_000L
    }
}
