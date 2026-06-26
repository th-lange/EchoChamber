package com.echochamber.engine.domain.model

/**
 * Admin console access levels.
 * - [VIEWER]   read-only (lists, retry history)
 * - [OPERATOR] may trigger retries and modify-before-retry
 * - [ADMIN]    may also manage users and view the audit log
 */
enum class UserRole {
    VIEWER,
    OPERATOR,
    ADMIN,
}
