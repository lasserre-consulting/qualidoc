package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.TestFixtures
import com.qualidoc.TestSecurityConfig
import com.qualidoc.application.dto.EstablishmentDto
import com.qualidoc.application.usecase.ListActiveEstablishmentsUseCase
import com.qualidoc.domain.model.User
import com.qualidoc.domain.model.UserRole
import com.qualidoc.infrastructure.security.JwtService
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.EstablishmentController
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

@WebMvcTest(EstablishmentController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
@ActiveProfiles("test")
class EstablishmentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

    @MockkBean
    lateinit var listActiveEstablishmentsUseCase: ListActiveEstablishmentsUseCase

    private fun token(): String = jwtService.generateAccessToken(
        User(id = UUID.randomUUID(), establishmentId = UUID.randomUUID(), email = "test@test.com",
            firstName = "Test", lastName = "User", role = UserRole.READER)
    )

    @Test
    fun `GET establishments retourne la liste des etablissements actifs`() {
        val activeEstablishment = EstablishmentDto(
            id = UUID.randomUUID(), name = "Clinique Nord", code = "CLN", active = true
        )
        every { listActiveEstablishmentsUseCase.execute() } returns listOf(activeEstablishment)

        mockMvc.get("/api/v1/establishments") {
            header("Authorization", "Bearer ${token()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].name") { value("Clinique Nord") }
            jsonPath("$[0].code") { value("CLN") }
            jsonPath("$[0].active") { value(true) }
        }
    }

    @Test
    fun `GET establishments retourne liste vide quand aucun actif`() {
        every { listActiveEstablishmentsUseCase.execute() } returns emptyList()

        mockMvc.get("/api/v1/establishments") {
            header("Authorization", "Bearer ${token()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `GET establishments retourne 401 sans authentification`() {
        mockMvc.get("/api/v1/establishments").andExpect {
            status { isUnauthorized() }
        }
    }
}
