package com.qualidoc.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.application.dto.DocumentDto
import com.qualidoc.application.usecase.DeleteDocumentUseCase
import com.qualidoc.application.usecase.DownloadDocumentUseCase
import com.qualidoc.application.usecase.DownloadResult
import com.qualidoc.application.usecase.GetEstablishmentDocumentsUseCase
import com.qualidoc.application.usecase.UploadDocumentUseCase
import com.qualidoc.domain.model.DocumentType
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.DocumentController
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import

import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID

@WebMvcTest(DocumentController::class)
@Import(SecurityConfig::class)
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var uploadDocumentUseCase: UploadDocumentUseCase

    @MockkBean
    lateinit var getEstablishmentDocumentsUseCase: GetEstablishmentDocumentsUseCase

    @MockkBean
    lateinit var downloadDocumentUseCase: DownloadDocumentUseCase

    @MockkBean
    lateinit var deleteDocumentUseCase: DeleteDocumentUseCase

    private val userId = UUID.randomUUID()
    private val establishmentId = UUID.randomUUID()

    @Test
    fun `GET documents retourne 200 pour un utilisateur authentifié`() {
        val docs = listOf(
            DocumentDto(
                id = UUID.randomUUID(),
                title = "Procédure hygiène",
                type = DocumentType.PROCEDURE,
                typeLabel = "Procédure / protocole",
                uploaderId = userId,
                establishmentId = establishmentId,
                originalFilename = "hygiene.pdf",
                mimeType = "application/pdf",
                sizeBytes = 2048L,
                version = 1,
                createdAt = Instant.now()
            )
        )

        every { getEstablishmentDocumentsUseCase.execute(userId) } returns docs

        mockMvc.get("/api/v1/documents") {
            with(
                jwt().jwt { builder ->
                    builder
                        .subject(userId.toString())
                        .claim("establishment_id", establishmentId.toString())
                        .claim("realm_access", mapOf("roles" to listOf("READER")))
                }
            )
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$[0].title") { value("Procédure hygiène") }
            jsonPath("$[0].type") { value("PROCEDURE") }
        }
    }

    @Test
    fun `GET documents retourne 401 sans authentification`() {
        mockMvc.get("/api/v1/documents").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST documents retourne 201 pour un éditeur`() {
        val file = MockMultipartFile("file", "hygiene.pdf", "application/pdf", ByteArray(512))
        val savedDoc = buildDocumentDto("Protocole hygiène")

        every { uploadDocumentUseCase.execute(any()) } returns savedDoc

        mockMvc.multipart("/api/v1/documents") {
            file(file)
            param("title", "Protocole hygiène")
            param("type", "PROCEDURE")
            with(jwt().jwt { it.subject(userId.toString()).claim("establishment_id", establishmentId.toString()) }
                .authorities(SimpleGrantedAuthority("ROLE_EDITOR")))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("Protocole hygiène") }
        }
    }

    @Test
    fun `POST documents retourne 403 pour un lecteur`() {
        val file = MockMultipartFile("file", "hygiene.pdf", "application/pdf", ByteArray(512))

        mockMvc.multipart("/api/v1/documents") {
            file(file)
            param("title", "Protocole hygiène")
            param("type", "PROCEDURE")
            with(jwt().jwt { it.subject(userId.toString()) }
                .authorities(SimpleGrantedAuthority("ROLE_READER")))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET download retourne le fichier pour un utilisateur authentifié`() {
        val docId = UUID.randomUUID()
        val content = "PDF content".toByteArray()

        every { downloadDocumentUseCase.execute(docId, userId) } returns DownloadResult(
            inputStream = ByteArrayInputStream(content),
            filename = "hygiene.pdf",
            mimeType = "application/pdf",
            sizeBytes = content.size.toLong()
        )

        mockMvc.get("/api/v1/documents/$docId/download") {
            with(jwt().jwt { it.subject(userId.toString()).claim("establishment_id", establishmentId.toString()) })
        }.andExpect {
            status { isOk() }
            header { string("Content-Disposition", "attachment; filename=\"hygiene.pdf\"") }
        }
    }

    @Test
    fun `DELETE document retourne 204 pour un éditeur`() {
        val docId = UUID.randomUUID()
        every { deleteDocumentUseCase.execute(docId, userId) } returns Unit

        mockMvc.delete("/api/v1/documents/$docId") {
            with(jwt().jwt { it.subject(userId.toString()).claim("establishment_id", establishmentId.toString()) }
                .authorities(SimpleGrantedAuthority("ROLE_EDITOR")))
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE document retourne 403 pour un lecteur`() {
        val docId = UUID.randomUUID()

        mockMvc.delete("/api/v1/documents/$docId") {
            with(jwt().jwt { it.subject(userId.toString()) }
                .authorities(SimpleGrantedAuthority("ROLE_READER")))
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildDocumentDto(title: String) = DocumentDto(
        id = UUID.randomUUID(),
        title = title,
        type = DocumentType.PROCEDURE,
        typeLabel = "Procédure / protocole",
        uploaderId = userId,
        establishmentId = establishmentId,
        originalFilename = "hygiene.pdf",
        mimeType = "application/pdf",
        sizeBytes = 512L,
        version = 1,
        createdAt = Instant.now()
    )
}
