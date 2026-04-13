package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ListUsersUseCaseTest {

    private val userRepository = mockk<UserRepository>()

    private lateinit var useCase: ListUsersUseCase

    @BeforeEach
    fun setUp() {
        useCase = ListUsersUseCase(userRepository)
    }

    @Test
    fun `should_return_all_users_when_no_establishment_filter`() {
        val editor = TestFixtures.anEditor()
        val reader = TestFixtures.aReader()
        every { userRepository.findAll() } returns listOf(editor, reader)

        val result = useCase.execute()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.email }).containsExactlyInAnyOrder(editor.email, reader.email)
    }

    @Test
    fun `should_filter_users_by_establishment_when_provided`() {
        val editor = TestFixtures.anEditor()
        every { userRepository.findByEstablishmentId(TestFixtures.ESTABLISHMENT_ID) } returns listOf(editor)

        val result = useCase.execute(TestFixtures.ESTABLISHMENT_ID)

        assertThat(result).hasSize(1)
        assertThat(result[0].email).isEqualTo(editor.email)
        verify(exactly = 0) { userRepository.findAll() }
    }

    @Test
    fun `should_return_empty_list_when_no_users_found`() {
        every { userRepository.findAll() } returns emptyList()

        val result = useCase.execute()

        assertThat(result).isEmpty()
    }
}
