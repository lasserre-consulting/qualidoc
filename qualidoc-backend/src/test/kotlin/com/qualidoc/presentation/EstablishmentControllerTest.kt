package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.domain.model.Establishment
import com.qualidoc.domain.repository.EstablishmentRepository
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.EstablishmentController
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(EstablishmentController::class)
@Import(SecurityConfig::class)
@ActiveProfiles("test")
class EstablishmentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var establishmentRepository: EstablishmentRepository

    @Test
    fun `GET establishments retourne la liste des établissements actifs`() {
        val active = Establishment(id = UUID.randomUUID(), name = "Clinique Nord", code = "CLN", active = true)
        val inactive = Establishment(id = UUID.randomUUID(), name = "Clinique Sud", code = "CLS", active = false)
        every { establishmentRepository.findAll() } returns listOf(active, inactive)

        mockMvc.get("/api/v1/establishments") {
            with(jwt())
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
    fun `GET establishments filtre les établissements inactifs`() {
        every { establishmentRepository.findAll() } returns listOf(
            Establishment(id = UUID.randomUUID(), name = "Fermé", code = "FRM", active = false)
        )

        mockMvc.get("/api/v1/establishments") {
            with(jwt())
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
