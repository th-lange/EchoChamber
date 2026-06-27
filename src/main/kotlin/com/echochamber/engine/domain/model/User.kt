package com.echochamber.engine.domain.model

import java.time.Instant
import java.util.UUID

/**
 * An admin console account. Pure domain model — `passwordHash` is a BCrypt hash, never a
 * plaintext password, and is never exposed in a web DTO.
 */
data class User(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val enabled: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: UUID?,
)
