package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.AuditLogEntity
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.stereotype.Repository as SpringRepository
import java.util.Optional
import java.util.UUID

/**
 * Append-only repository for [AuditLogEntity]. Exposed **read-only** over Spring Data REST
 * at `/api/auditLog` (save hidden); access is further restricted to ADMIN in `SecurityConfig`.
 */
@SpringRepository
@RepositoryRestResource(path = "auditLog", collectionResourceRel = "auditLog")
interface AuditLogJpaRepository : PagingAndSortingRepository<AuditLogEntity, UUID> {

    @RestResource(exported = false)
    fun save(entity: AuditLogEntity): AuditLogEntity

    fun findById(id: UUID): Optional<AuditLogEntity>

    fun findAll(): List<AuditLogEntity>
}
