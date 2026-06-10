package com.echochamber.engine.adapter.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for the append-only `execution_logs` table.
 */
@Entity
@Table(name = "execution_logs")
class ExecutionLogEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "request_id", nullable = false)
    var requestId: UUID,

    @Column(name = "job_id", nullable = false)
    var jobId: UUID,

    @Column(name = "executed_at", nullable = false)
    var executedAt: Instant,

    @Column(name = "status", nullable = false, length = 20)
    var status: String,

    @Column(name = "response_status")
    var responseStatus: Int?,

    @Column(name = "response_time_ms", nullable = false)
    var responseTimeMs: Long,

    @Column(name = "response_headers", nullable = false, columnDefinition = "TEXT")
    var responseHeadersJson: String,

    @Column(name = "response_body", columnDefinition = "TEXT")
    var responseBody: String?
)
