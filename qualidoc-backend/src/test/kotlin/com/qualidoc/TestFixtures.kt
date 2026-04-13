package com.qualidoc

import com.qualidoc.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Factories d'objets de test reutilisables.
 * Chaque methode retourne un objet avec des valeurs par defaut
 * que l'on peut surcharger via copy().
 */
object TestFixtures {

    // ── IDs stables pour les tests ──────────────────────────────────────────
    val ESTABLISHMENT_ID: UUID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    val OTHER_ESTABLISHMENT_ID: UUID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    val GROUPEMENT_ID: UUID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    val EDITOR_ID: UUID = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    val READER_ID: UUID = UUID.fromString("cccccccc-0000-0000-0000-000000000002")
    val DOCUMENT_ID: UUID = UUID.fromString("dddddddd-0000-0000-0000-000000000001")

    // ── Constantes metier ───────────────────────────────────────────────────
    const val EDITOR_EMAIL = "editor@qualidoc.fr"
    const val READER_EMAIL = "reader@qualidoc.fr"
    const val DEFAULT_PASSWORD = "S3cur3P@ss!"
    const val DEFAULT_PASSWORD_HASH = "\$2a\$12\$dummyhashfortest000000000000000000000000000000000000"

    // ── Factories ───────────────────────────────────────────────────────────

    fun anEditor(
        id: UUID = EDITOR_ID,
        establishmentId: UUID = ESTABLISHMENT_ID,
        email: String = EDITOR_EMAIL,
        active: Boolean = true,
        passwordHash: String? = DEFAULT_PASSWORD_HASH
    ) = User(
        id = id,
        establishmentId = establishmentId,
        email = email,
        firstName = "Marie",
        lastName = "Dupont",
        role = UserRole.EDITOR,
        active = active,
        passwordHash = passwordHash
    )

    fun aReader(
        id: UUID = READER_ID,
        establishmentId: UUID = ESTABLISHMENT_ID,
        email: String = READER_EMAIL,
        active: Boolean = true,
        passwordHash: String? = DEFAULT_PASSWORD_HASH
    ) = User(
        id = id,
        establishmentId = establishmentId,
        email = email,
        firstName = "Jean",
        lastName = "Martin",
        role = UserRole.READER,
        active = active,
        passwordHash = passwordHash
    )

    fun anEstablishment(
        id: UUID = ESTABLISHMENT_ID,
        name: String = "CHU Toulouse",
        code: String = "CHU",
        active: Boolean = true,
        groupementId: UUID = GROUPEMENT_ID
    ) = Establishment(
        id = id,
        name = name,
        code = code,
        active = active,
        groupementId = groupementId
    )

    fun aRefreshToken(
        id: UUID = UUID.randomUUID(),
        userId: UUID = EDITOR_ID,
        tokenHash: String = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        revoked: Boolean = false
    ) = RefreshToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked
    )

    fun aDocument(
        id: UUID = DOCUMENT_ID,
        title: String = "Procedure hygiene",
        type: DocumentType = DocumentType.PROCEDURE,
        uploaderId: UUID = EDITOR_ID,
        establishmentId: UUID = ESTABLISHMENT_ID,
        folderId: UUID? = null,
        createdAt: Instant = Instant.now()
    ) = Document(
        id = id,
        title = title,
        type = type,
        uploaderId = uploaderId,
        establishmentId = establishmentId,
        folderId = folderId,
        storageKey = "$establishmentId/${type.name}/${UUID.randomUUID()}.pdf",
        originalFilename = "document.pdf",
        mimeType = "application/pdf",
        sizeBytes = 2048L,
        createdAt = createdAt
    )
}
