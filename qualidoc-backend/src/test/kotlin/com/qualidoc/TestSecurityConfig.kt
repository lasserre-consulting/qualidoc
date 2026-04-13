package com.qualidoc

import com.qualidoc.infrastructure.security.JwtService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    fun testJwtService(): JwtService = JwtService(
        secret = "test-secret-for-unit-tests-only-min-32-chars",
        accessTokenExpiration = 900,
        refreshTokenExpiration = 604800
    )
}
