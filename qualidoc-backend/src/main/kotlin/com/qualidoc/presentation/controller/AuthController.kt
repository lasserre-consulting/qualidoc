package com.qualidoc.presentation.controller

import com.qualidoc.application.dto.AuthResponse
import com.qualidoc.application.dto.LoginRequest
import com.qualidoc.application.dto.RefreshRequest
import com.qualidoc.application.dto.UserDto
import com.qualidoc.application.usecase.AuthenticationException
import com.qualidoc.application.usecase.LoginUseCase
import com.qualidoc.application.usecase.LogoutUseCase
import com.qualidoc.application.usecase.RefreshTokenUseCase
import com.qualidoc.application.usecase.toDto
import com.qualidoc.domain.repository.UserRepository
import com.qualidoc.infrastructure.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Login, logout, refresh token")
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository
) {

    @PostMapping("/login")
    @Operation(summary = "Authentification par email/mot de passe")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = loginUseCase.execute(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du couple access/refresh token")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<AuthResponse> {
        val response = refreshTokenUseCase.execute(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Révocation de tous les refresh tokens")
    fun logout(@AuthenticationPrincipal user: AuthenticatedUser): ResponseEntity<Void> {
        logoutUseCase.execute(user.id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Informations de l'utilisateur connecté")
    fun me(@AuthenticationPrincipal user: AuthenticatedUser): ResponseEntity<UserDto> {
        val domainUser = userRepository.findById(user.id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(domainUser.toDto())
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthException(ex: AuthenticationException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "unauthorized", "message" to (ex.message ?: "Authentification échouée")))
}
