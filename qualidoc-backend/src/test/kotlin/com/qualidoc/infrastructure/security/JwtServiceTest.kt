package com.qualidoc.infrastructure.security

import com.qualidoc.TestFixtures
import com.qualidoc.domain.model.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class JwtServiceTest {

    companion object {
        private const val TEST_SECRET = "test-secret-for-unit-tests-only-min-32-chars"
        private const val ACCESS_TOKEN_EXPIRATION = 900L
        private const val REFRESH_TOKEN_EXPIRATION = 604800L
    }

    private val jwtService = JwtService(
        secret = TEST_SECRET,
        accessTokenExpiration = ACCESS_TOKEN_EXPIRATION,
        refreshTokenExpiration = REFRESH_TOKEN_EXPIRATION
    )

    // ── generateAccessToken ─────────────────────────────────────────────────

    @Test
    fun `should_include_correct_subject_claim_when_generating_access_token`() {
        val user = TestFixtures.anEditor()

        val token = jwtService.generateAccessToken(user)
        val claims = jwtService.parseAccessToken(token)

        assertThat(claims.subject).isEqualTo(user.id.toString())
    }

    @Test
    fun `should_include_role_claim_when_generating_access_token`() {
        val user = TestFixtures.anEditor()

        val token = jwtService.generateAccessToken(user)
        val claims = jwtService.parseAccessToken(token)

        assertThat(claims["role"]).isEqualTo(UserRole.EDITOR.name)
    }

    @Test
    fun `should_include_establishment_id_claim_when_generating_access_token`() {
        val user = TestFixtures.anEditor()

        val token = jwtService.generateAccessToken(user)
        val claims = jwtService.parseAccessToken(token)

        assertThat(claims["establishment_id"]).isEqualTo(user.establishmentId.toString())
    }

    @Test
    fun `should_set_expiration_based_on_configured_duration`() {
        val user = TestFixtures.anEditor()
        val beforeGeneration = Date()

        val token = jwtService.generateAccessToken(user)
        val claims = jwtService.parseAccessToken(token)

        val expectedExpiration = Date(beforeGeneration.time + ACCESS_TOKEN_EXPIRATION * 1000)
        // Tolerance de 2 secondes pour le temps d'execution
        assertThat(claims.expiration).isCloseTo(expectedExpiration, 2000L)
    }

    // ── parseAccessToken ────────────────────────────────────────────────────

    @Test
    fun `should_parse_valid_token_and_return_correct_claims`() {
        val user = TestFixtures.aReader()
        val token = jwtService.generateAccessToken(user)

        val claims = jwtService.parseAccessToken(token)

        assertThat(claims.subject).isEqualTo(user.id.toString())
        assertThat(claims["role"]).isEqualTo(UserRole.READER.name)
    }

    @Test
    fun `should_throw_exception_when_token_is_expired`() {
        val expiredJwtService = JwtService(
            secret = TEST_SECRET,
            accessTokenExpiration = -1, // expiration negative = deja expire
            refreshTokenExpiration = REFRESH_TOKEN_EXPIRATION
        )
        val user = TestFixtures.anEditor()
        val token = expiredJwtService.generateAccessToken(user)

        assertThatThrownBy { jwtService.parseAccessToken(token) }
            .isInstanceOf(io.jsonwebtoken.ExpiredJwtException::class.java)
    }

    @Test
    fun `should_throw_exception_when_token_has_invalid_signature`() {
        val differentSecret = "a-completely-different-secret-at-least-32-chars-long"
        val otherJwtService = JwtService(
            secret = differentSecret,
            accessTokenExpiration = ACCESS_TOKEN_EXPIRATION,
            refreshTokenExpiration = REFRESH_TOKEN_EXPIRATION
        )
        val token = otherJwtService.generateAccessToken(TestFixtures.anEditor())

        assertThatThrownBy { jwtService.parseAccessToken(token) }
            .isInstanceOf(io.jsonwebtoken.JwtException::class.java)
    }

    // ── isValid ─────────────────────────────────────────────────────────────

    @Test
    fun `should_return_true_for_valid_token`() {
        val token = jwtService.generateAccessToken(TestFixtures.anEditor())

        assertThat(jwtService.isValid(token)).isTrue()
    }

    @Test
    fun `should_return_false_for_malformed_token`() {
        assertThat(jwtService.isValid("not.a.valid.jwt.token")).isFalse()
    }

    @Test
    fun `should_return_false_for_empty_token`() {
        assertThat(jwtService.isValid("")).isFalse()
    }

    @Test
    fun `should_return_false_for_expired_token`() {
        val expiredJwtService = JwtService(
            secret = TEST_SECRET,
            accessTokenExpiration = -1,
            refreshTokenExpiration = REFRESH_TOKEN_EXPIRATION
        )
        val token = expiredJwtService.generateAccessToken(TestFixtures.anEditor())

        assertThat(jwtService.isValid(token)).isFalse()
    }

    @Test
    fun `should_return_false_for_token_signed_with_different_key`() {
        val otherKey = Keys.hmacShaKeyFor("another-secret-key-that-is-at-least-32-chars".toByteArray())
        val token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .signWith(otherKey)
            .compact()

        assertThat(jwtService.isValid(token)).isFalse()
    }

    // ── generateRefreshToken ────────────────────────────────────────────────

    @Test
    fun `should_generate_unique_refresh_tokens`() {
        val token1 = jwtService.generateRefreshToken()
        val token2 = jwtService.generateRefreshToken()

        assertThat(token1).isNotEqualTo(token2)
    }

    @Test
    fun `should_generate_refresh_token_in_uuid_format`() {
        val token = jwtService.generateRefreshToken()

        assertThat(UUID.fromString(token)).isNotNull()
    }

    // ── refreshTokenExpirationSeconds ───────────────────────────────────────

    @Test
    fun `should_return_configured_refresh_token_expiration`() {
        assertThat(jwtService.refreshTokenExpirationSeconds()).isEqualTo(REFRESH_TOKEN_EXPIRATION)
    }
}
