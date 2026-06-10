package com.echochamber.engine.domain.model

import java.time.Instant
import java.util.UUID

data class ExecutionLog(
    val id: UUID,
    val jobId: UUID,
    val requestId: UUID,
    val status: ExecutionStatus,
    val responseStatus: Int?,
    val responseTimeMs: Long,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val executedAt: Instant
)
