package com.echochamber.engine.web.console

import com.echochamber.engine.application.AuditService
import com.echochamber.engine.application.ConsoleService
import com.echochamber.engine.application.MutationEngine
import com.echochamber.engine.application.ReplayJobScheduler
import com.echochamber.engine.application.ReplayService
import com.echochamber.engine.application.UserService
import com.echochamber.engine.domain.model.AuditEntry
import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.echochamber.engine.domain.port.AuditStore
import com.echochamber.engine.domain.port.HttpExecutor
import com.echochamber.engine.support.FakePasswordHasher
import com.echochamber.engine.support.FakeStorageAdapter
import com.echochamber.engine.support.FakeUserStore
import com.echochamber.engine.web.security.SecurityConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import java.util.UUID

@WebMvcTest(controllers = [ConsoleController::class])
@Import(ConsoleControllerTest.TestConfig::class, SecurityConfig::class, ConsoleWebConfig::class, ForcedPasswordChangeInterceptor::class)
class ConsoleControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    class TestConfig {
        val storage = FakeStorageAdapter()

        @Bean fun consoleService() = ConsoleService(storage)
        @Bean fun userService() = UserService(FakeUserStore(), FakePasswordHasher())
        @Bean fun audit() = AuditService(object : AuditStore {
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

    private fun statusOf(builder: RequestBuilder): Int {
        val result = mockMvc.perform(builder).andReturn()
        val finalResult = if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)).andReturn() else result
        return finalResult.response.status
    }

    @Test
    fun `unauthenticated requests page redirects to login`() {
        assertEquals(302, statusOf(get("/admin/requests")))
    }

    @Test
    @WithMockUser(roles = ["VIEWER"])
    fun `viewer can see requests page`() {
        assertEquals(200, statusOf(get("/admin/requests")))
    }

    @Test
    @WithMockUser(roles = ["VIEWER"])
    fun `viewer cannot reexecute`() {
        assertEquals(403, statusOf(post("/admin/requests/reexecute").with(csrf()).param("requestIds", UUID.randomUUID().toString())))
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `operator reexecute of selected requests redirects to history`() {
        assertEquals(
            302,
            statusOf(
                post("/admin/requests/reexecute").with(csrf())
                    .param("requestIds", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                    .param("targetUrl", "https://staging.example.com"),
            ),
        )
    }

    @Test
    @WithMockUser(roles = ["OPERATOR"])
    fun `operator reexecute with no selection redirects without error`() {
        assertEquals(302, statusOf(post("/admin/requests/reexecute").with(csrf())))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can see users page`() {
        assertEquals(200, statusOf(get("/admin/users")))
    }

    @Test
    @WithMockUser(roles = ["VIEWER"])
    fun `viewer cannot see users page`() {
        assertEquals(403, statusOf(get("/admin/users")))
    }
}
