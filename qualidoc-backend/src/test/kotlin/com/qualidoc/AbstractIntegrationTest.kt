package com.qualidoc

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * Classe de base pour les tests d'intégration.
 *
 * Utilise les services locaux déjà en cours d'exécution (PostgreSQL :5432,
 * Elasticsearch :9200, MinIO :9000) définis dans application.yml.
 *
 * Note CI : ces tests requièrent que les services soient disponibles.
 * Pour un pipeline CI sans services pré-installés, remplacer par TestContainers
 * quand une version compatible Docker 29 (API >= 1.40) sera disponible.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
abstract class AbstractIntegrationTest
