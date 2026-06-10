package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.CapturedRequestEntity
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Repository as SpringRepository
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * Append-only repository for [CapturedRequestEntity].
 *
 * Extends the marker [Repository] interface rather than `JpaRepository` so that no
 * `delete*`, `deleteAll*`, or other mutating methods are exposed. The only operations
 * permitted are insert (`save`) and read (`findById`, `findAll`).
 */
@SpringRepository
interface CapturedRequestJpaRepository : Repository<CapturedRequestEntity, UUID> {

    fun save(entity: CapturedRequestEntity): CapturedRequestEntity

    fun findById(id: UUID): Optional<CapturedRequestEntity>

    fun findAll(): List<CapturedRequestEntity>

    fun findAllByMethod(method: String): List<CapturedRequestEntity>

    fun findAllByAuthority(authority: String): List<CapturedRequestEntity>

    fun findAllByCapturedAtBetween(from: Instant, to: Instant): List<CapturedRequestEntity>
}
