package com.qualidoc.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

// ── Établissement ────────────────────────────────────────────────────────────

data class Establishment(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val code: String,
    val active: Boolean = true,
    val groupementId: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now()
)

// ── Rôle utilisateur ─────────────────────────────────────────────────────────

enum class UserRole {
    READER,   // lecture seule
    EDITOR    // upload + partage
}

// ── Utilisateur ──────────────────────────────────────────────────────────────

data class User(
    val id: UUID = UUID.randomUUID(),
    val establishmentId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val active: Boolean = true,
    val passwordHash: String? = null,
    val createdAt: Instant = Instant.now()
) {
    val fullName: String get() = "$firstName $lastName"

    fun canUpload(): Boolean = role == UserRole.EDITOR
    fun canShare(): Boolean = role == UserRole.EDITOR
}

// ── Type de document ─────────────────────────────────────────────────────────

enum class DocumentType(val label: String) {
    PROCEDURE("Procédure"),
    PROTOCOL("Protocole"),
    FORM("Formulaire qualité"),
    AWARENESS_BOOKLET("Livret de sensibilisation")
}

// ── Dossier ──────────────────────────────────────────────────────────────────

data class Folder(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val parentId: UUID? = null,
    val groupementId: UUID,
    val createdAt: Instant = Instant.now()
)

// ── Document ─────────────────────────────────────────────────────────────────

data class Document(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val type: DocumentType,
    val uploaderId: UUID,
    val establishmentId: UUID,
    val folderId: UUID? = null,
    val storageKey: String,          // clé MinIO / S3
    val originalFilename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val version: Int = 1,
    val published: Boolean = true,   // publication directe (pas de workflow)
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

// ── Journal d'audit (RGPD / HDS) ─────────────────────────────────────────────

enum class AuditAction {
    DOCUMENT_UPLOADED,
    DOCUMENT_DOWNLOADED,
    DOCUMENT_DELETED,
    USER_LOGIN,
    USER_LOGOUT
}

data class AuditLog(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val documentId: UUID?,
    val action: AuditAction,
    val details: String? = null,
    val occurredAt: Instant = Instant.now()
)

// ── Notification ──────────────────────────────────────────────────────────────

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val documentId: UUID,
    val recipientEmail: String,
    val subject: String,
    val body: String,
    val sent: Boolean = false,
    val createdAt: Instant = Instant.now()
)

// ── Refresh Token ────────────────────────────────────────────────────────────

data class RefreshToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: LocalDateTime,
    val revoked: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
