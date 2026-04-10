package com.qualidoc.infrastructure

import com.qualidoc.AbstractIntegrationTest
import com.qualidoc.domain.model.Document
import com.qualidoc.domain.model.DocumentType
import com.qualidoc.domain.repository.DocumentRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// UUIDs issus du seed V2 — établissements et utilisateurs déjà présents en base
private val CHU_TOULOUSE     = UUID.fromString("11111111-0000-0000-0000-000000000001")
private val CLINIQUE_ST_JEAN = UUID.fromString("11111111-0000-0000-0000-000000000002")
private val EDITOR_MARIE     = UUID.fromString("22222222-0000-0000-0000-000000000001")
private val EDITOR_SOPHIE    = UUID.fromString("22222222-0000-0000-0000-000000000003")

@Transactional
class DocumentRepositoryAdapterIT(
    @param:Autowired private val documentRepository: DocumentRepository
) : AbstractIntegrationTest() {

    @Test
    fun `save et findByEstablishmentId retournent le document persisté`() {
        val doc = documentRepository.save(buildDocument(uploaderId = EDITOR_MARIE, establishmentId = CHU_TOULOUSE))

        val found = documentRepository.findByEstablishmentId(CHU_TOULOUSE)

        assertTrue(found.any { it.id == doc.id })
        assertEquals("Procédure hygiène", found.first { it.id == doc.id }.title)
    }

    @Test
    fun `findById retourne null pour un id inconnu`() {
        assertNull(documentRepository.findById(UUID.randomUUID()))
    }

    @Test
    fun `findByEstablishmentId ne retourne pas les documents d'un autre groupement`() {
        documentRepository.save(buildDocument(uploaderId = EDITOR_SOPHIE, establishmentId = CLINIQUE_ST_JEAN))

        val chuDocs = documentRepository.findByEstablishmentId(CHU_TOULOUSE)
        assertTrue(chuDocs.none { it.establishmentId == CLINIQUE_ST_JEAN })
    }

    private fun buildDocument(uploaderId: UUID, establishmentId: UUID) = Document(
        title = "Procédure hygiène",
        type = DocumentType.PROCEDURE,
        uploaderId = uploaderId,
        establishmentId = establishmentId,
        storageKey = "$establishmentId/PROCEDURE/test-${UUID.randomUUID()}.pdf",
        originalFilename = "hygiene.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1024L
    )
}
