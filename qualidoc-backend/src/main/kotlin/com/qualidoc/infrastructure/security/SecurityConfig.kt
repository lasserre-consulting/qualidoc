package com.qualidoc.infrastructure.security

import com.qualidoc.domain.model.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val role: UserRole,
    val establishmentId: UUID
)

class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)
            try {
                val claims = jwtService.parseAccessToken(token)
                val userId = UUID.fromString(claims.subject)
                val role = UserRole.valueOf(claims["role"] as String)
                val establishmentId = UUID.fromString(claims["establishment_id"] as String)

                val principal = AuthenticatedUser(userId, role, establishmentId)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
                val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: Exception) {
                // Invalid token -- continue without authentication
            }
        }
        filterChain.doFilter(request, response)
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtService)

    @Bean
    fun jwtFilterRegistration(filter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                    .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                    .requestMatchers("/api/v1/health").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/documents/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/documents/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.POST, "/api/v1/folders/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/folders/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/folders/**").hasRole("EDITOR")
                    .requestMatchers("/api/v1/admin/**").hasRole("EDITOR")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json"
                    response.writer.write("""{"error": "unauthorized", "message": "Authentification requise"}""")
                }
                exceptions.accessDeniedHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = "application/json"
                    response.writer.write("""{"error": "forbidden", "message": "Accès refusé"}""")
                }
            }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:4200", "https://www.lasserre-consulting.fr")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "Authorization", "X-Requested-With")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }
    }
}
