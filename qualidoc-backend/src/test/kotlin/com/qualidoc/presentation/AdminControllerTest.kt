package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.TestFixtures
import com.qualidoc.TestSecurityConfig
import com.qualidoc.application.dto.CreateUserRequest
import com.qualidoc.application.dto.ResetPasswordRequest
import com.qualidoc.application.dto.UpdateUserRequest
import com.qualidoc.application.dto.UserDto
import com.qualidoc.application.usecase.*
import com.qualidoc.domain.model.User
import com.qualidoc.domain.model.UserRole
import com.qualidoc.infrastructure.security.JwtService
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.AdminController
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(AdminController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        .apply { findAndRegisterModules() }

    @MockkBean
    lateinit var listUsersUseCase: ListUsersUseCase

    @MockkBean
    lateinit var createUserUseCase: CreateUserUseCase

    @MockkBean
    lateinit var updateUserUseCase: UpdateUserUseCase

    @MockkBean
    lateinit var resetUserPasswordUseCase: ResetUserPasswordUseCase

    @MockkBean
    lateinit var deleteUserUseCase: DeleteUserUseCase

    private fun editorToken(): String = jwtService.generateAccessToken(
        User(
            id = TestFixtures.EDITOR_ID,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            email = TestFixtures.EDITOR_EMAIL,
            firstName = "Marie",
            lastName = "Dupont",
            role = UserRole.EDITOR
        )
    )

    private fun readerToken(): String = jwtService.generateAccessToken(
        User(
            id = TestFixtures.READER_ID,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            email = TestFixtures.READER_EMAIL,
            firstName = "Jean",
            lastName = "Martin",
            role = UserRole.READER
        )
    )

    private fun aUserDto(
        id: UUID = TestFixtures.EDITOR_ID,
        email: String = TestFixtures.EDITOR_EMAIL,
        role: UserRole = UserRole.EDITOR
    ) = UserDto(
        id = id,
        email = email,
        firstName = "Marie",
        lastName = "Dupont",
        role = role,
        establishmentId = TestFixtures.ESTABLISHMENT_ID
    )

    // ── GET /api/v1/admin/users ─────────────────────────────────────────────

    @Test
    fun `GET users should_return_200_for_editor`() {
        val users = listOf(aUserDto())
        every { listUsersUseCase.execute(null) } returns users

        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer ${editorToken()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].email") { value(TestFixtures.EDITOR_EMAIL) }
        }
    }

    @Test
    fun `GET users should_return_403_for_reader`() {
        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer ${readerToken()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET users should_return_401_without_token`() {
        mockMvc.get("/api/v1/admin/users").andExpect {
            status { isUnauthorized() }
        }
    }

    // ── POST /api/v1/admin/users ────────────────────────────────────────────

    @Test
    fun `POST users should_return_201_for_editor`() {
        val request = CreateUserRequest(
            email = "new-user@qualidoc.fr",
            firstName = "Pierre",
            lastName = "Durand",
            role = UserRole.READER,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            password = TestFixtures.DEFAULT_PASSWORD
        )
        val createdUser = aUserDto(
            id = UUID.randomUUID(),
            email = "new-user@qualidoc.fr",
            role = UserRole.READER
        )
        every { createUserUseCase.execute(request) } returns createdUser

        mockMvc.post("/api/v1/admin/users") {
            header("Authorization", "Bearer ${editorToken()}")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("new-user@qualidoc.fr") }
            jsonPath("$.role") { value("READER") }
        }
    }

    @Test
    fun `POST users should_return_403_for_reader`() {
        val request = CreateUserRequest(
            email = "new-user@qualidoc.fr",
            firstName = "Pierre",
            lastName = "Durand",
            role = UserRole.READER,
            establishmentId = TestFixtures.ESTABLISHMENT_ID,
            password = TestFixtures.DEFAULT_PASSWORD
        )

        mockMvc.post("/api/v1/admin/users") {
            header("Authorization", "Bearer ${readerToken()}")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ── PATCH /api/v1/admin/users/{id} ──────────────────────────────────────

    @Test
    fun `PATCH users should_return_200_for_editor`() {
        val userId = TestFixtures.READER_ID
        val request = UpdateUserRequest(firstName = "NouveauPrenom")
        val updatedUser = aUserDto(id = userId, email = TestFixtures.READER_EMAIL, role = UserRole.READER)
        every { updateUserUseCase.execute(userId, request) } returns updatedUser

        mockMvc.patch("/api/v1/admin/users/$userId") {
            header("Authorization", "Bearer ${editorToken()}")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value(TestFixtures.READER_EMAIL) }
        }
    }

    @Test
    fun `PATCH users should_return_403_for_reader`() {
        val userId = UUID.randomUUID()
        val request = UpdateUserRequest(firstName = "Test")

        mockMvc.patch("/api/v1/admin/users/$userId") {
            header("Authorization", "Bearer ${readerToken()}")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ── POST /api/v1/admin/users/{id}/reset-password ────────────────────────

    @Test
    fun `POST reset-password should_return_200_for_editor`() {
        val userId = TestFixtures.READER_ID
        val request = ResetPasswordRequest("N3wP@ssw0rd!")
        every { resetUserPasswordUseCase.execute(userId, request.newPassword) } just Runs

        mockMvc.post("/api/v1/admin/users/$userId/reset-password") {
            header("Authorization", "Bearer ${editorToken()}")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }

        verify(exactly = 1) { resetUserPasswordUseCase.execute(userId, request.newPassword) }
    }

    @Test
    fun `POST reset-password should_return_403_for_reader`() {
        val userId = UUID.randomUUID()
        val request = ResetPasswordRequest("N3wP@ssw0rd!")

        mockMvc.post("/api/v1/admin/users/$userId/reset-password") {
            header("Authorization", "Bearer ${readerToken()}")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ── DELETE /api/v1/admin/users/{id} ─────────────────────────────────────

    @Test
    fun `DELETE user should_return_204_for_editor`() {
        val userId = TestFixtures.READER_ID
        every { deleteUserUseCase.execute(userId) } just Runs

        mockMvc.delete("/api/v1/admin/users/$userId") {
            header("Authorization", "Bearer ${editorToken()}")
        }.andExpect {
            status { isNoContent() }
        }

        verify(exactly = 1) { deleteUserUseCase.execute(userId) }
    }

    @Test
    fun `DELETE user should_return_403_for_reader`() {
        val userId = UUID.randomUUID()

        mockMvc.delete("/api/v1/admin/users/$userId") {
            header("Authorization", "Bearer ${readerToken()}")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE user should_return_401_without_token`() {
        mockMvc.delete("/api/v1/admin/users/${UUID.randomUUID()}").andExpect {
            status { isUnauthorized() }
        }
    }
}
