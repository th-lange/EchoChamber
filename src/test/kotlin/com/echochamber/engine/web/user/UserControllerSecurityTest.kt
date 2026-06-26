package com.echochamber.engine.web.user

import com.echochamber.engine.application.UserService
import com.echochamber.engine.domain.port.UserStore
import com.echochamber.engine.support.FakePasswordHasher
import com.echochamber.engine.support.FakeUserStore
import com.echochamber.engine.web.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Verifies the RBAC posture of `/api/users` (ADMIN-only) via the real [SecurityConfig].
 * No database/Docker — the service is backed by in-memory fakes.
 */
@WebMvcTest(controllers = [UserController::class])
@Import(UserControllerSecurityTest.TestConfig::class, SecurityConfig::class)
class UserControllerSecurityTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    class TestConfig {
        private val store = FakeUserStore()

        @Bean
        fun userStore(): UserStore = store

        @Bean
        fun userService(): UserService = UserService(store, FakePasswordHasher())
    }

    @Test
    fun `unauthenticated request is redirected to login`() {
        mockMvc.perform(get("/api/users")).andExpect(status().is3xxRedirection)
    }

    @Test
    @WithMockUser(roles = ["VIEWER"])
    fun `viewer is forbidden`() {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can list users`() {
        mockMvc.perform(get("/api/users")).andExpect(status().isOk)
    }
}
