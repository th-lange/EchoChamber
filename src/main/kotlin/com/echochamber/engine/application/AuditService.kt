package com.echochamber.engine.application

import com.echochamber.engine.domain.model.AuditAction
import com.echochamber.engine.domain.model.AuditEntry
import com.echochamber.engine.domain.port.AuditStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Writes audit-trail entries (TICKET-021). **Best-effort**: a failure to persist an audit
 * entry is logged but never propagated, so auditing can never abort the primary action.
 */
@Service
class AuditService(
    private val auditStore: AuditStore,
) {

    private val log = LoggerFactory.getLogger(AuditService::class.java)

    suspend fun record(
        action: AuditAction,
        actorUsername: String,
        actorUserId: UUID? = null,
        targetType: String? = null,
        targetId: String? = null,
        detail: String? = null,
    ) {
        try {
            auditStore.append(
                AuditEntry(
                    id = UUID.randomUUID(),
                    actorUserId = actorUserId,
                    actorUsername = actorUsername,
                    action = action,
                    targetType = targetType,
                    targetId = targetId,
                    detail = detail,
                    occurredAt = Instant.now(),
                ),
            )
        } catch (e: Exception) {
            log.warn("Audit write failed for action {} by {}: {}", action, actorUsername, e.toString())
        }
    }
}
