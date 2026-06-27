package com.echochamber.engine.web.security

import com.echochamber.engine.web.filter.InternalAuthFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Two security filter chains (TICKET-019):
 *
 * 1. `/internal/` paths — machine-to-machine for SnapReq. CSRF off, stateless, permitAll at the
 *    Spring Security level; the actual gate is [InternalAuthFilter] (static bearer token),
 *    wired explicitly here. Its global auto-registration is disabled so it neither
 *    double-runs nor is bypassed.
 * 2. everything else — admin console form login with role-based authorization.
 *
 * Roles: VIEWER (read), OPERATOR (+retry/modify), ADMIN (+users/audit).
 */
@Configuration
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** Prevent Boot from auto-registering [InternalAuthFilter] as a global servlet filter. */
    @Bean
    fun internalAuthFilterRegistration(filter: InternalAuthFilter): FilterRegistrationBean<InternalAuthFilter> =
        FilterRegistrationBean(filter).apply { isEnabled = false }

    @Bean
    @Order(1)
    fun internalChain(http: HttpSecurity, internalAuthFilter: InternalAuthFilter): SecurityFilterChain {
        http {
            securityMatcher("/internal/**")
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests { authorize(anyRequest, permitAll) }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(internalAuthFilter)
        }
        return http.build()
    }

    @Bean
    @Order(2)
    fun adminChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/login", permitAll)
                authorize("/css/**", permitAll)
                authorize("/js/**", permitAll)
                authorize("/webjars/**", permitAll)
                authorize("/error", permitAll)
                // ADMIN-only resources (before the broad GET rule)
                authorize("/api/auditLog/**", hasRole("ADMIN"))
                authorize("/api/users/**", hasRole("ADMIN"))
                authorize("/admin/users/**", hasRole("ADMIN"))
                authorize("/admin/audit/**", hasRole("ADMIN"))
                authorize("/admin/requests/reexecute", hasAnyRole("OPERATOR", "ADMIN"))
                // OPERATOR+ actions
                authorize("/api/replayJobs/trigger", hasAnyRole("OPERATOR", "ADMIN"))
                authorize("/api/replayJobs/*/cancel", hasAnyRole("OPERATOR", "ADMIN"))
                authorize(HttpMethod.POST, "/api/executionConfigs/**", hasAnyRole("OPERATOR", "ADMIN"))
                authorize(HttpMethod.PUT, "/api/executionConfigs/**", hasAnyRole("OPERATOR", "ADMIN"))
                authorize(HttpMethod.PATCH, "/api/executionConfigs/**", hasAnyRole("OPERATOR", "ADMIN"))
                authorize(HttpMethod.DELETE, "/api/executionConfigs/**", hasAnyRole("OPERATOR", "ADMIN"))
                // read access for any authenticated role
                authorize(HttpMethod.GET, "/api/**", hasAnyRole("VIEWER", "OPERATOR", "ADMIN"))
                authorize(anyRequest, authenticated)
            }
            formLogin { loginPage = "/login"; permitAll() }
            logout { logoutSuccessUrl = "/login?logout"; permitAll() }
        }
        return http.build()
    }
}
