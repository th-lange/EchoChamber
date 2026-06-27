package com.echochamber.engine.web.ingestion

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Ingest payload — the shared contract between SnapReq and EchoChamber (Agent.md §4).
 *
 * Note: there is intentionally **no `capturedAt`** field; EchoChamber stamps it on receipt.
 * `body` is `null` (present, not omitted) when there is no body. `headers` is a flat,
 * single-value-per-name map.
 */
data class IngestRequestDto(
    @field:NotBlank val method: String?,
    @field:NotBlank val uri: String?,
    @field:NotBlank val authority: String?,
    @field:NotNull val headers: Map<String, String>?,
    val body: String?,
)
