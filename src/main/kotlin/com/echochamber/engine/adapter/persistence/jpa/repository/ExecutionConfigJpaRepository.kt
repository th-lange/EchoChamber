package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.ExecutionConfigEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Full CRUD repository for [ExecutionConfigEntity]. Configs are mutable.
 */
@Repository
interface ExecutionConfigJpaRepository : JpaRepository<ExecutionConfigEntity, UUID>
