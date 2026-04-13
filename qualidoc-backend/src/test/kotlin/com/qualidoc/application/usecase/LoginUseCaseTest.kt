package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.application.dto.LoginRequest
import com.qualidoc.domain.port.JwtPort
import com.qualidoc.domain.repository.RefreshTokenRepository
import com.qualidoc.domain.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class LoginUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtPort = mockk<JwtPort>()

    private lateinit var useCase: LoginUseCase

    companion object {
        private const val RAW_PASSWORD = "S3cur3P@ss!"
        private const val ACCESS_TOKEN = "access-token-value"
        private const val REFRESH_TOKEN = "refresh-token-value"
        private const val REFRESH_EXPIRATION = 604800L
    }

    @BeforeEach
    fun setUp() {
        useCase = LoginUseCase(userRepository, refreshTokenRepository, passwordEncoder, jwtPort)
    }

    @Test
    fun `should_return_auth_response_when_credentials_are_valid`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findByEmail(user.email) } returns user
        every { passwordEncoder.matches(RAW_PASSWORD, user.passwordHash) } returns true
        every { jwtPort.generateAccessToken(user) } returns ACCESS_TOKEN
        every { jwtPort.generateRefreshToken() } returns REFRESH_TOKEN
        every { jwtPort.refreshTokenExpirationSeconds() } returns REFRESH_EXPIRATION
        every { refreshTokenRepository.save(any()) } returns mockk()

        val result = useCase.execute(LoginRequest(user.email, RAW_PASSWORD))

        assertThat(result.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(REFRESH_TOKEN)
        assertThat(result.user.id).isEqualTo(user.id)
        assertThat(result.user.email).isEqualTo(user.email)
        assertThat(result.user.role).isEqualTo(user.role)
    }

    @Test
    fun `should_persist_refresh_token_hash_on_successful_login`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findByEmail(user.email) } returns user
        every { passwordEncoder.matches(RAW_PASSWORD, user.passwordHash) } returns true
        every { jwtPort.generateAccessToken(user) } returns ACCESS_TOKEN
        every { jwtPort.generateRefreshToken() } returns REFRESH_TOKEN
        every { jwtPort.refreshTokenExpirationSeconds() } returns REFRESH_EXPIRATION
        every { refreshTokenRepository.save(any()) } returns mockk()

        useCase.execute(LoginRequest(user.email, RAW_PASSWORD))

        verify(exactly = 1) { refreshTokenRepository.save(match { it.userId == user.id }) }
    }

    @Test
    fun `should_throw_authentication_exception_when_email_is_unknown`() {
        every { userRepository.findByEmail("unknown@test.com") } returns null

        assertThatThrownBy { useCase.execute(LoginRequest("unknown@test.com", RAW_PASSWORD)) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("incorrect")
    }

    @Test
    fun `should_throw_authentication_exception_when_password_is_wrong`() {
        val user = TestFixtures.anEditor()
        every { userRepository.findByEmail(user.email) } returns user
        every { passwordEncoder.matches("wrong-password", user.passwordHash) } returns false

        assertThatThrownBy { useCase.execute(LoginRequest(user.email, "wrong-password")) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("incorrect")
    }

    @Test
    fun `should_throw_authentication_exception_when_user_is_inactive`() {
        val inactiveUser = TestFixtures.anEditor(active = false)
        every { userRepository.findByEmail(inactiveUser.email) } returns inactiveUser

        assertThatThrownBy { useCase.execute(LoginRequest(inactiveUser.email, RAW_PASSWORD)) }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessageContaining("sactiv")
    }

    @Test
    fun `should_throw_authentication_exception_when_password_hash_is_null`() {
        val userWithoutPassword = TestFixtures.anEditor(passwordHash = null)
        every { userRepository.findByEmail(userWithoutPassword.email) } returns userWithoutPassword

        assertThatThrownBy { useCase.execute(LoginRequest(userWithoutPassword.email, RAW_PASSWORD)) }
            .isInstanceOf(AuthenticationException::class.java)
    }
}
