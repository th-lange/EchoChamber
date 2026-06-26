package com.echochamber.engine.application

import com.echochamber.engine.domain.model.User
import com.echochamber.engine.domain.model.UserRole
import com.echochamber.engine.domain.port.PasswordHasher
import com.echochamber.engine.domain.port.UserStore
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Creates the single default ADMIN from env on first boot when no users exist
 * (TICKET-019). Credentials come only from `ADMIN_BOOTSTRAP_USER`/`ADMIN_BOOTSTRAP_PASSWORD`
 * — never hardcoded. The account is created with `mustChangePassword = true`. It can later
 * be deactivated, subject to the last-active-admin guard.
 */
@Component
class BootstrapAdminInitializer(
    private val users: UserStore,
    private val hasher: PasswordHasher,
    @Value("\${ADMIN_BOOTSTRAP_USER:}") private val bootstrapUser: String,
    @Value("\${ADMIN_BOOTSTRAP_PASSWORD:}") private val bootstrapPassword: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(BootstrapAdminInitializer::class.java)

    override fun run(args: ApplicationArguments) = runBlocking {
        if (users.findAll().isNotEmpty()) return@runBlocking
        if (bootstrapUser.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No users exist and ADMIN_BOOTSTRAP_USER/PASSWORD are not set — skipping admin bootstrap")
            return@runBlocking
        }
        val now = Instant.now()
        users.save(
            User(
                id = UUID.randomUUID(),
                username = bootstrapUser,
                passwordHash = hasher.hash(bootstrapPassword),
                role = UserRole.ADMIN,
                enabled = true,
                mustChangePassword = true,
                createdAt = now,
                updatedAt = now,
                createdBy = null,
            ),
        )
        log.info("Bootstrapped initial ADMIN account '{}'", bootstrapUser)
    }
}
