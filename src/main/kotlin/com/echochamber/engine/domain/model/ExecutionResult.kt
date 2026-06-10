package com.echochamber.engine.domain.model

/**
 * Value object describing the outcome of a single HTTP execution performed by an [com.echochamber.engine.domain.port.HttpExecutor].
 *
 * Pure data — no framework dependencies.
 */
data class ExecutionResult(
    val status: ExecutionStatus,
    val responseStatus: Int?,
    val responseTimeMs: Long,
    val responseHeaders: Map<String, String>,
    val responseBody: String?
)
