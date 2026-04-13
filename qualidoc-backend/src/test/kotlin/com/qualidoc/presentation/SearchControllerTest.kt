package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.TestSecurityConfig
import com.qualidoc.application.dto.SearchResultDto
import com.qualidoc.application.usecase.SearchDocumentUseCase
import com.qualidoc.domain.model.User
import com.qualidoc.domain.model.UserRole
import com.qualidoc.infrastructure.security.JwtService
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.SearchController
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(SearchController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

    @MockkBean
    lateinit var searchDocumentUseCase: SearchDocumentUseCase

    private val userId = UUID.randomUUID()
    private val establishmentId = UUID.randomUUID()

    private fun token(): String = jwtService.generateAccessToken(
        User(id = userId, establishmentId = establishmentId, email = "test@test.com",
            firstName = "Test", lastName = "User", role = UserRole.READER)
    )

    @Test
    fun `GET search retourne 200 avec les resultats`() {
        val results = listOf(
            SearchResultDto(
                documentId = UUID.randomUUID(),
                title = "Procédure hygiène",
                type = "PROCEDURE",
                establishmentId = establishmentId,
                snippet = "...les mains doivent être lavées..."
            )
        )
        every { searchDocumentUseCase.execute(any()) } returns results

        mockMvc.get("/api/v1/search?q=hygiène") {
            header("Authorization", "Bearer ${token()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].title") { value("Procédure hygiène") }
        }
    }

    @Test
    fun `GET search retourne 200 avec liste vide si aucun resultat`() {
        every { searchDocumentUseCase.execute(any()) } returns emptyList()

        mockMvc.get("/api/v1/search?q=inexistant") {
            header("Authorization", "Bearer ${token()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `GET search retourne 401 sans authentification`() {
        mockMvc.get("/api/v1/search?q=test").andExpect {
            status { isUnauthorized() }
        }
    }
}
