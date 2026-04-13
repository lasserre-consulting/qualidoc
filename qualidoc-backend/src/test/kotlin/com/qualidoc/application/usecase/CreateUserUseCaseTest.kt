package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.application.dto.CreateUserRequest
import com.qualidoc.domain.model.UserRole
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class CreateUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    private lateinit var useCase: CreateUserUseCase

    companion object {
        private const val ENCODED_PASSWORD = "\$2a\$12\$encodedPasswordHash"
    }

    @BeforeEach
    fun setUp() {
        useCase = CreateUserUseCase(userRepository, passwordEncoder)
    }

    @Test
    fun `should_create_user_with_encoded_password`() {
        val request = CreateUserRequest(
            email = "new-user@qualidoc.fr",
            firstName = "Pierre",
            lastName = "Durand",
            role = UserRole.READER,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            password = TestFixtures.DEFAULT_PASSWORD
        )
        every { userRepository.findByEmail(request.email) } returns null
        every { passwordEncoder.encode(TestFixtures.DEFAULT_PASSWORD) } returns ENCODED_PASSWORD
        every { userRepository.save(any()) } answers { firstArg() }

        val result = useCase.execute(request)

        assertThat(result.email).isEqualTo("new-user@qualidoc.fr")
        assertThat(result.firstName).isEqualTo("Pierre")
        assertThat(result.lastName).isEqualTo("Durand")
        assertThat(result.role).isEqualTo(UserRole.READER)
        assertThat(result.establishmentId).isEqualTo(TestFixtures.ESTABLISHMENT_ID)
    }

    @Test
    fun `should_save_user_with_bcrypt_hashed_password`() {
        val request = CreateUserRequest(
            email = "new-user@qualidoc.fr",
            firstName = "Pierre",
            lastName = "Durand",
            role = UserRole.EDITOR,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            password = TestFixtures.DEFAULT_PASSWORD
        )
        every { userRepository.findByEmail(request.email) } returns null
        every { passwordEncoder.encode(TestFixtures.DEFAULT_PASSWORD) } returns ENCODED_PASSWORD
        every { userRepository.save(any()) } answers { firstArg() }

        useCase.execute(request)

        verify(exactly = 1) {
            userRepository.save(match { it.passwordHash == ENCODED_PASSWORD })
        }
    }

    @Test
    fun `should_throw_exception_when_email_already_exists`() {
        val existingUser = TestFixtures.anEditor()
        val request = CreateUserRequest(
            email = existingUser.email,
            firstName = "Autre",
            lastName = "Personne",
            role = UserRole.READER,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            password = TestFixtures.DEFAULT_PASSWORD
        )
        every { userRepository.findByEmail(existingUser.email) } returns existingUser

        assertThatThrownBy { useCase.execute(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("existe")

        verify(exactly = 0) { userRepository.save(any()) }
    }
}
