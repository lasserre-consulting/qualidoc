package com.qualidoc.application

import com.qualidoc.AbstractIntegrationTest
import com.qualidoc.application.dto.UploadDocumentCommand
import com.qualidoc.application.usecase.UploadDocumentUseCase
import com.qualidoc.domain.model.DocumentType
import com.qualidoc.domain.port.SearchPort
import com.qualidoc.domain.repository.DocumentRepository
import com.qualidoc.infrastructure.search.DocumentIndex
import com.qualidoc.infrastructure.search.DocumentIndexRepository
import com.qualidoc.infrastructure.storage.MinioStorageAdapter
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import java.io.ByteArrayOutputStream
import java.util.UUID

// UUIDs issus du seed V2
private val EDITOR_MARIE  = UUID.fromString("22222222-0000-0000-0000-000000000001")
private val CHU_TOULOUSE  = UUID.fromString("11111111-0000-0000-0000-000000000001")

class UploadDocumentUseCaseIT(
    @param:Autowired private val uploadDocumentUseCase: UploadDocumentUseCase,
    @param:Autowired private val documentRepository: DocumentRepository,
    @param:Autowired private val documentIndexRepository: DocumentIndexRepository,
    @param:Autowired private val elasticsearchOperations: ElasticsearchOperations,
    @param:Autowired private val searchPort: SearchPort,
    @param:Autowired private val minioClient: MinioClient,
    @param:Value("\${minio.bucket}") private val bucket: String
) : AbstractIntegrationTest() {

    private val createdDocIds = mutableListOf<UUID>()

    @AfterEach
    fun cleanup() {
        // Suppression des documents créés en PostgreSQL
        createdDocIds.forEach { documentRepository.delete(it) }
        createdDocIds.clear()

        // Nettoyage de l'index Elasticsearch
        documentIndexRepository.deleteAll()
        elasticsearchOperations.indexOps(DocumentIndex::class.java).refresh()

        // Nettoyage du bucket MinIO
        val objects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build())
        objects.forEach { item ->
            minioClient.removeObject(
                RemoveObjectArgs.builder().bucket(bucket).`object`(item.get().objectName()).build()
            )
        }
    }

    @Test
    fun `upload d'un PDF persisté en base, stocké dans MinIO, indexé dans Elasticsearch`() {
        val pdfBytes = createPdf("Procédure nettoyage bloc opératoire")
        val command = UploadDocumentCommand(
            title = "Protocole hygiène",
            type = DocumentType.PROCEDURE,
            uploaderId = EDITOR_MARIE,
            establishmentId = CHU_TOULOUSE,
            filename = "hygiene.pdf",
            mimeType = "application/pdf",
            sizeBytes = pdfBytes.size.toLong(),
            contentStream = pdfBytes.inputStream()
        )

        val result = uploadDocumentUseCase.execute(command)
        createdDocIds += result.id

        // Vérifié en PostgreSQL
        val docInDb = documentRepository.findById(result.id)
        assertNotNull(docInDb)
        assertEquals("Protocole hygiène", docInDb!!.title)
        assertEquals(DocumentType.PROCEDURE, docInDb.type)

        // Vérifié dans MinIO (la clé de stockage commence par "documents/")
        val objects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build()).toList()
        assertTrue(objects.isNotEmpty())
        assertTrue(objects.any { it.get().objectName().startsWith("$CHU_TOULOUSE/PROCEDURE/") })

        // Vérifié dans Elasticsearch
        refreshIndex()
        val indexed = documentIndexRepository.findById(result.id.toString())
        assertTrue(indexed.isPresent)
        assertEquals("Protocole hygiène", indexed.get().title)
    }

    @Test
    fun `le contenu textuel du PDF est extrait et indexé pour la recherche full-text`() {
        val contenuPdf = "Stérilisation autoclave classe B température 134 degrés durée 18 minutes"
        val pdfBytes = createPdf(contenuPdf)
        val command = UploadDocumentCommand(
            title = "Protocole stérilisation",
            type = DocumentType.PROCEDURE,
            uploaderId = EDITOR_MARIE,
            establishmentId = CHU_TOULOUSE,
            filename = "sterilisation.pdf",
            mimeType = "application/pdf",
            sizeBytes = pdfBytes.size.toLong(),
            contentStream = pdfBytes.inputStream()
        )

        val result = uploadDocumentUseCase.execute(command)
        createdDocIds += result.id
        refreshIndex()

        // Recherche par un terme présent dans le contenu PDF, pas dans le titre
        val results = searchPort.search("autoclave", listOf(CHU_TOULOUSE))

        assertEquals(1, results.size)
        assertEquals(result.id, results[0].documentId)
        assertNotNull(results[0].snippet)
        assertTrue(results[0].snippet!!.contains("autoclave"))
    }

    @Test
    fun `un fichier non-PDF est indexé avec son titre comme contenu`() {
        val command = UploadDocumentCommand(
            title = "Tableau de bord qualité",
            type = DocumentType.FORM,
            uploaderId = EDITOR_MARIE,
            establishmentId = CHU_TOULOUSE,
            filename = "tableau.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            sizeBytes = 512L,
            contentStream = ByteArray(512).inputStream()
        )

        val result = uploadDocumentUseCase.execute(command)
        createdDocIds += result.id
        refreshIndex()

        val indexed = documentIndexRepository.findById(result.id.toString())
        assertTrue(indexed.isPresent)
        // Le contenu indexé doit être le titre puisque ce n'est pas un PDF
        assertEquals("Tableau de bord qualité", indexed.get().content)
    }

    private fun refreshIndex() {
        elasticsearchOperations.indexOps(DocumentIndex::class.java).refresh()
    }

    /** Crée un PDF minimal avec PDFBox contenant le texte fourni. */
    private fun createPdf(text: String): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
            cs.newLineAtOffset(50f, 700f)
            cs.showText(text)
            cs.endText()
        }
        return ByteArrayOutputStream().also { doc.save(it); doc.close() }.toByteArray()
    }
}
