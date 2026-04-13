package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.TestFixtures
import com.qualidoc.TestSecurityConfig
import com.qualidoc.application.usecase.*
import com.qualidoc.domain.model.User
import com.qualidoc.domain.model.UserRole
import com.qualidoc.infrastructure.security.JwtService
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.AdminController
import com.qualidoc.presentation.controller.HealthController
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [HealthController::class, AdminController::class])
@Import(SecurityConfig::class, TestSecurityConfig::class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

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

    private fun tokenForRole(role: UserRole): String {
        val user = when (role) {
            UserRole.EDITOR -> User(
                id = TestFixtures.EDITOR_ID,
                establishmentId = TestFixtures.ESTABLISHMENT_ID,
                email = TestFixtures.EDITOR_EMAIL,
                firstName = "Marie", lastName = "Dupont", role = role
            )
            UserRole.READER -> User(
                id = TestFixtures.READER_ID,
                establishmentId = TestFixtures.ESTABLISHMENT_ID,
                email = TestFixtures.READER_EMAIL,
                firstName = "Jean", lastName = "Martin", role = role
            )
        }
        return jwtService.generateAccessToken(user)
    }

    // ── Public endpoints ────────────────────────────────────────────────────

    @Test
    fun `health_endpoint_should_be_accessible_without_token`() {
        mockMvc.get("/api/v1/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
        }
    }

    // ── Protected endpoints ─────────────────────────────────────────────────

    @Test
    fun `admin_endpoint_should_return_401_without_token`() {
        mockMvc.get("/api/v1/admin/users").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `admin_endpoint_should_return_403_for_reader_role`() {
        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer ${tokenForRole(UserRole.READER)}")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `admin_endpoint_should_return_200_for_editor_role`() {
        every { listUsersUseCase.execute(null) } returns emptyList()

        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer ${tokenForRole(UserRole.EDITOR)}")
        }.andExpect {
            status { isOk() }
        }
    }

    // ── Invalid token ───────────────────────────────────────────────────────

    @Test
    fun `protected_endpoint_should_return_401_with_malformed_token`() {
        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer not-a-valid-jwt")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `protected_endpoint_should_return_401_with_expired_token`() {
        val expiredJwtService = JwtService(
            secret = "test-secret-for-unit-tests-only-min-32-chars",
            accessTokenExpiration = -1,
            refreshTokenExpiration = 604800
        )
        val expiredToken = expiredJwtService.generateAccessToken(
            User(
                id = TestFixtures.EDITOR_ID,
                establishmentId = TestFixtures.ESTABLISHMENT_ID,
                email = TestFixtures.EDITOR_EMAIL,
                firstName = "Marie", lastName = "Dupont", role = UserRole.EDITOR
            )
        )

        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer $expiredToken")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
