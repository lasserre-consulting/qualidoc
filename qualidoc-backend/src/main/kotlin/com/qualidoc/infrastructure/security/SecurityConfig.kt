package com.qualidoc.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Endpoints publics
                    .requestMatchers("/api/v1/health").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    // Documents : écriture réservée aux éditeurs
                    .requestMatchers(HttpMethod.POST, "/api/v1/documents").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/documents/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").hasRole("EDITOR")
                    // Dossiers : écriture réservée aux éditeurs
                    .requestMatchers(HttpMethod.POST, "/api/v1/folders").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/folders/**").hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/folders/**").hasRole("EDITOR")
                    // Lecture pour tous les utilisateurs authentifiés
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
        return http.build()
    }

    /**
     * Extrait les rôles Keycloak depuis le claim imbriqué "realm_access" -> "roles"
     * et les préfixe avec "ROLE_" pour Spring Security.
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt ->
                val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
                    ?: return@setJwtGrantedAuthoritiesConverter emptyList()
                @Suppress("UNCHECKED_CAST")
                val roles = realmAccess["roles"] as? List<String>
                    ?: return@setJwtGrantedAuthoritiesConverter emptyList()
                roles.map { SimpleGrantedAuthority("ROLE_$it") }
            }
        }
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
