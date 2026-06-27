package com.echochamber.engine.web.console

import com.echochamber.engine.application.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Redirects an authenticated user whose account is flagged `mustChangePassword` to the
 * change-password page before they can use the rest of the console (TICKET-019/020).
 */
@Component
class ForcedPasswordChangeInterceptor(
    private val userService: UserService,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val path = request.requestURI
        if (path.startsWith("/admin/password") || path == "/logout") return true

        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.name == "anonymousUser") return true

        val mustChange = runBlocking { userService.requiresPasswordChange(auth.name) }
        if (mustChange) {
            response.sendRedirect("/admin/password")
            return false
        }
        return true
    }
}
