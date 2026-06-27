package com.echochamber.engine.web.console

import com.echochamber.engine.application.AuditService
import com.echochamber.engine.application.ConsoleService
import com.echochamber.engine.application.ReplayJobScheduler
import com.echochamber.engine.application.ReplaySelection
import com.echochamber.engine.application.UserService
import com.echochamber.engine.domain.model.AuditAction
import com.echochamber.engine.domain.model.RequestOverride
import com.echochamber.engine.domain.model.UserRole
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import java.util.UUID

/**
 * Server-rendered admin console (TICKET-020). Read views + action forms. RBAC is enforced
 * in `SecurityConfig` by path; this controller depends only on application services.
 */
@Controller
class ConsoleController(
    private val console: ConsoleService,
    private val scheduler: ReplayJobScheduler,
    private val audit: AuditService,
    private val userService: UserService,
) {

    @GetMapping("/login")
    fun login() = "login"

    @GetMapping("/admin")
    fun home() = "redirect:/admin/requests"

    @GetMapping("/admin/requests")
    suspend fun requests(model: Model): String {
        model.addAttribute("requests", console.listRequests())
        return "requests"
    }

    @PostMapping("/admin/requests/{id}/retry")
    suspend fun retry(
        @PathVariable id: UUID,
        @RequestParam configId: UUID,
        @RequestParam(required = false) targetUrl: String?,
        @RequestParam(required = false) pathOverride: String?,
        @RequestParam(required = false) headersSet: String?,
        @RequestParam(required = false) bodyPatches: String?,
        principal: Principal?,
    ): String {
        val override = RequestOverride(
            targetUrl = targetUrl?.ifBlank { null },
            pathOverride = pathOverride?.ifBlank { null },
            headersSet = parsePairs(headersSet, ":"),
            bodyPatches = parsePairs(bodyPatches, "="),
        )
        val actor = principal?.name ?: "unknown"
        val job = scheduler.scheduleJob(
            configId = configId,
            selection = ReplaySelection(requestIds = listOf(id)),
            override = if (override.isEmpty) null else override,
            triggeredByUsername = actor,
        )
        audit.record(AuditAction.RETRY_TRIGGERED, actorUsername = actor, targetType = "ReplayJob", targetId = job.id.toString())
        return "redirect:/admin/history"
    }

    @GetMapping("/admin/history")
    fun history() = "history"

    @GetMapping("/admin/users")
    suspend fun users(model: Model): String {
        model.addAttribute("users", userService.listUsers())
        model.addAttribute("roles", UserRole.entries)
        return "users"
    }

    @PostMapping("/admin/users")
    suspend fun createUser(
        @RequestParam username: String,
        @RequestParam password: String,
        @RequestParam role: UserRole,
        principal: Principal?,
    ): String {
        userService.create(username, password, role, null)
        audit.record(AuditAction.USER_CREATED, actorUsername = principal?.name ?: "unknown", targetType = "User", targetId = username)
        return "redirect:/admin/users"
    }

    @PostMapping("/admin/users/{id}/disable")
    suspend fun disableUser(@PathVariable id: UUID, principal: Principal?): String {
        userService.setEnabled(id, false)
        audit.record(AuditAction.USER_DISABLED, actorUsername = principal?.name ?: "unknown", targetType = "User", targetId = id.toString())
        return "redirect:/admin/users"
    }

    @PostMapping("/admin/users/{id}/enable")
    suspend fun enableUser(@PathVariable id: UUID): String {
        userService.setEnabled(id, true)
        return "redirect:/admin/users"
    }

    @GetMapping("/admin/audit")
    fun auditView() = "audit"

    @GetMapping("/admin/password")
    fun passwordForm() = "password"

    @PostMapping("/admin/password")
    suspend fun changePassword(@RequestParam newPassword: String, principal: Principal?): String {
        userService.changeOwnPassword(principal?.name ?: error("no principal"), newPassword)
        return "redirect:/admin/requests"
    }

    private fun parsePairs(text: String?, sep: String): Map<String, String> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.lines()
            .mapNotNull { line ->
                val i = line.indexOf(sep)
                if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + sep.length).trim()
            }
            .filter { it.first.isNotEmpty() }
            .toMap()
    }
}
