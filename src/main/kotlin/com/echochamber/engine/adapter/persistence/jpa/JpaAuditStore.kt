package com.echochamber.engine.adapter.persistence.jpa

import com.echochamber.engine.adapter.persistence.jpa.entity.AuditLogEntity
import com.echochamber.engine.adapter.persistence.jpa.repository.AuditLogJpaRepository
import com.echochamber.engine.domain.model.AuditAction
import com.echochamber.engine.domain.model.AuditEntry
import com.echochamber.engine.domain.port.AuditStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/** JPA-backed [AuditStore]. */
@Component
class JpaAuditStore(
    private val repository: AuditLogJpaRepository,
) : AuditStore {

    override suspend fun append(entry: AuditEntry): AuditEntry = withContext(Dispatchers.IO) {
        repository.save(
            AuditLogEntity(
                id = entry.id,
                actorUserId = entry.actorUserId,
                actorUsername = entry.actorUsername,
                action = entry.action.name,
                targetType = entry.targetType,
                targetId = entry.targetId,
                detail = entry.detail,
                occurredAt = entry.occurredAt,
            ),
        )
        entry
    }
}
