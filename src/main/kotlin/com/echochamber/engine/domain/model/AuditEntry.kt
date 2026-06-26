package com.echochamber.engine.domain.model

import java.time.Instant
import java.util.UUID

/**
 * A single audit-trail record — who did what, when. Append-only. Pure domain model.
 */
data class AuditEntry(
    val id: UUID,
    val actorUserId: UUID?,
    val actorUsername: String,
    val action: AuditAction,
    val targetType: String?,
    val targetId: String?,
    val detail: String?,
    val occurredAt: Instant,
)
