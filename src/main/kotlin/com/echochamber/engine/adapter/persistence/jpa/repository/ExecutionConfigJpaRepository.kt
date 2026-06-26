package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.ExecutionConfigEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Full CRUD repository for [ExecutionConfigEntity]. Configs are mutable and are managed
 * entirely through Spring Data REST (`GET/POST/PUT/PATCH/DELETE /api/executionConfigs`) —
 * no hand-written controller. See Agent.md TICKET-007.
 */
@Repository
@RepositoryRestResource(path = "executionConfigs", collectionResourceRel = "executionConfigs")
interface ExecutionConfigJpaRepository : JpaRepository<ExecutionConfigEntity, UUID>
