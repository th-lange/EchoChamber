package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.ReplayJobEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for [ReplayJobEntity]. Jobs are inserted then updated as they progress.
 */
@Repository
interface ReplayJobJpaRepository : JpaRepository<ReplayJobEntity, UUID>
