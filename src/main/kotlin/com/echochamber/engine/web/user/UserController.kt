package com.echochamber.engine.web.user

import com.echochamber.engine.application.LastActiveAdminException
import com.echochamber.engine.application.UserService
import com.echochamber.engine.domain.port.UserStore
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * ADMIN-only user management API (authorization enforced in `SecurityConfig` for the
 * `/api/users/` paths). Password hashes are never returned. Backs the console's user-management
 * screen (TICKET-020).
 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val users: UserStore,
) {

    @GetMapping
    suspend fun list(): List<UserDto> = users.findAll().map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@Valid @RequestBody dto: CreateUserDto): UserDto =
        userService.create(dto.username!!, dto.password!!, dto.role!!, null).toDto()

    @PostMapping("/{id}/disable")
    suspend fun disable(@PathVariable id: UUID): UserDto = userService.setEnabled(id, false).toDto()

    @PostMapping("/{id}/enable")
    suspend fun enable(@PathVariable id: UUID): UserDto = userService.setEnabled(id, true).toDto()

    @PostMapping("/{id}/role")
    suspend fun setRole(@PathVariable id: UUID, @Valid @RequestBody dto: RoleDto): UserDto =
        userService.setRole(id, dto.role!!).toDto()

    @PostMapping("/{id}/reset-password")
    suspend fun resetPassword(@PathVariable id: UUID, @Valid @RequestBody dto: PasswordDto): UserDto =
        userService.resetPassword(id, dto.password!!).toDto()

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(e: NoSuchElementException): Map<String, String?> = mapOf("error" to e.message)

    @ExceptionHandler(LastActiveAdminException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflict(e: LastActiveAdminException): Map<String, String?> = mapOf("error" to e.message)

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: IllegalArgumentException): Map<String, String?> = mapOf("error" to e.message)
}
