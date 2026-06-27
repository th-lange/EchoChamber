package com.echochamber.engine.domain.model

import java.time.Instant
import java.util.UUID

data class ReplayJob(
    val id: UUID,
    val configId: UUID,
    val status: ReplayJobStatus,
    val totalRequests: Int,
    val processedRequests: Int,
    val failedRequests: Int,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val triggeredBy: UUID? = null,
    val triggeredByUsername: String? = null,
)
