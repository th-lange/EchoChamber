package com.echochamber.engine.application

import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.ExecutionStatus
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.model.RequestOverride
import com.echochamber.engine.domain.model.toMutable
import com.echochamber.engine.domain.port.HttpExecutor
import com.echochamber.engine.domain.port.StorageAdapter
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/** Which captured requests a replay job should process. */
data class ReplaySelection(
    val requestIds: List<UUID>? = null,
    val filter: ReplayFilter? = null,
)

/**
 * Core async replay loop (TICKET-013).
 *
 * Loads the job + config, resolves the request set, then for each request copies it to a
 * [com.echochamber.engine.domain.model.MutableRequest], runs the [MutationEngine] (plus the
 * optional inline [RequestOverride]), fires it via [HttpExecutor], and persists an
 * [ExecutionLog]. Concurrency is bounded by a coroutine [Semaphore] sized to
 * `ExecutionConfig.maxConcurrency`; throughput is bounded by a Resilience4j [RateLimiter]
 * derived from `ExecutionConfig.rateLimitPerSecond` (coroutine-suspending, never
 * `Thread.sleep`). A single request failure never aborts the job.
 *
 * Depends only on ports + [MutationEngine] — no concrete adapters.
 */
@Service
class ReplayService(
    private val storage: StorageAdapter,
    private val mutationEngine: MutationEngine,
    private val httpExecutor: HttpExecutor,
) {

    private val log = LoggerFactory.getLogger(ReplayService::class.java)

    suspend fun executeJob(
        jobId: UUID,
        selection: ReplaySelection,
        override: RequestOverride? = null,
    ) {
        val job = storage.getJob(jobId)
        if (job == null) {
            log.warn("executeJob called for unknown job {}", jobId)
            return
        }
        val config = storage.findConfig(job.configId)
        if (config == null) {
            log.error("Replay job {} references missing config {} — marking FAILED", jobId, job.configId)
            storage.updateJob(job.copy(status = ReplayJobStatus.FAILED, completedAt = Instant.now()))
            return
        }

        val requests = resolveRequests(selection)
        val running = job.copy(
            status = ReplayJobStatus.RUNNING,
            startedAt = Instant.now(),
            totalRequests = requests.size,
            processedRequests = 0,
            failedRequests = 0,
        )
        storage.updateJob(running)

        val processed = AtomicInteger(0)
        val failed = AtomicInteger(0)
        val concurrency = config.maxConcurrency.takeIf { it > 0 } ?: DEFAULT_CONCURRENCY
        val semaphore = Semaphore(concurrency)
        val rateLimiter = buildRateLimiter(jobId, config.rateLimitPerSecond)

        try {
            coroutineScope {
                requests.forEach { request ->
                    launch {
                        semaphore.withPermit {
                            val result = runRequest(rateLimiter, request, config, override)
                            storage.saveLog(toLog(jobId, request.id, result))
                            if (result.status == ExecutionStatus.SUCCESS) processed.incrementAndGet() else failed.incrementAndGet()
                            storage.updateJob(running.copy(processedRequests = processed.get(), failedRequests = failed.get()))
                        }
                    }
                }
            }
            storage.updateJob(
                running.copy(
                    status = ReplayJobStatus.COMPLETED,
                    processedRequests = processed.get(),
                    failedRequests = failed.get(),
                    completedAt = Instant.now(),
                ),
            )
            log.info("Replay job {} completed: {} processed, {} failed", jobId, processed.get(), failed.get())
        } catch (e: CancellationException) {
            storage.updateJob(
                running.copy(
                    status = ReplayJobStatus.FAILED,
                    processedRequests = processed.get(),
                    failedRequests = failed.get(),
                    completedAt = Instant.now(),
                ),
            )
            throw e
        } catch (e: Exception) {
            log.error("Replay job {} failed", jobId, e)
            storage.updateJob(
                running.copy(
                    status = ReplayJobStatus.FAILED,
                    processedRequests = processed.get(),
                    failedRequests = failed.get(),
                    completedAt = Instant.now(),
                ),
            )
        }
    }

    private suspend fun runRequest(
        rateLimiter: RateLimiter?,
        request: CapturedRequest,
        config: com.echochamber.engine.domain.model.ExecutionConfig,
        override: RequestOverride?,
    ): ExecutionResult {
        val exec: suspend () -> ExecutionResult = {
            val mutated = mutationEngine.mutate(request.toMutable(), config, override)
            httpExecutor.execute(mutated)
        }
        return try {
            if (rateLimiter != null) rateLimiter.executeSuspendFunction(exec) else exec()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Mutation error or unexpected failure — never abort the job.
            log.warn("Replay of request {} failed: {}", request.id, e.toString())
            ExecutionResult(ExecutionStatus.FAILURE, null, 0, emptyMap(), null)
        }
    }

    private suspend fun resolveRequests(selection: ReplaySelection): List<CapturedRequest> = when {
        !selection.requestIds.isNullOrEmpty() -> selection.requestIds.mapNotNull { storage.findRequest(it) }
        selection.filter != null -> storage.findRequests(selection.filter).toList()
        else -> emptyList()
    }

    private fun buildRateLimiter(jobId: UUID, ratePerSecond: Double): RateLimiter? {
        if (ratePerSecond <= 0.0) return null
        val config = RateLimiterConfig.custom()
            .limitForPeriod(ceil(ratePerSecond).toInt().coerceAtLeast(1))
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMinutes(5))
            .build()
        return RateLimiter.of("replay-$jobId", config)
    }

    private fun toLog(jobId: UUID, requestId: UUID, result: ExecutionResult) = ExecutionLog(
        id = UUID.randomUUID(),
        jobId = jobId,
        requestId = requestId,
        status = result.status,
        responseStatus = result.responseStatus,
        responseTimeMs = result.responseTimeMs,
        responseHeaders = result.responseHeaders,
        responseBody = result.responseBody,
        executedAt = Instant.now(),
    )

    private companion object {
        const val DEFAULT_CONCURRENCY = 10
    }
}
