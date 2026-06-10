package com.echochamber.engine.adapter.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for the mutable `execution_configs` table.
 *
 * Column `base_url_override` maps to the domain field `targetBaseUrl`; column
 * `rate_limit_per_second` is DOUBLE PRECISION to match the domain `Double` type.
 * Audit columns `created_at` / `updated_at` are stamped by the adapter on save.
 */
@Entity
@Table(name = "execution_configs")
class ExecutionConfigEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "base_url_override", nullable = false, columnDefinition = "TEXT")
    var baseUrlOverride: String,

    @Column(name = "header_overrides", nullable = false, columnDefinition = "TEXT")
    var headerOverridesJson: String,

    @Column(name = "max_concurrency", nullable = false)
    var maxConcurrency: Int,

    @Column(name = "rate_limit_per_second", nullable = false)
    var rateLimitPerSecond: Double,

    @Column(name = "mutation_parameters", nullable = false, columnDefinition = "TEXT")
    var mutationParametersJson: String,

    @Column(name = "mutation_script", columnDefinition = "TEXT")
    var mutationScript: String?,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)
