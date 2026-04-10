package com.qualidoc

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * Fournit un JwtDecoder no-op pour les tests d'intégration.
 * Les ITs appellent les use cases directement — le JWT n'est jamais décodé.
 */
@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    fun jwtDecoder(): JwtDecoder = JwtDecoder { _ ->
        throw UnsupportedOperationException("JWT decoding non supporté en tests d'intégration")
    }
}
