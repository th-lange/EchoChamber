package com.echochamber.engine.adapter.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** JPA entity for the append-only `audit_log` table. */
@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "actor_user_id")
    var actorUserId: UUID?,

    @Column(name = "actor_username", nullable = false, length = 255)
    var actorUsername: String,

    @Column(name = "action", nullable = false, length = 40)
    var action: String,

    @Column(name = "target_type", length = 60)
    var targetType: String?,

    @Column(name = "target_id", length = 255)
    var targetId: String?,

    @Column(name = "detail", columnDefinition = "TEXT")
    var detail: String?,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant,
)
