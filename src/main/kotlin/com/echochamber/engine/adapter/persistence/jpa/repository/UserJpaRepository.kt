package com.echochamber.engine.adapter.persistence.jpa.repository

import com.echochamber.engine.adapter.persistence.jpa.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

/**
 * Repository for [UserEntity]. **Not** exported over Spring Data REST
 * (`exported = false`) so password hashes never serialize and user management goes only
 * through the ADMIN-gated `UserController`.
 */
@Repository
@RepositoryRestResource(exported = false)
interface UserJpaRepository : JpaRepository<UserEntity, UUID> {
    fun findByUsername(username: String): Optional<UserEntity>
    fun countByRoleAndEnabledTrue(role: String): Long
}
