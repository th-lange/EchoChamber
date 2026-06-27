package com.echochamber.engine.application

import com.echochamber.engine.domain.model.User
import com.echochamber.engine.domain.model.UserRole
import com.echochamber.engine.support.FakePasswordHasher
import com.echochamber.engine.support.FakeUserStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserServiceTest {

    private val store = FakeUserStore()
    private val service = UserService(store, FakePasswordHasher())

    private fun admin(username: String, enabled: Boolean = true): User {
        val u = User(UUID.randomUUID(), username, "hashed:x", UserRole.ADMIN, enabled, false, Instant.now(), Instant.now(), null)
        store.store[u.id] = u
        return u
    }

    @Test
    fun `create stores a hashed password and forces a change`() = runBlocking {
        val user = service.create("alice", "s3cret", UserRole.OPERATOR, null)
        assertEquals("hashed:s3cret", user.passwordHash)
        assertTrue(user.mustChangePassword)
        assertEquals(UserRole.OPERATOR, user.role)
    }

    @Test
    fun `create rejects duplicate username`() = runBlocking {
        service.create("bob", "p", UserRole.VIEWER, null)
        assertThrows<IllegalArgumentException> { runBlocking { service.create("bob", "p2", UserRole.VIEWER, null) } }
        Unit
    }

    @Test
    fun `disabling the last active admin is rejected`() = runBlocking {
        val a = admin("root")
        assertThrows<LastActiveAdminException> { runBlocking { service.setEnabled(a.id, false) } }
        Unit
    }

    @Test
    fun `disabling an admin is allowed when another active admin remains`() = runBlocking {
        val a = admin("root")
        admin("root2")
        val updated = service.setEnabled(a.id, false)
        assertFalse(updated.enabled)
    }

    @Test
    fun `demoting the last active admin is rejected`() = runBlocking {
        val a = admin("root")
        assertThrows<LastActiveAdminException> { runBlocking { service.setRole(a.id, UserRole.VIEWER) } }
        Unit
    }

    @Test
    fun `changeOwnPassword clears the must-change flag`() = runBlocking {
        val a = User(UUID.randomUUID(), "carol", "hashed:old", UserRole.VIEWER, true, true, Instant.now(), Instant.now(), null)
        store.store[a.id] = a
        val updated = service.changeOwnPassword("carol", "new")
        assertEquals("hashed:new", updated.passwordHash)
        assertFalse(updated.mustChangePassword)
    }
}
