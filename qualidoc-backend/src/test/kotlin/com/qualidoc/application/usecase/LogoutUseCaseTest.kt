package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.repository.RefreshTokenRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    private val refreshTokenRepository = mockk<RefreshTokenRepository>()

    private lateinit var useCase: LogoutUseCase

    @BeforeEach
    fun setUp() {
        useCase = LogoutUseCase(refreshTokenRepository)
    }

    @Test
    fun `should_revoke_all_refresh_tokens_for_user`() {
        val userId = TestFixtures.EDITOR_ID
        every { refreshTokenRepository.revokeAllForUser(userId) } just Runs

        useCase.execute(userId)

        verify(exactly = 1) { refreshTokenRepository.revokeAllForUser(userId) }
    }

    @Test
    fun `should_succeed_even_if_user_has_no_tokens`() {
        val userId = TestFixtures.READER_ID
        every { refreshTokenRepository.revokeAllForUser(userId) } just Runs

        useCase.execute(userId)

        verify(exactly = 1) { refreshTokenRepository.revokeAllForUser(userId) }
    }
}
