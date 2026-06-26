package com.echochamber.engine.web.user

import com.echochamber.engine.domain.model.User
import com.echochamber.engine.domain.model.UserRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

/** Read DTO — never carries the password hash. */
data class UserDto(
    val id: UUID,
    val username: String,
    val role: UserRole,
    val enabled: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: Instant,
)

data class CreateUserDto(
    @field:NotBlank val username: String?,
    @field:NotBlank val password: String?,
    @field:NotNull val role: UserRole?,
)

data class RoleDto(@field:NotNull val role: UserRole?)

data class PasswordDto(@field:NotBlank val password: String?)

fun User.toDto() = UserDto(
    id = id,
    username = username,
    role = role,
    enabled = enabled,
    mustChangePassword = mustChangePassword,
    createdAt = createdAt,
)
