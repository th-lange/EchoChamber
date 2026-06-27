package com.echochamber.engine.web

import com.echochamber.engine.Application
import com.echochamber.engine.adapter.persistence.jpa.entity.CapturedRequestEntity
import com.echochamber.engine.adapter.persistence.jpa.repository.CapturedRequestJpaRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

/**
 * Integration test for the Spring Data REST surface (TICKET-007).
 *
 * Verifies the read-only vs. full-CRUD posture of each exported repository and that the
 * HAL Explorer is served. Runs against a real PostgreSQL container via the Testcontainers
 * JDBC URL in `src/test/resources/application.yml`; gated behind `-Drun.integration=true`
 * like the other Testcontainers-driven tests so local runs without Docker skip rather than
 * fail.
 */
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@EnabledIfSystemProperty(named = "run.integration", matches = "true")
@WithMockUser(roles = ["ADMIN"])
class SpringDataRestIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val capturedRequests: CapturedRequestJpaRepository,
) {

    // ----- executionConfigs: full CRUD -----

    @Test
    fun `GET executionConfigs returns a paginated HAL response`() {
        mockMvc.perform(get("/api/executionConfigs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$._embedded").exists())
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$.page").exists())
    }

    @Test
    fun `POST executionConfigs creates a record`() {
        mockMvc.perform(
            post("/api/executionConfigs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(configJson("created-via-rest")),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `DELETE executionConfig removes the record`() {
        val location = mockMvc.perform(
            post("/api/executionConfigs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(configJson("to-delete")),
        ).andExpect(status().isCreated).andReturn().response.getHeader("Location")!!

        mockMvc.perform(delete(location).with(csrf())).andExpect(status().isNoContent)
        mockMvc.perform(get(location)).andExpect(status().isNotFound)
    }

    // ----- capturedRequests: read-only -----

    @Test
    fun `DELETE capturedRequest is rejected with 405`() {
        val saved = capturedRequests.save(sampleCapturedEntity())

        mockMvc.perform(delete("/api/capturedRequests/${saved.id}").with(csrf()))
            .andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `PUT capturedRequest is rejected with 405`() {
        val saved = capturedRequests.save(sampleCapturedEntity())

        // A full, valid body so SDR gets past body deserialization to the method-not-allowed
        // check (the body alone proves nothing — `save` is not exported, so PUT must be 405).
        val body = """
            {
              "id": "${saved.id}",
              "capturedAt": "2026-01-01T00:00:00Z",
              "method": "PUT",
              "uri": "https://example.com/changed",
              "authority": "example.com",
              "headersJson": "{}",
              "body": null
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/capturedRequests/${saved.id}")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        ).andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `GET capturedRequests returns a paginated HAL response`() {
        capturedRequests.save(sampleCapturedEntity())

        mockMvc.perform(get("/api/capturedRequests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$._embedded.capturedRequests").exists())
            .andExpect(jsonPath("$.page").exists())
    }

    // ----- HAL Explorer -----

    @Test
    fun `HAL Explorer UI is served`() {
        // The explorer SPA is served as a static resource under the REST base path.
        mockMvc.perform(get("/api/explorer/index.html"))
            .andExpect(status().isOk)
    }

    // ----- fixtures -----

    private fun sampleCapturedEntity() = CapturedRequestEntity(
        id = UUID.randomUUID(),
        capturedAt = Instant.parse("2026-01-01T00:00:00Z"),
        method = "GET",
        uri = "https://example.com/api/resource",
        authority = "example.com",
        headersJson = """{"accept":"application/json"}""",
        body = null,
    )

    private fun configJson(name: String) = """
        {
          "id": "${UUID.randomUUID()}",
          "name": "$name",
          "baseUrlOverride": "https://staging.example.com",
          "headerOverridesJson": "{}",
          "maxConcurrency": 5,
          "rateLimitPerSecond": 2.5,
          "mutationParametersJson": "{}",
          "mutationScript": null,
          "createdAt": "2026-01-01T00:00:00Z",
          "updatedAt": "2026-01-01T00:00:00Z"
        }
    """.trimIndent()
}
