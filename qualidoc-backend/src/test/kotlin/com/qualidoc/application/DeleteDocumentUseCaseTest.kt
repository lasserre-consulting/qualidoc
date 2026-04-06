package com.qualidoc.application

import com.qualidoc.application.usecase.DeleteDocumentUseCase
import com.qualidoc.domain.model.*
import com.qualidoc.domain.port.SearchPort
import com.qualidoc.domain.port.StoragePort
import com.qualidoc.domain.repository.AuditLogRepository
import com.qualidoc.domain.repository.DocumentRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class DeleteDocumentUseCaseTest {

    private val documentRepository = mockk<DocumentRepository>()
    private val userRepository     = mockk<UserRepository>()
    private val storagePort        = mockk<StoragePort>()
    private val searchPort         = mockk<SearchPort>()
    private val auditLogRepository = mockk<AuditLogRepository>()

    private lateinit var useCase: DeleteDocumentUseCase

    private val establishmentId      = UUID.randomUUID()
    private val otherEstablishmentId = UUID.randomUUID()
    private val editorId             = UUID.randomUUID()
    private val documentId           = UUID.randomUUID()

    private val editor = User(
        id = editorId,
        establishmentId = establishmentId,
        email = "editor@qualidoc.fr",
        firstName = "Marie",
        lastName = "Dupont",
        role = UserRole.EDITOR
    )

    private val document = Document(
        id = documentId,
        title = "Protocole hygiène",
        type = DocumentType.PROCEDURE,
        uploaderId = editorId,
        establishmentId = establishmentId,
        storageKey = "$establishmentId/PROCEDURE/hygiene.pdf",
        originalFilename = "hygiene.pdf",
        mimeType = "application/pdf",
        sizeBytes = 2048L
    )

    @BeforeEach
    fun setUp() {
        useCase = DeleteDocumentUseCase(
            documentRepository, userRepository, storagePort, searchPort, auditLogRepository
        )
    }

    @Test
    fun `un éditeur du même établissement peut supprimer un document`() {
        every { documentRepository.findById(documentId) } returns document
        every { userRepository.findById(editorId) } returns editor
        every { storagePort.delete(any()) } just Runs
        every { searchPort.delete(any()) } just Runs
        every { documentRepository.delete(any()) } just Runs
        every { auditLogRepository.save(any()) } returns mockk()

        useCase.execute(documentId, editorId)

        verify(exactly = 1) { storagePort.delete(document.storageKey) }
        verify(exactly = 1) { searchPort.delete(documentId) }
        verify(exactly = 1) { documentRepository.delete(documentId) }
    }

    @Test
    fun `un éditeur d'un autre établissement ne peut pas supprimer le document`() {
        val editorFromOtherEstablishment = editor.copy(establishmentId = otherEstablishmentId)

        every { documentRepository.findById(documentId) } returns document
        every { userRepository.findById(editorId) } returns editorFromOtherEstablishment

        assertThrows<IllegalStateException> {
            useCase.execute(documentId, editorId)
        }

        verify(exactly = 0) { storagePort.delete(any()) }
        verify(exactly = 0) { documentRepository.delete(any()) }
    }

    @Test
    fun `une exception est levée si le document est introuvable`() {
        every { documentRepository.findById(any()) } returns null

        assertThrows<IllegalArgumentException> {
            useCase.execute(UUID.randomUUID(), editorId)
        }
    }

    @Test
    fun `une exception est levée si l'utilisateur est introuvable`() {
        every { documentRepository.findById(documentId) } returns document
        every { userRepository.findById(any()) } returns null

        assertThrows<IllegalArgumentException> {
            useCase.execute(documentId, UUID.randomUUID())
        }
    }
}
