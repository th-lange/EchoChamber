package com.echochamber.engine.web.ingestion

import com.echochamber.engine.application.IngestionService
import com.echochamber.engine.support.FakeStorageAdapter
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.beans.factory.annotation.Autowired

/**
 * Web-slice test for the ingest endpoint. No database/Docker required — the service is
 * backed by an in-memory [FakeStorageAdapter]. A valid token is always sent so the test
 * passes whether or not `InternalAuthFilter` is applied in this slice (the filter's own
 * behaviour is covered by `InternalAuthFilterTest`).
 */
@WebMvcTest(controllers = [IngestController::class])
@Import(IngestControllerTest.TestConfig::class)
@TestPropertySource(properties = ["INTERNAL_INGEST_TOKEN=test-token"])
class IngestControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    class TestConfig {
        @Bean
        fun ingestionService(): IngestionService = IngestionService(FakeStorageAdapter())
    }

    @Test
    fun `valid payload returns 202`() {
        mockMvc.perform(
            post("/internal/ingest")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"method":"GET","uri":"https://example.com/api/x","authority":"example.com",
                     "headers":{"accept":"application/json"},"body":null}
                    """.trimIndent(),
                ),
        ).andExpect(status().isAccepted)
    }

    @Test
    fun `missing required field returns 400`() {
        mockMvc.perform(
            post("/internal/ingest")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"uri":"https://example.com/api/x","authority":"example.com","headers":{},"body":null}""",
                ),
        ).andExpect(status().isBadRequest)
    }
}
