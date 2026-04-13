package com.qualidoc.presentation.controller

import com.qualidoc.application.dto.CreateUserRequest
import com.qualidoc.application.dto.ResetPasswordRequest
import com.qualidoc.application.dto.UpdateUserRequest
import com.qualidoc.application.dto.UserDto
import com.qualidoc.application.usecase.CreateUserUseCase
import com.qualidoc.application.usecase.DeleteUserUseCase
import com.qualidoc.application.usecase.ListUsersUseCase
import com.qualidoc.application.usecase.ResetUserPasswordUseCase
import com.qualidoc.application.usecase.UpdateUserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin - Utilisateurs", description = "CRUD utilisateurs (éditeurs uniquement)")
@SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val listUsersUseCase: ListUsersUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val resetUserPasswordUseCase: ResetUserPasswordUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    @GetMapping
    @Operation(summary = "Liste tous les utilisateurs")
    fun listUsers(
        @RequestParam(required = false) establishmentId: UUID?
    ): ResponseEntity<List<UserDto>> =
        ResponseEntity.ok(listUsersUseCase.execute(establishmentId))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un nouvel utilisateur")
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<UserDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(createUserUseCase.execute(request))

    @PatchMapping("/{id}")
    @Operation(summary = "Modifie un utilisateur")
    fun updateUser(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserDto> =
        ResponseEntity.ok(updateUserUseCase.execute(id, request))

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Réinitialise le mot de passe d'un utilisateur")
    fun resetPassword(
        @PathVariable id: UUID,
        @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<Void> {
        resetUserPasswordUseCase.execute(id, request.newPassword)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un utilisateur")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        deleteUserUseCase.execute(id)
        return ResponseEntity.noContent().build()
    }
}
