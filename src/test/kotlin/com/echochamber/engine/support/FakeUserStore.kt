package com.echochamber.engine.support

import com.echochamber.engine.domain.model.User
import com.echochamber.engine.domain.model.UserRole
import com.echochamber.engine.domain.port.PasswordHasher
import com.echochamber.engine.domain.port.UserStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** In-memory [UserStore] for tests. */
class FakeUserStore : UserStore {
    val store = ConcurrentHashMap<UUID, User>()

    override suspend fun findByUsername(username: String): User? = store.values.firstOrNull { it.username == username }
    override suspend fun findById(id: UUID): User? = store[id]
    override suspend fun findAll(): List<User> = store.values.toList()
    override suspend fun save(user: User): User {
        store[user.id] = user
        return user
    }
    override suspend fun countActiveAdmins(): Int = store.values.count { it.role == UserRole.ADMIN && it.enabled }
}

/** Trivial reversible "hasher" for tests — not real BCrypt. */
class FakePasswordHasher : PasswordHasher {
    override fun hash(raw: String): String = "hashed:$raw"
    override fun matches(raw: String, hash: String): Boolean = hash == "hashed:$raw"
}
