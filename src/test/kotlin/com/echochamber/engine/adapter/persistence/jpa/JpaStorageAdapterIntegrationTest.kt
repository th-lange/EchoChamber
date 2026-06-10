package com.echochamber.engine.adapter.persistence.jpa

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import com.echochamber.engine.Application
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.util.UUID

/**
 * Integration test for [JpaStorageAdapter] against a real PostgreSQL container.
 *
 * Uses the Testcontainers JDBC URL configured in `src/test/resources/application.yml`
 * (`jdbc:tc:postgresql:15:///echochamber`) so Flyway runs the real V1 migration
 * before any test executes. `spring.jpa.hibernate.ddl-auto=validate` guarantees the
 * entity classes match the migration schema.
 *
 * **Running locally:** Testcontainers 1.19.x requires a Docker daemon that supports
 * API version 1.40 or newer. To run this test:
 * ```
 * ./gradlew test -Drun.integration=true
 * ```
 * In CI this property is always set; in local dev environments with an older Docker
 * server the test is skipped (matching the pre-existing handling of
 * `ApplicationContextTest`, which is also Testcontainers-driven).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = [Application::class])
@Import(JpaStorageAdapterIntegrationTest.TestConfig::class, JpaStorageAdapter::class)
@Tag("integration")
@EnabledIfSystemProperty(named = "run.integration", matches = "true")
class JpaStorageAdapterIntegrationTest @Autowired constructor(
    private val adapter: JpaStorageAdapter
) {

    @Configuration
    class TestConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper()
    }

    // ----- AC: appendRequest persists correctly -----

    @Test
    fun `appendRequest persists a captured request`() = runBlocking {
        val req = sampleRequest()

        val saved = adapter.appendRequest(req)

        assertEquals(req, saved)
        val fetched = adapter.findRequest(req.id)
        assertEquals(req, fetched)
    }

    // ----- AC: appendRequest called twice produces two rows -----

    @Test
    fun `appendRequest called twice produces two rows`() = runBlocking {
        val r1 = sampleRequest()
        val r2 = sampleRequest()

        adapter.appendRequest(r1)
        adapter.appendRequest(r2)

        val all = adapter.findRequests(ReplayFilter()).toList()
        val ids = all.map { it.id }.toSet()
        assertTrue(ids.contains(r1.id))
        assertTrue(ids.contains(r2.id))
    }

    // ----- findRequests applies a filter -----

    @Test
    fun `findRequests filters by method`() = runBlocking {
        val get = sampleRequest(method = "GET")
        val post = sampleRequest(method = "POST")
        adapter.appendRequest(get)
        adapter.appendRequest(post)

        val results = adapter.findRequests(ReplayFilter(method = "GET")).toList()

        assertTrue(results.any { it.id == get.id })
        assertFalse(results.any { it.id == post.id })
    }

    // ----- Config CRUD -----

    @Test
    fun `saveConfig then findConfig round-trips`() = runBlocking {
        val cfg = sampleConfig()

        adapter.saveConfig(cfg)
        val fetched = adapter.findConfig(cfg.id)

        assertEquals(cfg, fetched)
    }

    @Test
    fun `deleteConfig removes the row`() = runBlocking {
        val cfg = sampleConfig()
        adapter.saveConfig(cfg)

        adapter.deleteConfig(cfg.id)

        assertNull(adapter.findConfig(cfg.id))
    }

    // ----- Job round-trip + status update -----

    @Test
    fun `saveJob then updateJob persists progress`() = runBlocking {
        val cfg = sampleConfig()
        adapter.saveConfig(cfg)
        val job = ReplayJob(
            id = UUID.randomUUID(),
            configId = cfg.id,
            status = ReplayJobStatus.PENDING,
            totalRequests = 0,
            processedRequests = 0,
            failedRequests = 0,
            startedAt = null,
            completedAt = null
        )
        adapter.saveJob(job)

        val running = job.copy(
            status = ReplayJobStatus.RUNNING,
            totalRequests = 10,
            processedRequests = 3,
            startedAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        adapter.updateJob(running)

        val fetched = adapter.getJob(job.id)
        assertEquals(running, fetched)
    }

    // ----- ExecutionLog persistence -----

    @Test
    fun `saveLog persists an execution log`() = runBlocking {
        val cfg = sampleConfig()
        adapter.saveConfig(cfg)
        val req = sampleRequest()
        adapter.appendRequest(req)
        val job = ReplayJob(
            id = UUID.randomUUID(),
            configId = cfg.id,
            status = ReplayJobStatus.RUNNING,
            totalRequests = 1,
            processedRequests = 0,
            failedRequests = 0,
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            completedAt = null
        )
        adapter.saveJob(job)

        val log = ExecutionLog(
            id = UUID.randomUUID(),
            jobId = job.id,
            requestId = req.id,
            status = ExecutionStatus.SUCCESS,
            responseStatus = 200,
            responseTimeMs = 42L,
            responseHeaders = mapOf("content-type" to "application/json"),
            responseBody = """{"ok":true}""",
            executedAt = Instant.parse("2026-01-01T00:00:01Z")
        )

        val saved = adapter.saveLog(log)

        assertEquals(log, saved)
        assertNotNull(saved.id)
    }

    // ----- fixtures -----

    private fun sampleRequest(method: String = "GET") = CapturedRequest(
        id = UUID.randomUUID(),
        capturedAt = Instant.parse("2026-01-01T00:00:00Z"),
        method = method,
        uri = "https://example.com/api/resource",
        authority = "example.com",
        headers = mapOf("accept" to "application/json"),
        body = null
    )

    private fun sampleConfig() = ExecutionConfig(
        id = UUID.randomUUID(),
        name = "default",
        targetBaseUrl = "https://staging.example.com",
        mutationScript = null,
        mutationParameters = mapOf("userId" to "42"),
        maxConcurrency = 5,
        rateLimitPerSecond = 2.5,
        headerOverrides = mapOf("x-replay" to "true")
    )
}
