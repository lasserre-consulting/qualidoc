package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.TestSecurityConfig
import com.qualidoc.domain.model.Establishment
import com.qualidoc.domain.model.User
import com.qualidoc.domain.model.UserRole
import com.qualidoc.domain.repository.EstablishmentRepository
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
    lateinit var establishmentRepository: EstablishmentRepository

    private fun token(): String = jwtService.generateAccessToken(
        User(id = UUID.randomUUID(), establishmentId = UUID.randomUUID(), email = "test@test.com",
            firstName = "Test", lastName = "User", role = UserRole.READER)
    )

    @Test
    fun `GET establishments retourne la liste des etablissements actifs`() {
        val active = Establishment(id = UUID.randomUUID(), name = "Clinique Nord", code = "CLN", active = true)
        val inactive = Establishment(id = UUID.randomUUID(), name = "Clinique Sud", code = "CLS", active = false)
        every { establishmentRepository.findAll() } returns listOf(active, inactive)

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
    fun `GET establishments filtre les etablissements inactifs`() {
        every { establishmentRepository.findAll() } returns listOf(
            Establishment(id = UUID.randomUUID(), name = "Fermé", code = "FRM", active = false)
        )

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
