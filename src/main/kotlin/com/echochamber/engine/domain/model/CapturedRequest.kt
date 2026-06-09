package com.echochamber.engine.domain.model

import java.time.Instant
import java.util.UUID

data class CapturedRequest(
    val id: UUID,
    val capturedAt: Instant,
    val method: String,
    val uri: String,
    val authority: String,
    val headers: Map<String, String>,
    val body: String?
)
