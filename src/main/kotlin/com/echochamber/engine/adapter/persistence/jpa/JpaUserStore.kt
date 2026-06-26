package com.echochamber.engine.adapter.persistence.jpa

import com.echochamber.engine.adapter.persistence.jpa.entity.UserEntity
import com.echochamber.engine.adapter.persistence.jpa.repository.UserJpaRepository
import com.echochamber.engine.domain.model.User
import com.echochamber.engine.domain.model.UserRole
import com.echochamber.engine.domain.port.UserStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.util.UUID

/** JPA-backed [UserStore]. Blocking JPA calls dispatched on [Dispatchers.IO]. */
@Component
class JpaUserStore(
    private val repository: UserJpaRepository,
) : UserStore {

    override suspend fun findByUsername(username: String): User? = withContext(Dispatchers.IO) {
        repository.findByUsername(username).map(::toDomain).orElse(null)
    }

    override suspend fun findById(id: UUID): User? = withContext(Dispatchers.IO) {
        repository.findById(id).map(::toDomain).orElse(null)
    }

    override suspend fun findAll(): List<User> = withContext(Dispatchers.IO) {
        repository.findAll().map(::toDomain)
    }

    override suspend fun save(user: User): User = withContext(Dispatchers.IO) {
        toDomain(repository.save(toEntity(user)))
    }

    override suspend fun countActiveAdmins(): Int = withContext(Dispatchers.IO) {
        repository.countByRoleAndEnabledTrue(UserRole.ADMIN.name).toInt()
    }

    private fun toDomain(e: UserEntity) = User(
        id = e.id,
        username = e.username,
        passwordHash = e.passwordHash,
        role = UserRole.valueOf(e.role),
        enabled = e.enabled,
        mustChangePassword = e.mustChangePassword,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
        createdBy = e.createdBy,
    )

    private fun toEntity(u: User) = UserEntity(
        id = u.id,
        username = u.username,
        passwordHash = u.passwordHash,
        role = u.role.name,
        enabled = u.enabled,
        mustChangePassword = u.mustChangePassword,
        createdAt = u.createdAt,
        updatedAt = u.updatedAt,
        createdBy = u.createdBy,
    )
}
