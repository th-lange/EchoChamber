package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.ExecutionLogEntity
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.stereotype.Repository as SpringRepository
import java.util.Optional
import java.util.UUID

/**
 * Append-only repository for [ExecutionLogEntity].
 *
 * Mirrors [CapturedRequestJpaRepository]: paged/sorted reads only, [save] hidden from
 * Spring Data REST so logs are exposed **read-only** over HTTP. Execution logs are an
 * audit record and must never be mutated or deleted through the REST API.
 */
@SpringRepository
@RepositoryRestResource(path = "executionLogs", collectionResourceRel = "executionLogs")
interface ExecutionLogJpaRepository : PagingAndSortingRepository<ExecutionLogEntity, UUID> {

    @RestResource(exported = false)
    fun save(entity: ExecutionLogEntity): ExecutionLogEntity

    fun findById(id: UUID): Optional<ExecutionLogEntity>

    fun findAll(): List<ExecutionLogEntity>

    fun findAllByJobId(jobId: UUID): List<ExecutionLogEntity>

    fun findAllByRequestId(requestId: UUID): List<ExecutionLogEntity>
}
