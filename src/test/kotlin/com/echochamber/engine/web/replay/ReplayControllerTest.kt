package com.echochamber.engine.web.replay

import com.echochamber.engine.application.AuditService
import com.echochamber.engine.application.MutationEngine
import com.echochamber.engine.application.ReplayJobScheduler
import com.echochamber.engine.application.ReplayService
import com.echochamber.engine.domain.model.AuditEntry
import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.port.AuditStore
import com.echochamber.engine.domain.port.HttpExecutor
import com.echochamber.engine.domain.port.StorageAdapter
import com.echochamber.engine.domain.port.UserStore
import com.echochamber.engine.support.FakeStorageAdapter
import com.echochamber.engine.support.FakeUserStore
import com.echochamber.engine.web.console.ConsoleWebConfig
import com.echochamber.engine.web.console.ForcedPasswordChangeInterceptor
import com.echochamber.engine.web.security.SecurityConfig
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.UUID

@WebMvcTest(
    controllers = [ReplayController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [ConsoleWebConfig::class, ForcedPasswordChangeInterceptor::class])],
)
@Import(ReplayControllerTest.TestConfig::class, SecurityConfig::class)
class ReplayControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val storage: StorageAdapter,
) {

    class TestConfig {
        val storage = FakeStorageAdapter()

        @Bean fun storage(): StorageAdapter = storage
        @Bean fun users(): UserStore = FakeUserStore()
        @Bean fun audit(): AuditService = AuditService(object : AuditStore {
            override suspend fun append(entry: AuditEntry) = entry
        })

        @Bean
        fun scheduler(): ReplayJobScheduler {
            val exec = object : HttpExecutor {
                override suspend fun execute(request: MutableRequest) =
                    ExecutionResult(ExecutionStatus.SUCCESS, 200, 1, emptyMap(), null)
            }
            return ReplayJobScheduler(storage, ReplayService(storage, MutationEngine(emptyList()), exec))
        }
    }

    private fun seedJob(status: ReplayJobStatus): UUID {
        val job = ReplayJob(UUID.randomUUID(), UUID.randomUUID(), status, 0, 0, 0, null, null)
        (storage as FakeStorageAdapter).jobs[job.id] = job
        return job.id
    }

    private fun triggerBody(both: Boolean = false, neither: Boolean = false): String {
        val cfg = UUID.randomUUID()
        return when {
            neither -> """{"configId":"$cfg"}"""
            both -> """{"configId":"$cfg","requestIds":["${UUID.randomUUID()}"],"filter":{"method":"GET"}}"""
            else -> """{"configId":"$cfg","requestIds":["${UUID.randomUUID()}"]}"""
        }
    }

    /** Performs the request, dispatching the async result when the suspend handler started one. */
    private fun statusOf(builder: RequestBuilder): Int {
        val result = mockMvc.perform(builder).andReturn()
        val finalResult = if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)).andReturn() else result
        return finalResult.response.status
    }

    private fun triggerPost(body: String) =
        post("/api/replayJobs/trigger").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)

    @Test
    @WithMockUser(roles = ["VIEWER"])
    fun `viewer cannot trigger`() {
        assertEquals(403, statusOf(triggerPost(triggerBody())))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `operator can trigger with requestIds`() {
        assertEquals(202, statusOf(triggerPost(triggerBody())))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `trigger with both requestIds and filter is 400`() {
        assertEquals(400, statusOf(triggerPost(triggerBody(both = true))))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `trigger with neither is 400`() {
        assertEquals(400, statusOf(triggerPost(triggerBody(neither = true))))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `cancel pending job returns 200`() {
        val id = seedJob(ReplayJobStatus.PENDING)
        assertEquals(200, statusOf(post("/api/replayJobs/$id/cancel").with(csrf())))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `cancel completed job returns 409`() {
        val id = seedJob(ReplayJobStatus.COMPLETED)
        assertEquals(409, statusOf(post("/api/replayJobs/$id/cancel").with(csrf())))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `cancel unknown job returns 404`() {
        assertEquals(404, statusOf(post("/api/replayJobs/${UUID.randomUUID()}/cancel").with(csrf())))
    }
}
