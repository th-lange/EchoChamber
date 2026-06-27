package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.User
import java.util.UUID

/**
 * Persistence port for admin console [User] accounts. Implemented in `adapter/`.
 */
interface UserStore {
    suspend fun findByUsername(username: String): User?
    suspend fun findById(id: UUID): User?
    suspend fun findAll(): List<User>
    suspend fun save(user: User): User
    /** Number of accounts that are both ADMIN and enabled — used to guard the last admin. */
    suspend fun countActiveAdmins(): Int
}
