package com.echochamber.engine.domain.model

/** Auditable admin actions (TICKET-021). */
enum class AuditAction {
    LOGIN,
    LOGIN_FAILED,
    RETRY_TRIGGERED,
    RETRY_CANCELLED,
    USER_CREATED,
    USER_DISABLED,
    USER_ENABLED,
    USER_ROLE_CHANGED,
    USER_PASSWORD_RESET,
    CONFIG_CHANGED,
}
