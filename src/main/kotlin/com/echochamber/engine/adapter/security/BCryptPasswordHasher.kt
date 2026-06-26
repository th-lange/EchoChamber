package com.echochamber.engine.adapter.security

import com.echochamber.engine.domain.port.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/** [PasswordHasher] implemented over the shared Spring Security [PasswordEncoder] (BCrypt). */
@Component
class BCryptPasswordHasher(
    private val encoder: PasswordEncoder,
) : PasswordHasher {
    override fun hash(raw: String): String = encoder.encode(raw)
    override fun matches(raw: String, hash: String): Boolean = encoder.matches(raw, hash)
}
