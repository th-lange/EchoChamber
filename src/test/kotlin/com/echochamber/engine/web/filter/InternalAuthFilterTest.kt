package com.echochamber.engine.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.`when` as mockWhen

@ExtendWith(MockitoExtension::class)
class InternalAuthFilterTest {

    private val token = "test-secret-token"

    private class TestableFilter(token: String) : InternalAuthFilter(token) {
        public override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            super.doFilterInternal(request, response, filterChain)
        }

        public override fun shouldNotFilter(request: HttpServletRequest): Boolean =
            super.shouldNotFilter(request)
    }

    @Test
    fun `missing Authorization header returns 401`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        mockWhen(request.servletPath).thenReturn("/internal/ingest")
        mockWhen(request.getHeader("Authorization")).thenReturn(null)

        TestableFilter(token).doFilterInternal(request, response, chain)

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token")
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `wrong token returns 401`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        mockWhen(request.servletPath).thenReturn("/internal/ingest")
        mockWhen(request.getHeader("Authorization")).thenReturn("Bearer wrong-token")

        TestableFilter(token).doFilterInternal(request, response, chain)

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token")
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `correct token passes through to chain`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        mockWhen(request.getHeader("Authorization")).thenReturn("Bearer $token")

        TestableFilter(token).doFilterInternal(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(response, never()).sendError(anyInt(), anyString())
    }

    @Test
    fun `blank expected token disables auth and passes through`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        // No Authorization header, but the filter is configured with no token —
        // ingest auth is disabled, so the request passes straight through.
        TestableFilter("").doFilterInternal(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(response, never()).sendError(anyInt(), anyString())
    }

    @Test
    fun `whitespace-only expected token disables auth and passes through`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        TestableFilter("   ").doFilterInternal(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(response, never()).sendError(anyInt(), anyString())
    }

    @Test
    fun `non-internal path is skipped by shouldNotFilter`() {
        val request = mock(HttpServletRequest::class.java)
        mockWhen(request.servletPath).thenReturn("/api/configs")

        assertTrue(TestableFilter(token).shouldNotFilter(request))
    }

    @Test
    fun `internal path is not skipped`() {
        val request = mock(HttpServletRequest::class.java)
        mockWhen(request.servletPath).thenReturn("/internal/ingest")

        assertFalse(TestableFilter(token).shouldNotFilter(request))
    }

    @Test
    fun `malformed Authorization header without Bearer prefix returns 401`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        mockWhen(request.servletPath).thenReturn("/internal/ingest")
        mockWhen(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz")

        TestableFilter(token).doFilterInternal(request, response, chain)

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token")
        verify(chain, never()).doFilter(request, response)
    }
}
