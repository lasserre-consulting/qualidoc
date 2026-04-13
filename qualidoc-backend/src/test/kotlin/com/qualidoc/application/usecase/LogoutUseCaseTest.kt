package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.model.RefreshToken
import com.qualidoc.domain.repository.RefreshTokenRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LogoutUseCaseTest {

    private val refreshTokenRepository = mockk<RefreshTokenRepository>()

    private lateinit var useCase: LogoutUseCase

    @BeforeEach
    fun setUp() {
        useCase = LogoutUseCase(refreshTokenRepository)
    }

    @Test
    fun `should_revoke_all_refresh_tokens_when_valid_token_provided`() {
        val rawToken = "valid-refresh-token"
        val storedToken = RefreshToken(
            userId = TestFixtures.EDITOR_ID,
            tokenHash = "doesnt-matter",
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        every { refreshTokenRepository.findByTokenHash(any()) } returns storedToken
        every { refreshTokenRepository.revokeAllForUser(TestFixtures.EDITOR_ID) } just Runs

        useCase.execute(rawToken)

        verify(exactly = 1) { refreshTokenRepository.revokeAllForUser(TestFixtures.EDITOR_ID) }
    }

    @Test
    fun `should_do_nothing_when_token_is_null`() {
        useCase.execute(null)

        verify(exactly = 0) { refreshTokenRepository.findByTokenHash(any()) }
        verify(exactly = 0) { refreshTokenRepository.revokeAllForUser(any()) }
    }

    @Test
    fun `should_do_nothing_when_token_not_found_in_db`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns null

        useCase.execute("unknown-token")

        verify(exactly = 0) { refreshTokenRepository.revokeAllForUser(any()) }
    }
}
