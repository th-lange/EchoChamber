package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.port.StorageAdapter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Read-side queries + console helpers backing the admin console (TICKET-020). Keeps the web
 * layer off the persistence ports directly — the console controller depends only on this service.
 */
@Service
class ConsoleService(
    private val storage: StorageAdapter,
) {
    /** Most recent captured requests (capped) for the "retriable requests" table. */
    suspend fun listRequests(limit: Int = 200): List<CapturedRequest> =
        storage.findRequests(ReplayFilter()).take(limit).toList().sortedByDescending { it.capturedAt }

    /**
     * Returns the id of the console's default execution config, creating it on first use.
     * Operators reexecute from the console without choosing a config — per-retry changes ride
     * on the inline override, so this default is intentionally a no-op (empty base URL/overrides,
     * unlimited rate). The id is fixed so repeated calls reuse the same row.
     */
    suspend fun ensureDefaultConfigId(): UUID {
        if (storage.findConfig(DEFAULT_CONFIG_ID) == null) {
            storage.saveConfig(
                ExecutionConfig(
                    id = DEFAULT_CONFIG_ID,
                    name = "console-default",
                    targetBaseUrl = "",
                    mutationScript = null,
                    mutationParameters = emptyMap(),
                    maxConcurrency = 5,
                    rateLimitPerSecond = 0.0,
                    headerOverrides = emptyMap(),
                ),
            )
        }
        return DEFAULT_CONFIG_ID
    }

    private companion object {
        val DEFAULT_CONFIG_ID: UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de")
    }
}
