package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.ExecutionLogEntity
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Repository as SpringRepository
import java.util.Optional
import java.util.UUID

/**
 * Append-only repository for [ExecutionLogEntity]. Mirrors the captured-request
 * repository in that no `delete*` operations are exposed.
 */
@SpringRepository
interface ExecutionLogJpaRepository : Repository<ExecutionLogEntity, UUID> {

    fun save(entity: ExecutionLogEntity): ExecutionLogEntity

    fun findById(id: UUID): Optional<ExecutionLogEntity>

    fun findAll(): List<ExecutionLogEntity>

    fun findAllByJobId(jobId: UUID): List<ExecutionLogEntity>

    fun findAllByRequestId(requestId: UUID): List<ExecutionLogEntity>
}
