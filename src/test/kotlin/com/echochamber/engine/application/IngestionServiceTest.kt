package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.support.FakeStorageAdapter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class IngestionServiceTest {

    private val storage = FakeStorageAdapter()
    private val service = IngestionService(storage)

    @Test
    fun `valid request is stored with server-stamped capturedAt and generated id`() = runBlocking {
        val before = Instant.now()

        val outcome = service.ingest(
            method = "GET",
            uri = "https://example.com/api/resource",
            authority = "example.com",
            headers = mapOf("accept" to "application/json"),
            body = null,
        )

        assertEquals(IngestionService.Outcome.STORED, outcome)
        assertEquals(1, storage.requests.size)
        val stored = storage.requests.first()
        assertNotNull(stored.id)
        assertEquals("GET", stored.method)
        assertEquals(null, stored.body)
        assertTrue(!stored.capturedAt.isBefore(before), "capturedAt should be stamped on receipt")
    }

    @Test
    fun `near-duplicate within the idempotency window is dropped`() = runBlocking {
        storage.requests.add(
            CapturedRequest(
                id = UUID.randomUUID(),
                capturedAt = Instant.now(),
                method = "POST",
                uri = "https://example.com/api/orders",
                authority = "example.com",
                headers = emptyMap(),
                body = """{"x":1}""",
            ),
        )

        val outcome = service.ingest(
            method = "POST",
            uri = "https://example.com/api/orders",
            authority = "example.com",
            headers = emptyMap(),
            body = """{"x":1}""",
        )

        assertEquals(IngestionService.Outcome.DUPLICATE, outcome)
        assertEquals(1, storage.requests.size)
    }

    @Test
    fun `an older same-tuple request outside the window does not block storage`() = runBlocking {
        storage.requests.add(
            CapturedRequest(
                id = UUID.randomUUID(),
                capturedAt = Instant.now().minus(5, ChronoUnit.SECONDS),
                method = "GET",
                uri = "https://example.com/api/resource",
                authority = "example.com",
                headers = emptyMap(),
                body = null,
            ),
        )

        val outcome = service.ingest(
            method = "GET",
            uri = "https://example.com/api/resource",
            authority = "example.com",
            headers = emptyMap(),
            body = null,
        )

        assertEquals(IngestionService.Outcome.STORED, outcome)
        assertEquals(2, storage.requests.size)
    }
}
