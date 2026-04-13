package com.qualidoc.application

import com.qualidoc.application.dto.UploadDocumentCommand
import com.qualidoc.application.usecase.UploadDocumentUseCase
import com.qualidoc.domain.model.*
import com.qualidoc.domain.port.SearchPort
import com.qualidoc.domain.port.StoragePort
import com.qualidoc.domain.repository.AuditLogRepository
import com.qualidoc.domain.repository.DocumentRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.util.UUID

class UploadDocumentUseCaseTest {

    private val documentRepository = mockk<DocumentRepository>()
    private val userRepository     = mockk<UserRepository>()
    private val storagePort        = mockk<StoragePort>()
    private val searchPort         = mockk<SearchPort>()
    private val auditLogRepository = mockk<AuditLogRepository>()

    private lateinit var useCase: UploadDocumentUseCase

    private val establishmentId = UUID.randomUUID()
    private val editorId        = UUID.randomUUID()
    private val readerId        = UUID.randomUUID()

    private val editor = User(
        id = editorId,
        establishmentId = establishmentId,
        email = "editor@qualidoc.fr",
        firstName = "Marie",
        lastName = "Dupont",
        role = UserRole.EDITOR
    )

    private val reader = User(
        id = readerId,
        establishmentId = establishmentId,
        email = "reader@qualidoc.fr",
        firstName = "Jean",
        lastName = "Martin",
        role = UserRole.READER
    )

    @BeforeEach
    fun setUp() {
        useCase = UploadDocumentUseCase(
            documentRepository, userRepository, storagePort, searchPort, auditLogRepository
        )
    }

    @Test
    fun `un éditeur peut uploader un document`() {
        val command = buildCommand(editorId)
        val savedDocument = buildDocument(editorId)

        every { userRepository.findById(editorId) } returns editor
        every { storagePort.store(any(), any(), any(), any(), any(), any()) } returns "$establishmentId/PROCEDURE/test.pdf"
        every { documentRepository.save(any()) } returns savedDocument
        every { searchPort.index(any(), any(), any(), any(), any()) } just Runs
        every { auditLogRepository.save(any()) } returns mockk()

        val result = useCase.execute(command)

        assertEquals("Procédure de test", result.title)
        assertEquals(DocumentType.PROCEDURE, result.type)
        verify(exactly = 1) { storagePort.store(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { searchPort.index(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { auditLogRepository.save(any()) }
    }

    @Test
    fun `un lecteur ne peut pas uploader un document`() {
        every { userRepository.findById(readerId) } returns reader

        val exception = assertThrows<IllegalStateException> {
            useCase.execute(buildCommand(readerId))
        }

        assertTrue(exception.message!!.contains("droit"))
        verify(exactly = 0) { storagePort.store(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { documentRepository.save(any()) }
    }

    @Test
    fun `une exception est levée si l'utilisateur est introuvable`() {
        every { userRepository.findById(any()) } returns null

        assertThrows<IllegalArgumentException> {
            useCase.execute(buildCommand(UUID.randomUUID()))
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildCommand(uploaderId: UUID) = UploadDocumentCommand(
        title = "Procédure de test",
        type = DocumentType.PROCEDURE,
        uploaderId = uploaderId,
        filename = "procedure.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1024L,
        contentStream = ByteArrayInputStream(ByteArray(0))
    )

    private fun buildDocument(uploaderId: UUID) = Document(
        title = "Procédure de test",
        type = DocumentType.PROCEDURE,
        uploaderId = uploaderId,
        establishmentId = establishmentId,
        storageKey = "$establishmentId/PROCEDURE/test.pdf",
        originalFilename = "procedure.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1024L
    )
}
