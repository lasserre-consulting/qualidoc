package com.qualidoc.presentation

import com.ninjasquad.springmockk.MockkBean
import com.qualidoc.TestFixtures
import com.qualidoc.TestSecurityConfig
import com.qualidoc.application.dto.AuthResponse
import com.qualidoc.application.dto.LoginRequest
import com.qualidoc.application.dto.RefreshRequest
import com.qualidoc.application.usecase.*
import com.qualidoc.domain.model.User
import com.qualidoc.domain.model.UserRole
import com.qualidoc.infrastructure.security.JwtService
import com.qualidoc.infrastructure.security.SecurityConfig
import com.qualidoc.presentation.controller.AuthController
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        .apply { findAndRegisterModules() }

    @MockkBean
    lateinit var loginUseCase: LoginUseCase

    @MockkBean
    lateinit var refreshTokenUseCase: RefreshTokenUseCase

    @MockkBean
    lateinit var logoutUseCase: LogoutUseCase

    @MockkBean
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    companion object {
        private const val ACCESS_TOKEN = "generated-access-token"
        private const val REFRESH_TOKEN = "generated-refresh-token"
    }

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

    private fun anAuthResponse() = AuthResponse(
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
        user = toUserDto(TestFixtures.anEditor())
    )

    private fun toUserDto(user: User) = com.qualidoc.application.dto.UserDto(
        id = user.id,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        role = user.role,
        establishmentId = user.establishmentId
    )

    // ── POST /api/v1/auth/login ─────────────────────────────────────────────

    @Test
    fun `POST login should_return_200_with_auth_response_when_credentials_are_valid`() {
        val loginRequest = LoginRequest(TestFixtures.EDITOR_EMAIL, TestFixtures.DEFAULT_PASSWORD)
        every { loginUseCase.execute(loginRequest) } returns anAuthResponse()

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { value(ACCESS_TOKEN) }
            jsonPath("$.refreshToken") { value(REFRESH_TOKEN) }
            jsonPath("$.user.email") { value(TestFixtures.EDITOR_EMAIL) }
            jsonPath("$.user.role") { value("EDITOR") }
        }
    }

    @Test
    fun `POST login should_return_401_when_credentials_are_invalid`() {
        val loginRequest = LoginRequest("bad@test.com", "wrong-password")
        every { loginUseCase.execute(loginRequest) } throws AuthenticationException("Email ou mot de passe incorrect")

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { value("unauthorized") }
        }
    }

    // ── POST /api/v1/auth/refresh ───────────────────────────────────────────

    @Test
    fun `POST refresh should_return_200_with_new_tokens`() {
        val refreshRequest = RefreshRequest("valid-refresh-token")
        every { refreshTokenUseCase.execute(refreshRequest) } returns anAuthResponse()

        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(refreshRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { value(ACCESS_TOKEN) }
            jsonPath("$.refreshToken") { value(REFRESH_TOKEN) }
        }
    }

    @Test
    fun `POST refresh should_return_401_when_refresh_token_is_invalid`() {
        val refreshRequest = RefreshRequest("invalid-refresh-token")
        every { refreshTokenUseCase.execute(refreshRequest) } throws AuthenticationException("Refresh token invalide")

        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(refreshRequest)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { value("unauthorized") }
        }
    }

    // ── POST /api/v1/auth/logout ────────────────────────────────────────────

    @Test
    fun `POST logout should_return_200_with_refresh_token_in_body`() {
        every { logoutUseCase.execute("some-refresh-token") } just Runs

        mockMvc.post("/api/v1/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"some-refresh-token"}"""
        }.andExpect {
            status { isOk() }
        }

        verify(exactly = 1) { logoutUseCase.execute("some-refresh-token") }
    }

    @Test
    fun `POST logout should_return_200_without_body`() {
        every { logoutUseCase.execute(null) } just Runs

        mockMvc.post("/api/v1/auth/logout").andExpect {
            status { isOk() }
        }
    }

    // ── GET /api/v1/auth/me ─────────────────────────────────────────────────

    @Test
    fun `GET me should_return_200_with_user_dto`() {
        val userDto = toUserDto(TestFixtures.anEditor())
        every { getCurrentUserUseCase.execute(TestFixtures.EDITOR_ID) } returns userDto

        mockMvc.get("/api/v1/auth/me") {
            header("Authorization", "Bearer ${editorToken()}")
            accept(MediaType.APPLICATION_JSON)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(TestFixtures.EDITOR_ID.toString()) }
            jsonPath("$.email") { value(TestFixtures.EDITOR_EMAIL) }
            jsonPath("$.role") { value("EDITOR") }
        }
    }

    @Test
    fun `GET me should_return_401_without_token`() {
        mockMvc.get("/api/v1/auth/me").andExpect {
            status { isUnauthorized() }
        }
    }
}
