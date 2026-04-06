package com.qualidoc.infrastructure.config

import io.minio.MinioClient
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// ── MinIO ─────────────────────────────────────────────────────────────────────

@Configuration
class MinioConfig(
    @param:Value("\${minio.endpoint}") private val endpoint: String,
    @param:Value("\${minio.access-key}") private val accessKey: String,
    @param:Value("\${minio.secret-key}") private val secretKey: String
) {
    @Bean
    fun minioClient(): MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()
}

// ── OpenAPI / Swagger ─────────────────────────────────────────────────────────

@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("QualiDoc API")
                .description("API REST de gestion documentaire inter-établissements — Service Qualité")
                .version("1.0.0")
        )
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT Keycloak — realm : qualidoc")
            )
        )
}

// ── Gestion globale des erreurs ───────────────────────────────────────────────

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotFound(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Ressource introuvable")

    @ExceptionHandler(IllegalStateException::class)
    fun handleForbidden(ex: IllegalStateException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.message ?: "Action non autorisée")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Requête invalide"
        )

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur inattendue est survenue"
        ).also {
            it.setProperty("debug", ex.message)
        }
    }
}
