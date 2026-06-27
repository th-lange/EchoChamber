package com.echochamber.engine.domain.port

/**
 * Password hashing port so the application layer stays free of Spring Security imports.
 * Implemented in `adapter/` over BCrypt.
 */
interface PasswordHasher {
    fun hash(raw: String): String
    fun matches(raw: String, hash: String): Boolean
}
