package com.echochamber.engine.adapter.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** JPA entity for the `users` table (admin console accounts). */
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "username", nullable = false, length = 255, unique = true)
    var username: String,

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    var passwordHash: String,

    @Column(name = "role", nullable = false, length = 20)
    var role: String,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean,

    @Column(name = "must_change_password", nullable = false)
    var mustChangePassword: Boolean,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "created_by")
    var createdBy: UUID?,
)
