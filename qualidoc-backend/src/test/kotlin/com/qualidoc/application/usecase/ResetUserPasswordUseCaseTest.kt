package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.repository.RefreshTokenRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class ResetUserPasswordUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    private lateinit var useCase: ResetUserPasswordUseCase

    companion object {
        private const val NEW_PASSWORD = "N3wS3cur3P@ss!"
        private const val NEW_ENCODED_PASSWORD = "\$2a\$12\$newEncodedPasswordHash"
    }

    @BeforeEach
    fun setUp() {
        useCase = ResetUserPasswordUseCase(userRepository, refreshTokenRepository, passwordEncoder)
    }

    @Test
    fun `should_encode_new_password_and_save_user`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findById(user.id) } returns user
        every { passwordEncoder.encode(NEW_PASSWORD) } returns NEW_ENCODED_PASSWORD
        every { userRepository.save(any()) } answers { firstArg() }
        every { refreshTokenRepository.revokeAllForUser(user.id) } just Runs

        useCase.execute(user.id, NEW_PASSWORD)

        verify(exactly = 1) {
            userRepository.save(match { it.passwordHash == NEW_ENCODED_PASSWORD })
        }
    }

    @Test
    fun `should_revoke_all_refresh_tokens_after_password_reset`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findById(user.id) } returns user
        every { passwordEncoder.encode(NEW_PASSWORD) } returns NEW_ENCODED_PASSWORD
        every { userRepository.save(any()) } answers { firstArg() }
        every { refreshTokenRepository.revokeAllForUser(user.id) } just Runs

        useCase.execute(user.id, NEW_PASSWORD)

        verify(exactly = 1) { refreshTokenRepository.revokeAllForUser(user.id) }
    }

    @Test
    fun `should_throw_exception_when_user_does_not_exist`() {
        val unknownId = UUID.randomUUID()
        every { userRepository.findById(unknownId) } returns null

        assertThatThrownBy { useCase.execute(unknownId, NEW_PASSWORD) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("introuvable")
    }
}
