package com.qualidoc.application

import com.qualidoc.application.dto.SearchDocumentQuery
import com.qualidoc.application.usecase.SearchDocumentUseCase
import com.qualidoc.domain.model.*
import com.qualidoc.domain.port.DocumentSearchResult
import com.qualidoc.domain.port.SearchPort
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class SearchDocumentUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val searchPort     = mockk<SearchPort>()

    private lateinit var useCase: SearchDocumentUseCase

    private val userId          = UUID.randomUUID()
    private val establishmentId = UUID.randomUUID()

    private val user = User(
        id = userId,
        establishmentId = establishmentId,
        email = "user@qualidoc.fr",
        firstName = "Alice",
        lastName = "Bernard",
        role = UserRole.READER
    )

    @BeforeEach
    fun setUp() {
        useCase = SearchDocumentUseCase(userRepository, searchPort)
    }

    @Test
    fun `les résultats sont correctement mappés en SearchResultDto`() {
        val docId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns user
        every { searchPort.search("stérilisation", listOf(establishmentId)) } returns listOf(
            DocumentSearchResult(
                documentId = docId,
                title = "Protocole de stérilisation",
                type = "PROCEDURE",
                establishmentId = establishmentId,
                snippet = "…stérilisation autoclave…",
                score = 1.5f
            )
        )

        val results = useCase.execute(SearchDocumentQuery("stérilisation", userId))

        assertEquals(1, results.size)
        assertEquals(docId, results[0].documentId)
        assertEquals("Protocole de stérilisation", results[0].title)
        assertEquals("PROCEDURE", results[0].type)
        assertEquals("…stérilisation autoclave…", results[0].snippet)
    }

    @Test
    fun `la recherche est filtrée par l'établissement de l'utilisateur`() {
        every { userRepository.findById(userId) } returns user
        every { searchPort.search(any(), listOf(establishmentId)) } returns emptyList()

        useCase.execute(SearchDocumentQuery("hygiène", userId))

        // Vérifie que seul l'établissement de l'utilisateur est passé au port
        verify(exactly = 1) { searchPort.search("hygiène", listOf(establishmentId)) }
    }

    @Test
    fun `une liste vide est retournée si aucun résultat`() {
        every { userRepository.findById(userId) } returns user
        every { searchPort.search(any(), any()) } returns emptyList()

        val results = useCase.execute(SearchDocumentQuery("inexistant", userId))

        assertTrue(results.isEmpty())
    }

    @Test
    fun `une exception est levée si l'utilisateur est introuvable`() {
        every { userRepository.findById(any()) } returns null

        assertThrows<IllegalArgumentException> {
            useCase.execute(SearchDocumentQuery("test", UUID.randomUUID()))
        }

        verify(exactly = 0) { searchPort.search(any(), any()) }
    }
}
