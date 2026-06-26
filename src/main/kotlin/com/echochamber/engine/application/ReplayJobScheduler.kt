package com.echochamber.engine.application

import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.model.ReplayJobStatus
import com.echochamber.engine.domain.model.RequestOverride
import com.echochamber.engine.domain.port.StorageAdapter
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates replay jobs and runs them asynchronously (TICKET-012).
 *
 * [scheduleJob] persists a `PENDING` job, launches [ReplayService.executeJob] on a managed
 * coroutine scope, and returns immediately so the trigger endpoint can answer `202`. Active
 * coroutines are tracked by job id so [cancelJob] can abort a running replay.
 */
@Service
class ReplayJobScheduler(
    private val storage: StorageAdapter,
    private val replayService: ReplayService,
) {

    private val log = LoggerFactory.getLogger(ReplayJobScheduler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val active = ConcurrentHashMap<UUID, Job>()

    suspend fun scheduleJob(
        configId: UUID,
        selection: ReplaySelection,
        override: RequestOverride? = null,
    ): ReplayJob {
        val job = ReplayJob(
            id = UUID.randomUUID(),
            configId = configId,
            status = ReplayJobStatus.PENDING,
            totalRequests = 0,
            processedRequests = 0,
            failedRequests = 0,
            startedAt = null,
            completedAt = null,
        )
        storage.saveJob(job)

        val coroutine = scope.launch {
            try {
                replayService.executeJob(job.id, selection, override)
            } catch (e: Exception) {
                log.error("Replay job {} terminated abnormally", job.id, e)
            } finally {
                active.remove(job.id)
            }
        }
        active[job.id] = coroutine
        log.info("Scheduled replay job {} for config {}", job.id, configId)
        return job
    }

    /** Cancels the running coroutine for [id]; returns true if a job was active. */
    fun cancelJob(id: UUID): Boolean {
        val coroutine = active.remove(id) ?: return false
        coroutine.cancel()
        log.info("Cancelled replay job {}", id)
        return true
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }
}
