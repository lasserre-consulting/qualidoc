package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class GetCurrentUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()

    private lateinit var useCase: GetCurrentUserUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetCurrentUserUseCase(userRepository)
    }

    @Test
    fun `should_return_user_dto_for_existing_user`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findById(user.id) } returns user

        val result = useCase.execute(user.id)

        assertThat(result.id).isEqualTo(user.id)
        assertThat(result.email).isEqualTo(user.email)
        assertThat(result.firstName).isEqualTo(user.firstName)
        assertThat(result.lastName).isEqualTo(user.lastName)
        assertThat(result.role).isEqualTo(user.role)
        assertThat(result.establishmentId).isEqualTo(user.establishmentId)
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
