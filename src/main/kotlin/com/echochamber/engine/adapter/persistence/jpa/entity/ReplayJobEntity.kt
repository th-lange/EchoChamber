package com.echochamber.engine.adapter.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for the `replay_jobs` table.
 *
 * Status is stored as a VARCHAR string (the domain enum is mapped manually in the
 * adapter, rather than using `@Enumerated`, to keep the entity free of domain enum
 * imports — adapter ↔ entity mapping owns the conversion).
 */
@Entity
@Table(name = "replay_jobs")
class ReplayJobEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "config_id", nullable = false)
    var configId: UUID,

    @Column(name = "status", nullable = false, length = 20)
    var status: String,

    @Column(name = "total_requests", nullable = false)
    var totalRequests: Int,

    @Column(name = "processed_requests", nullable = false)
    var processedRequests: Int,

    @Column(name = "failed_requests", nullable = false)
    var failedRequests: Int,

    @Column(name = "started_at")
    var startedAt: Instant?,

    @Column(name = "completed_at")
    var completedAt: Instant?,

    @Column(name = "triggered_by")
    var triggeredBy: UUID? = null,

    @Column(name = "triggered_by_username", length = 255)
    var triggeredByUsername: String? = null,
)
