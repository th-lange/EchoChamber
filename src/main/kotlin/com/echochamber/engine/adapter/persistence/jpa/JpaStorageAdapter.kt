package com.echochamber.engine.adapter.persistence.jpa

import com.echochamber.engine.adapter.persistence.jpa.repository.CapturedRequestJpaRepository
import com.echochamber.engine.adapter.persistence.jpa.repository.ExecutionConfigJpaRepository
import com.echochamber.engine.adapter.persistence.jpa.repository.ExecutionLogJpaRepository
import com.echochamber.engine.adapter.persistence.jpa.repository.ReplayJobJpaRepository
import com.echochamber.engine.domain.model.CapturedRequest
import com.echochamber.engine.domain.model.ExecutionConfig
import com.echochamber.engine.domain.model.ExecutionLog
import com.echochamber.engine.domain.model.ReplayFilter
import com.echochamber.engine.domain.model.ReplayJob
import com.echochamber.engine.domain.port.StorageAdapter
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * JPA-backed implementation of [StorageAdapter].
 *
 * All blocking Spring Data JPA calls are dispatched on [Dispatchers.IO] so they never
 * block a calling coroutine's dispatcher.
 *
 * **Transactions.** Spring's `@Transactional` proxy is intentionally not used on these
 * suspend functions: with blocking JPA + Hibernate the EntityManager is bound to the
 * thread that begins the transaction, while `withContext(Dispatchers.IO)` schedules the
 * body on a different thread — breaking session affinity. Each individual Spring Data
 * repository method already runs inside its own short transaction (defined by
 * `SimpleJpaRepository`), which is sufficient for the single-operation use cases here.
 * Multi-statement use cases that need a single atomic transaction should call a
 * dedicated blocking helper bean from inside `withContext` instead.
 */
@Component
class JpaStorageAdapter(
    private val requestRepository: CapturedRequestJpaRepository,
    private val configRepository: ExecutionConfigJpaRepository,
    private val jobRepository: ReplayJobJpaRepository,
    private val logRepository: ExecutionLogJpaRepository,
    objectMapper: ObjectMapper
) : StorageAdapter {

    private val mapper = JpaEntityMapper(objectMapper)

    // ---------- Captured requests ----------

    override suspend fun appendRequest(r: CapturedRequest): CapturedRequest =
        withContext(Dispatchers.IO) {
            mapper.toDomain(requestRepository.save(mapper.toEntity(r)))
        }

    override suspend fun findRequest(id: UUID): CapturedRequest? =
        withContext(Dispatchers.IO) {
            requestRepository.findById(id).map(mapper::toDomain).orElse(null)
        }

    override suspend fun findRecentDuplicate(
        method: String,
        uri: String,
        authority: String,
        notBefore: Instant,
    ): CapturedRequest? =
        withContext(Dispatchers.IO) {
            requestRepository
                .findFirstByMethodAndUriAndAuthorityAndCapturedAtGreaterThanEqual(method, uri, authority, notBefore)
                ?.let(mapper::toDomain)
        }

    override fun findRequests(filter: ReplayFilter): Flow<CapturedRequest> = flow {
        val all = requestRepository.findAll()
        val uriRegex = filter.uriPattern?.let { Regex(it) }
        all.asSequence()
            .filter { e -> filter.method == null || e.method.equals(filter.method, ignoreCase = true) }
            .filter { e -> filter.authority == null || e.authority == filter.authority }
            .filter { e -> filter.capturedAfter == null || !e.capturedAt.isBefore(filter.capturedAfter) }
            .filter { e -> filter.capturedBefore == null || e.capturedAt.isBefore(filter.capturedBefore) }
            .filter { e -> uriRegex == null || uriRegex.containsMatchIn(e.uri) }
            .map(mapper::toDomain)
            .forEach { emit(it) }
    }.flowOn(Dispatchers.IO)

    // ---------- Execution configs ----------

    override suspend fun saveConfig(c: ExecutionConfig): ExecutionConfig =
        withContext(Dispatchers.IO) {
            val now = Instant.now()
            val existing = configRepository.findById(c.id).orElse(null)
            mapper.toDomain(configRepository.save(mapper.toEntity(c, existing, now)))
        }

    override suspend fun findConfig(id: UUID): ExecutionConfig? =
        withContext(Dispatchers.IO) {
            configRepository.findById(id).map(mapper::toDomain).orElse(null)
        }

    override suspend fun deleteConfig(id: UUID) {
        withContext(Dispatchers.IO) {
            configRepository.deleteById(id)
        }
    }

    // ---------- Replay jobs ----------

    override suspend fun saveJob(j: ReplayJob): ReplayJob =
        withContext(Dispatchers.IO) {
            mapper.toDomain(jobRepository.save(mapper.toEntity(j)))
        }

    override suspend fun updateJob(j: ReplayJob): ReplayJob =
        withContext(Dispatchers.IO) {
            mapper.toDomain(jobRepository.save(mapper.toEntity(j)))
        }

    override suspend fun getJob(id: UUID): ReplayJob? =
        withContext(Dispatchers.IO) {
            jobRepository.findById(id).map(mapper::toDomain).orElse(null)
        }

    // ---------- Execution logs ----------

    override suspend fun saveLog(l: ExecutionLog): ExecutionLog =
        withContext(Dispatchers.IO) {
            mapper.toDomain(logRepository.save(mapper.toEntity(l)))
        }

    // ---------- Console read queries ----------

    override suspend fun listJobs(limit: Int): List<ReplayJob> =
        withContext(Dispatchers.IO) {
            jobRepository.findAll()
                .sortedByDescending { it.startedAt ?: Instant.MAX }
                .take(limit)
                .map(mapper::toDomain)
        }

    override suspend fun listLogs(limit: Int): List<ExecutionLog> =
        withContext(Dispatchers.IO) {
            logRepository.findAll()
                .sortedByDescending { it.executedAt }
                .take(limit)
                .map(mapper::toDomain)
        }

    override suspend fun executionCountsByRequest(): Map<UUID, Long> =
        withContext(Dispatchers.IO) {
            logRepository.countsByRequest().associate { row -> (row[0] as UUID) to (row[1] as Long) }
        }
}
