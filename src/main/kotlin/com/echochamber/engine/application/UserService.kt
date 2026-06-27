package com.echochamber.engine.application

import com.echochamber.engine.domain.model.User
import com.echochamber.engine.domain.model.UserRole
import com.echochamber.engine.domain.port.PasswordHasher
import com.echochamber.engine.domain.port.UserStore
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/** Thrown when an operation would leave the system with no active ADMIN. */
class LastActiveAdminException : IllegalStateException("Cannot disable, delete, or demote the last active ADMIN")

/**
 * User management (TICKET-019). Hashes passwords via the [PasswordHasher] port (no Spring
 * Security import here), forces a password change on accounts created/reset by an admin, and
 * guards against removing the last active ADMIN.
 */
@Service
class UserService(
    private val users: UserStore,
    private val hasher: PasswordHasher,
) {

    suspend fun listUsers(): List<User> = users.findAll().sortedBy { it.username }

    /** Whether the named user must change their password before using the console. */
    suspend fun requiresPasswordChange(username: String): Boolean =
        users.findByUsername(username)?.mustChangePassword ?: false

    suspend fun create(username: String, rawPassword: String, role: UserRole, createdBy: UUID?): User {
        require(users.findByUsername(username) == null) { "Username '$username' already exists" }
        val now = Instant.now()
        return users.save(
            User(
                id = UUID.randomUUID(),
                username = username,
                passwordHash = hasher.hash(rawPassword),
                role = role,
                enabled = true,
                mustChangePassword = true,
                createdAt = now,
                updatedAt = now,
                createdBy = createdBy,
            ),
        )
    }

    suspend fun setEnabled(id: UUID, enabled: Boolean): User {
        val user = users.findById(id) ?: throw NoSuchElementException("User $id not found")
        if (!enabled && user.enabled && user.role == UserRole.ADMIN) guardLastAdmin()
        return users.save(user.copy(enabled = enabled, updatedAt = Instant.now()))
    }

    suspend fun setRole(id: UUID, role: UserRole): User {
        val user = users.findById(id) ?: throw NoSuchElementException("User $id not found")
        if (user.enabled && user.role == UserRole.ADMIN && role != UserRole.ADMIN) guardLastAdmin()
        return users.save(user.copy(role = role, updatedAt = Instant.now()))
    }

    suspend fun resetPassword(id: UUID, rawPassword: String): User {
        val user = users.findById(id) ?: throw NoSuchElementException("User $id not found")
        return users.save(user.copy(passwordHash = hasher.hash(rawPassword), mustChangePassword = true, updatedAt = Instant.now()))
    }

    suspend fun changeOwnPassword(username: String, newRawPassword: String): User {
        val user = users.findByUsername(username) ?: throw NoSuchElementException("User '$username' not found")
        return users.save(user.copy(passwordHash = hasher.hash(newRawPassword), mustChangePassword = false, updatedAt = Instant.now()))
    }

    private suspend fun guardLastAdmin() {
        if (users.countActiveAdmins() <= 1) throw LastActiveAdminException()
    }
}
