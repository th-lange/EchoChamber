package com.echochamber.engine.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

@Component
class InternalAuthFilter(
    @Value("\${INTERNAL_INGEST_TOKEN:}") private val expectedToken: String
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(InternalAuthFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.servletPath.startsWith("/internal/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")

        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header on {}", request.servletPath)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token")
            return
        }

        val token = header.substring(7)

        if (!constantTimeEquals(token, expectedToken)) {
            log.warn("Invalid token on {}", request.servletPath)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
