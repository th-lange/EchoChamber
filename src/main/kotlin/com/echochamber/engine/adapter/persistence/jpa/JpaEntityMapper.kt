package com.echochamber.engine.adapter.persistence.jpa

import com.echochamber.engine.adapter.persistence.jpa.entity.CapturedRequestEntity
import com.echochamber.engine.adapter.persistence.jpa.entity.ExecutionConfigEntity
import com.echochamber.engine.adapter.persistence.jpa.entity.ExecutionLogEntity
import com.echochamber.engine.adapter.persistence.jpa.entity.ReplayJobEntity
import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Pure conversion between JPA entities and domain models.
 *
 * Centralising the mapping keeps the entity classes free of domain imports and the
 * domain models free of JPA imports. JSON-shaped columns (header maps, parameter maps,
 * response headers) are serialised/deserialised here via Jackson.
 */
internal class JpaEntityMapper(private val objectMapper: ObjectMapper) {

    private val stringMapType = object : TypeReference<Map<String, String>>() {}

    // ---------- CapturedRequest ----------

    fun toEntity(domain: CapturedRequest): CapturedRequestEntity = CapturedRequestEntity(
        id = domain.id,
        capturedAt = domain.capturedAt,
        method = domain.method,
        uri = domain.uri,
        authority = domain.authority,
        headersJson = objectMapper.writeValueAsString(domain.headers),
        body = domain.body
    )

    fun toDomain(entity: CapturedRequestEntity): CapturedRequest = CapturedRequest(
        id = entity.id,
        capturedAt = entity.capturedAt,
        method = entity.method,
        uri = entity.uri,
        authority = entity.authority,
        headers = readMap(entity.headersJson),
        body = entity.body
    )

    // ---------- ExecutionConfig ----------

    fun toEntity(domain: ExecutionConfig, existing: ExecutionConfigEntity?, now: java.time.Instant): ExecutionConfigEntity {
        val createdAt = existing?.createdAt ?: now
        return ExecutionConfigEntity(
            id = domain.id,
            name = domain.name,
            baseUrlOverride = domain.targetBaseUrl,
            headerOverridesJson = objectMapper.writeValueAsString(domain.headerOverrides),
            maxConcurrency = domain.maxConcurrency,
            rateLimitPerSecond = domain.rateLimitPerSecond,
            mutationParametersJson = objectMapper.writeValueAsString(domain.mutationParameters),
            mutationScript = domain.mutationScript,
            createdAt = createdAt,
            updatedAt = now
        )
    }

    fun toDomain(entity: ExecutionConfigEntity): ExecutionConfig = ExecutionConfig(
        id = entity.id,
        name = entity.name,
        targetBaseUrl = entity.baseUrlOverride,
        mutationScript = entity.mutationScript,
        mutationParameters = readMap(entity.mutationParametersJson),
        maxConcurrency = entity.maxConcurrency,
        rateLimitPerSecond = entity.rateLimitPerSecond,
        headerOverrides = readMap(entity.headerOverridesJson)
    )

    // ---------- ReplayJob ----------

    fun toEntity(domain: ReplayJob): ReplayJobEntity = ReplayJobEntity(
        id = domain.id,
        configId = domain.configId,
        status = domain.status.name,
        totalRequests = domain.totalRequests,
        processedRequests = domain.processedRequests,
        failedRequests = domain.failedRequests,
        startedAt = domain.startedAt,
        completedAt = domain.completedAt,
        triggeredBy = domain.triggeredBy,
        triggeredByUsername = domain.triggeredByUsername
    )

    fun toDomain(entity: ReplayJobEntity): ReplayJob = ReplayJob(
        id = entity.id,
        configId = entity.configId,
        status = ReplayJobStatus.valueOf(entity.status),
        totalRequests = entity.totalRequests,
        processedRequests = entity.processedRequests,
        failedRequests = entity.failedRequests,
        startedAt = entity.startedAt,
        completedAt = entity.completedAt,
        triggeredBy = entity.triggeredBy,
        triggeredByUsername = entity.triggeredByUsername
    )

    // ---------- ExecutionLog ----------

    fun toEntity(domain: ExecutionLog): ExecutionLogEntity = ExecutionLogEntity(
        id = domain.id,
        requestId = domain.requestId,
        jobId = domain.jobId,
        executedAt = domain.executedAt,
        status = domain.status.name,
        responseStatus = domain.responseStatus,
        responseTimeMs = domain.responseTimeMs,
        responseHeadersJson = objectMapper.writeValueAsString(domain.responseHeaders),
        responseBody = domain.responseBody
    )

    fun toDomain(entity: ExecutionLogEntity): ExecutionLog = ExecutionLog(
        id = entity.id,
        jobId = entity.jobId,
        requestId = entity.requestId,
        status = ExecutionStatus.valueOf(entity.status),
        responseStatus = entity.responseStatus,
        responseTimeMs = entity.responseTimeMs,
        responseHeaders = readMap(entity.responseHeadersJson),
        responseBody = entity.responseBody,
        executedAt = entity.executedAt
    )

    private fun readMap(json: String): Map<String, String> =
        if (json.isBlank()) emptyMap() else objectMapper.readValue(json, stringMapType)
}
