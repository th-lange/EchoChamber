package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.AuditEntry

/** Append-only persistence port for the audit trail. */
interface AuditStore {
    suspend fun append(entry: AuditEntry): AuditEntry
}
