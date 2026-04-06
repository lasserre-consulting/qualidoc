package com.qualidoc.application

import com.qualidoc.application.usecase.GetEstablishmentDocumentsUseCase
import com.qualidoc.domain.model.*
import com.qualidoc.domain.repository.DocumentRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class GetEstablishmentDocumentsUseCaseTest {

    private val documentRepository = mockk<DocumentRepository>()
    private val userRepository     = mockk<UserRepository>()

    private lateinit var useCase: GetEstablishmentDocumentsUseCase

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
        useCase = GetEstablishmentDocumentsUseCase(documentRepository, userRepository)
    }

    @Test
    fun `retourne uniquement les documents du groupement de l'utilisateur`() {
        val doc1 = buildDocument("Procédure hygiène", Instant.now().minusSeconds(60))
        val doc2 = buildDocument("Formulaire qualité", Instant.now().minusSeconds(30))

        every { userRepository.findById(userId) } returns user
        every { documentRepository.findByEstablishmentId(establishmentId) } returns listOf(doc1, doc2)

        val results = useCase.execute(userId)

        assertEquals(2, results.size)
        assertTrue(results.all { it.establishmentId == establishmentId })
        verify(exactly = 1) { documentRepository.findByEstablishmentId(establishmentId) }
    }

    @Test
    fun `les résultats sont triés du plus récent au plus ancien`() {
        val older = buildDocument("Ancien", Instant.now().minusSeconds(120))
        val newer = buildDocument("Récent", Instant.now().minusSeconds(10))

        every { userRepository.findById(userId) } returns user
        every { documentRepository.findByEstablishmentId(establishmentId) } returns listOf(older, newer)

        val results = useCase.execute(userId)

        assertEquals("Récent", results[0].title)
        assertEquals("Ancien", results[1].title)
    }

    @Test
    fun `une liste vide est retournée si aucun document`() {
        every { userRepository.findById(userId) } returns user
        every { documentRepository.findByEstablishmentId(establishmentId) } returns emptyList()

        val results = useCase.execute(userId)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `une exception est levée si l'utilisateur est introuvable`() {
        every { userRepository.findById(any()) } returns null

        assertThrows<IllegalArgumentException> {
            useCase.execute(UUID.randomUUID())
        }

        verify(exactly = 0) { documentRepository.findByEstablishmentId(any()) }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildDocument(title: String, createdAt: Instant) = Document(
        title = title,
        type = DocumentType.PROCEDURE,
        uploaderId = UUID.randomUUID(),
        establishmentId = establishmentId,
        storageKey = "$establishmentId/PROCEDURE/${UUID.randomUUID()}.pdf",
        originalFilename = "doc.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1024L,
        createdAt = createdAt
    )
}
