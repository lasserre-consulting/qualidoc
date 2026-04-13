package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.repository.RefreshTokenRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()

    private lateinit var useCase: DeleteUserUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteUserUseCase(userRepository, refreshTokenRepository)
    }

    @Test
    fun `should_revoke_tokens_and_delete_user`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findById(user.id) } returns user
        every { refreshTokenRepository.revokeAllForUser(user.id) } just Runs
        every { userRepository.deleteById(user.id) } just Runs

        useCase.execute(user.id)

        verifyOrder {
            refreshTokenRepository.revokeAllForUser(user.id)
            userRepository.deleteById(user.id)
        }
    }

    @Test
    fun `should_throw_exception_when_user_does_not_exist`() {
        val unknownId = UUID.randomUUID()
        every { userRepository.findById(unknownId) } returns null

        assertThatThrownBy { useCase.execute(unknownId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("introuvable")
    }
}
