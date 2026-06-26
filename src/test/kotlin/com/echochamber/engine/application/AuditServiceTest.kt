package com.echochamber.engine.application

import com.echochamber.engine.domain.model.AuditAction
import com.echochamber.engine.domain.model.AuditEntry
import com.echochamber.engine.domain.port.AuditStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class AuditServiceTest {

    @Test
    fun `record appends an entry with actor, action and target`() = runBlocking {
        val entries = CopyOnWriteArrayList<AuditEntry>()
        val store = object : AuditStore {
            override suspend fun append(entry: AuditEntry): AuditEntry = entry.also { entries.add(it) }
        }
        val service = AuditService(store)

        service.record(AuditAction.RETRY_TRIGGERED, actorUsername = "alice", targetType = "ReplayJob", targetId = "job-1", detail = "override=path")

        assertEquals(1, entries.size)
        val e = entries.first()
        assertEquals(AuditAction.RETRY_TRIGGERED, e.action)
        assertEquals("alice", e.actorUsername)
        assertEquals("ReplayJob", e.targetType)
        assertEquals("job-1", e.targetId)
    }

    @Test
    fun `a failing audit store never aborts the caller`() = runBlocking {
        val store = object : AuditStore {
            override suspend fun append(entry: AuditEntry): AuditEntry = throw RuntimeException("db down")
        }
        val service = AuditService(store)

        assertDoesNotThrow { runBlocking { service.record(AuditAction.LOGIN, actorUsername = "bob") } }
        Unit
    }
}
