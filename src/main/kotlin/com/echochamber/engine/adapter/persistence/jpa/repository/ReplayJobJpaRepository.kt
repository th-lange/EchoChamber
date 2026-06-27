package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.ReplayJobEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for [ReplayJobEntity]. Jobs are inserted then updated **programmatically** as
 * they progress (via the storage adapter), so the full [JpaRepository] surface is retained
 * for application code.
 *
 * Over HTTP, however, jobs are **read-only**: the mutating methods are hidden from Spring
 * Data REST with `@RestResource(exported = false)`. `@RestResource(exported=false)` affects
 * only the REST exposure — programmatic calls to `save`/`delete` continue to work, so the
 * replay scheduler can still persist job progress. Replay jobs are created and cancelled
 * through dedicated action endpoints (TICKET-014), never via generic REST CRUD.
 */
@Repository
@RepositoryRestResource(path = "replayJobs", collectionResourceRel = "replayJobs")
interface ReplayJobJpaRepository : JpaRepository<ReplayJobEntity, UUID> {

    @RestResource(exported = false)
    override fun <S : ReplayJobEntity> save(entity: S): S

    @RestResource(exported = false)
    override fun delete(entity: ReplayJobEntity)

    @RestResource(exported = false)
    override fun deleteById(id: UUID)
}
