package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.application.dto.UpdateUserRequest
import com.qualidoc.domain.model.UserRole
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UpdateUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()

    private lateinit var useCase: UpdateUserUseCase

    @BeforeEach
    fun setUp() {
        useCase = UpdateUserUseCase(userRepository)
    }

    @Test
    fun `should_update_only_provided_fields`() {
        val user = TestFixtures.aReader()
        val request = UpdateUserRequest(firstName = "NouveauPrenom")
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }

        val result = useCase.execute(user.id, request)

        assertThat(result.firstName).isEqualTo("NouveauPrenom")
        assertThat(result.lastName).isEqualTo(user.lastName)
        assertThat(result.role).isEqualTo(user.role)
    }

    @Test
    fun `should_update_role_when_provided`() {
        val user = TestFixtures.aReader()
        val request = UpdateUserRequest(role = UserRole.EDITOR)
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }

        val result = useCase.execute(user.id, request)

        assertThat(result.role).isEqualTo(UserRole.EDITOR)
    }

    @Test
    fun `should_update_active_status_when_provided`() {
        val user = TestFixtures.anEditor()
        val request = UpdateUserRequest(active = false)
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }

        useCase.execute(user.id, request)

        verify(exactly = 1) { userRepository.save(match { !it.active }) }
    }

    @Test
    fun `should_keep_all_fields_unchanged_when_request_is_empty`() {
        val user = TestFixtures.anEditor()
        val request = UpdateUserRequest()
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }

        val result = useCase.execute(user.id, request)

        assertThat(result.firstName).isEqualTo(user.firstName)
        assertThat(result.lastName).isEqualTo(user.lastName)
        assertThat(result.role).isEqualTo(user.role)
    }

    @Test
    fun `should_throw_exception_when_user_does_not_exist`() {
        val unknownId = UUID.randomUUID()
        every { userRepository.findById(unknownId) } returns null

        assertThatThrownBy { useCase.execute(unknownId, UpdateUserRequest(firstName = "Test")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("introuvable")
    }
}
