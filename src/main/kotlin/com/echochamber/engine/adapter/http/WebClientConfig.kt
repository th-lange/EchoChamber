package com.echochamber.engine.adapter.http

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Builds the [WebClient] used by [WebClientHttpExecutor] for replay traffic.
 *
 * Timeouts and the max in-memory response size are configurable so a slow or oversized
 * target cannot stall or OOM a replay job. Redirects are left disabled so a replay hits
 * exactly the target it was given.
 */
@Configuration
class WebClientConfig {

    @Bean
    fun replayWebClient(
        @Value("\${echochamber.executor.connect-timeout-ms:5000}") connectTimeoutMs: Int,
        @Value("\${echochamber.executor.read-timeout-ms:10000}") readTimeoutMs: Long,
        @Value("\${echochamber.executor.max-body-bytes:5242880}") maxBodyBytes: Int,
    ): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs))
            .followRedirect(false)

        val strategies = ExchangeStrategies.builder()
            .codecs { it.defaultCodecs().maxInMemorySize(maxBodyBytes) }
            .build()

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .build()
    }
}
