package com.echochamber.engine.domain.model

import java.util.UUID

data class ExecutionConfig(
    val id: UUID,
    val name: String,
    val targetBaseUrl: String,
    val mutationScript: String?,
    val mutationParameters: Map<String, String>,
    val maxConcurrency: Int,
    val rateLimitPerSecond: Double,
    val headerOverrides: Map<String, String>
)
