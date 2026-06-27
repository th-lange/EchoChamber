package com.echochamber.engine.web.security

import com.echochamber.engine.domain.port.UserStore
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.stereotype.Service

/**
 * Bridges the [UserStore] to Spring Security. The store is suspend-based but the
 * [UserDetailsService] SPI is blocking and called on a request thread, so `runBlocking`
 * is appropriate here.
 */
@Service
class DbUserDetailsService(
    private val users: UserStore,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = runBlocking { users.findByUsername(username) }
            ?: throw UsernameNotFoundException("Unknown user: $username")
        return SpringUser.builder()
            .username(user.username)
            .password(user.passwordHash)
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .disabled(!user.enabled)
            .build()
    }
}
