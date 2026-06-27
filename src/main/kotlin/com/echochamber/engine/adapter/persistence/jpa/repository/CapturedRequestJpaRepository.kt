package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.CapturedRequestEntity
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.stereotype.Repository as SpringRepository
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * Append-only repository for [CapturedRequestEntity].
 *
 * Extends [PagingAndSortingRepository] (which, since Spring Data 3.0, no longer extends
 * `CrudRepository`) so the only inherited operations are paged/sorted reads — never a
 * `delete*`. The single mutating method, [save], is required by the storage adapter but
 * is hidden from Spring Data REST via `@RestResource(exported = false)`, so the entity is
 * exposed **read-only** over HTTP: paginated `GET` lists and item `GET`, but no
 * `POST`/`PUT`/`PATCH`/`DELETE`. This enforces the captured-request immutability rule at
 * the REST boundary (Agent.md §2).
 */
@SpringRepository
@RepositoryRestResource(path = "capturedRequests", collectionResourceRel = "capturedRequests")
interface CapturedRequestJpaRepository : PagingAndSortingRepository<CapturedRequestEntity, UUID> {

    @RestResource(exported = false)
    fun save(entity: CapturedRequestEntity): CapturedRequestEntity

    fun findById(id: UUID): Optional<CapturedRequestEntity>

    fun findAll(): List<CapturedRequestEntity>

    fun findAllByMethod(method: String): List<CapturedRequestEntity>

    fun findAllByAuthority(authority: String): List<CapturedRequestEntity>

    fun findAllByCapturedAtBetween(from: Instant, to: Instant): List<CapturedRequestEntity>

    /** Idempotency lookup for ingest; not exposed over REST. */
    @RestResource(exported = false)
    fun findFirstByMethodAndUriAndAuthorityAndCapturedAtGreaterThanEqual(
        method: String,
        uri: String,
        authority: String,
        capturedAt: Instant,
    ): CapturedRequestEntity?
}
