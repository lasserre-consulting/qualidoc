package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.application.dto.RefreshRequest
import com.qualidoc.domain.model.RefreshToken
import com.qualidoc.domain.port.JwtPort
import com.qualidoc.domain.repository.RefreshTokenRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.time.LocalDateTime

class RefreshTokenUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val jwtPort = mockk<JwtPort>()

    private lateinit var useCase: RefreshTokenUseCase

    companion object {
        private const val RAW_REFRESH_TOKEN = "raw-refresh-token-value"
        private const val NEW_ACCESS_TOKEN = "new-access-token"
        private const val NEW_REFRESH_TOKEN = "new-refresh-token"
        private const val REFRESH_EXPIRATION = 604800L

        private fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }

    @BeforeEach
    fun setUp() {
        useCase = RefreshTokenUseCase(userRepository, refreshTokenRepository, jwtPort)
    }

    @Test
    fun `should_return_new_tokens_when_refresh_token_is_valid`() {
        val user = TestFixtures.anEditor()
        val storedToken = aStoredRefreshToken(userId = user.id)
        every { refreshTokenRepository.findByTokenHash(sha256(RAW_REFRESH_TOKEN)) } returns storedToken
        every { refreshTokenRepository.revokeAllForUser(user.id) } just Runs
        every { userRepository.findById(user.id) } returns user
        every { jwtPort.generateAccessToken(user) } returns NEW_ACCESS_TOKEN
        every { jwtPort.generateRefreshToken() } returns NEW_REFRESH_TOKEN
        every { jwtPort.refreshTokenExpirationSeconds() } returns REFRESH_EXPIRATION
        every { refreshTokenRepository.save(any()) } returns mockk()

        val result = useCase.execute(RefreshRequest(RAW_REFRESH_TOKEN))

        assertThat(result.accessToken).isEqualTo(NEW_ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(NEW_REFRESH_TOKEN)
        assertThat(result.user.id).isEqualTo(user.id)
    }

    @Test
    fun `should_revoke_old_tokens_and_save_new_one_on_rotation`() {
        val user = TestFixtures.anEditor()
        val storedToken = aStoredRefreshToken(userId = user.id)
        every { refreshTokenRepository.findByTokenHash(sha256(RAW_REFRESH_TOKEN)) } returns storedToken
        every { refreshTokenRepository.revokeAllForUser(user.id) } just Runs
        every { userRepository.findById(user.id) } returns user
        every { jwtPort.generateAccessToken(user) } returns NEW_ACCESS_TOKEN
        every { jwtPort.generateRefreshToken() } returns NEW_REFRESH_TOKEN
        every { jwtPort.refreshTokenExpirationSeconds() } returns REFRESH_EXPIRATION
        every { refreshTokenRepository.save(any()) } returns mockk()

        useCase.execute(RefreshRequest(RAW_REFRESH_TOKEN))

        verifyOrder {
            refreshTokenRepository.revokeAllForUser(user.id)
            refreshTokenRepository.save(match { it.userId == user.id })
        }
    }

    @Test
    fun `should_throw_exception_when_refresh_token_is_unknown`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns null

        assertThatThrownBy { useCase.execute(RefreshRequest("unknown-token")) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("invalide")
    }

    @Test
    fun `should_throw_exception_when_refresh_token_is_revoked`() {
        val revokedToken = aStoredRefreshToken(revoked = true)
        every { refreshTokenRepository.findByTokenHash(any()) } returns revokedToken

        assertThatThrownBy { useCase.execute(RefreshRequest(RAW_REFRESH_TOKEN)) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("voqu")
    }

    @Test
    fun `should_throw_exception_when_refresh_token_is_expired`() {
        val expiredToken = aStoredRefreshToken(expiresAt = LocalDateTime.now().minusDays(1))
        every { refreshTokenRepository.findByTokenHash(any()) } returns expiredToken

        assertThatThrownBy { useCase.execute(RefreshRequest(RAW_REFRESH_TOKEN)) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("expir")
    }

    @Test
    fun `should_throw_exception_when_user_is_inactive`() {
        val inactiveUser = TestFixtures.anEditor(active = false)
        val storedToken = aStoredRefreshToken(userId = inactiveUser.id)
        every { refreshTokenRepository.findByTokenHash(sha256(RAW_REFRESH_TOKEN)) } returns storedToken
        every { refreshTokenRepository.revokeAllForUser(inactiveUser.id) } just Runs
        every { userRepository.findById(inactiveUser.id) } returns inactiveUser

        assertThatThrownBy { useCase.execute(RefreshRequest(RAW_REFRESH_TOKEN)) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("sactiv")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun aStoredRefreshToken(
        userId: java.util.UUID = TestFixtures.EDITOR_ID,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        revoked: Boolean = false
    ) = RefreshToken(
        userId = userId,
        tokenHash = sha256(RAW_REFRESH_TOKEN),
        expiresAt = expiresAt,
        revoked = revoked
    )
}
