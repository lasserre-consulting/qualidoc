package com.qualidoc.infrastructure.config

import io.minio.MinioClient
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
                    .description("Token JWT — obtenu via POST /api/v1/auth/login")
            )
        )
}

