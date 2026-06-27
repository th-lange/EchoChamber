package com.echochamber.engine.web.replay

import com.echochamber.engine.application.AuditService
import com.echochamber.engine.application.ReplayJobScheduler
import com.echochamber.engine.application.ReplaySelection
import com.echochamber.engine.domain.model.AuditAction
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.port.StorageAdapter
import com.echochamber.engine.domain.port.UserStore
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.Instant
import java.util.UUID

/**
 * Action endpoints for replay (TICKET-014). RBAC (OPERATOR/ADMIN) is enforced in
 * `SecurityConfig` for these paths. Trigger carries the optional inline override
 * (modify-before-retry) and records the triggering user for attribution + audit.
 */
@RestController
@RequestMapping("/api/replayJobs")
class ReplayController(
    private val scheduler: ReplayJobScheduler,
    private val storage: StorageAdapter,
    private val users: UserStore,
    private val audit: AuditService,
) {

    @PostMapping("/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun trigger(@Valid @RequestBody dto: ReplayTriggerDto, principal: Principal?): ReplayJobDto {
        val hasIds = !dto.requestIds.isNullOrEmpty()
        val hasFilter = dto.filter != null
        require(hasIds != hasFilter) { "Provide exactly one of 'requestIds' or 'filter'" }

        val actorName = principal?.name ?: "unknown"
        val actorId = users.findByUsername(actorName)?.id

        val job = scheduler.scheduleJob(
            configId = dto.configId!!,
            selection = ReplaySelection(requestIds = dto.requestIds, filter = dto.filter?.toDomain()),
            override = dto.override?.toDomain(),
            triggeredBy = actorId,
            triggeredByUsername = actorName,
        )

        audit.record(
            action = AuditAction.RETRY_TRIGGERED,
            actorUsername = actorName,
            actorUserId = actorId,
            targetType = "ReplayJob",
            targetId = job.id.toString(),
            detail = overrideSummary(dto),
        )
        return job.toDto()
    }

    @PostMapping("/{id}/cancel")
    suspend fun cancel(@PathVariable id: UUID, principal: Principal?): ResponseEntity<ReplayJobDto> {
        val job = storage.getJob(id) ?: return ResponseEntity.notFound().build()
        if (job.status != ReplayJobStatus.PENDING && job.status != ReplayJobStatus.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        scheduler.cancelJob(id)
        val cancelled = storage.updateJob(job.copy(status = ReplayJobStatus.FAILED, completedAt = Instant.now()))
        audit.record(
            action = AuditAction.RETRY_CANCELLED,
            actorUsername = principal?.name ?: "unknown",
            targetType = "ReplayJob",
            targetId = id.toString(),
        )
        return ResponseEntity.ok(cancelled.toDto())
    }

    private fun overrideSummary(dto: ReplayTriggerDto): String? {
        val o = dto.override ?: return null
        return buildList {
            o.targetUrl?.let { add("targetUrl=$it") }
            o.pathOverride?.let { add("path=$it") }
            if (o.headersSet.isNotEmpty()) add("headersSet=${o.headersSet.keys}")
            if (o.headersRemove.isNotEmpty()) add("headersRemove=${o.headersRemove}")
            if (o.bodyPatches.isNotEmpty()) add("bodyPatches=${o.bodyPatches.keys}")
        }.joinToString(", ").ifEmpty { null }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: IllegalArgumentException): Map<String, String?> = mapOf("error" to e.message)
}
