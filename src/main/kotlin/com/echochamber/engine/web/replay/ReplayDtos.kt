package com.echochamber.engine.web.replay

import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.RequestOverride
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

/** Inline modify-before-retry override (maps to domain [RequestOverride]). */
data class RequestOverrideDto(
    val targetUrl: String? = null,
    val pathOverride: String? = null,
    val headersSet: Map<String, String> = emptyMap(),
    val headersRemove: List<String> = emptyList(),
    val bodyPatches: Map<String, String> = emptyMap(),
) {
    fun toDomain() = RequestOverride(targetUrl, pathOverride, headersSet, headersRemove, bodyPatches)
}

data class ReplayFilterDto(
    val method: String? = null,
    val uriPattern: String? = null,
    val authority: String? = null,
    val capturedAfter: Instant? = null,
    val capturedBefore: Instant? = null,
) {
    fun toDomain() = ReplayFilter(method, uriPattern, authority, capturedAfter, capturedBefore)
}

data class ReplayTriggerDto(
    @field:NotNull val configId: UUID?,
    val requestIds: List<UUID>? = null,
    val filter: ReplayFilterDto? = null,
    val override: RequestOverrideDto? = null,
)

data class ReplayJobDto(
    val id: UUID,
    val configId: UUID,
    val status: String,
    val totalRequests: Int,
    val processedRequests: Int,
    val failedRequests: Int,
    val triggeredByUsername: String?,
)

fun ReplayJob.toDto() = ReplayJobDto(
    id = id,
    configId = configId,
    status = status.name,
    totalRequests = totalRequests,
    processedRequests = processedRequests,
    failedRequests = failedRequests,
    triggeredByUsername = triggeredByUsername,
)
