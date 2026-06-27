package com.echochamber.engine.adapter.http

import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.MutableRequest
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Unit test for [WebClientHttpExecutor] against a JDK [HttpServer] — no Docker required.
 */
class WebClientHttpExecutorTest {

    private fun client(responseTimeoutMs: Long = 1000): WebClient {
        val httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(responseTimeoutMs))
        return WebClient.builder().clientConnector(ReactorClientHttpConnector(httpClient)).build()
    }

    private fun startServer(handler: (com.sun.net.httpserver.HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handler(exchange) }
        server.executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        server.start()
        return server
    }

    private fun req(uri: String, method: String = "GET", body: String? = null) = MutableRequest(
        id = UUID.randomUUID(),
        capturedAt = Instant.now(),
        method = method,
        uri = uri,
        authority = "127.0.0.1",
        headers = mapOf("accept" to "*/*"),
        body = body,
    )

    @Test
    fun `successful response is mapped to SUCCESS with status and body`() = runBlocking {
        val server = startServer { ex ->
            val resp = "pong".toByteArray()
            ex.sendResponseHeaders(200, resp.size.toLong())
            ex.responseBody.use { it.write(resp) }
        }
        try {
            val result = WebClientHttpExecutor(client())
                .execute(req("http://127.0.0.1:${server.address.port}/", method = "POST", body = "ping"))

            assertEquals(ExecutionStatus.SUCCESS, result.status)
            assertEquals(200, result.responseStatus)
            assertEquals("pong", result.responseBody)
            assertTrue(result.responseTimeMs >= 0)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `error status still yields SUCCESS with the response code`() = runBlocking {
        val server = startServer { ex ->
            ex.sendResponseHeaders(404, -1)
            ex.close()
        }
        try {
            val result = WebClientHttpExecutor(client()).execute(req("http://127.0.0.1:${server.address.port}/"))
            assertEquals(ExecutionStatus.SUCCESS, result.status)
            assertEquals(404, result.responseStatus)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `slow response beyond the read timeout yields TIMEOUT`() = runBlocking {
        val server = startServer { ex ->
            Thread.sleep(1500)
            ex.sendResponseHeaders(200, -1)
            ex.close()
        }
        try {
            val result = WebClientHttpExecutor(client(responseTimeoutMs = 300))
                .execute(req("http://127.0.0.1:${server.address.port}/"))
            assertEquals(ExecutionStatus.TIMEOUT, result.status)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `connection refused yields FAILURE`() = runBlocking {
        val freePort = ServerSocket(0).use { it.localPort } // closed immediately -> nothing listening
        val result = WebClientHttpExecutor(client()).execute(req("http://127.0.0.1:$freePort/"))
        assertEquals(ExecutionStatus.FAILURE, result.status)
    }
}
