package com.qualidoc.infrastructure.persistence.entity

import com.qualidoc.domain.model.*
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

// ── Establishment ─────────────────────────────────────────────────────────────

@Entity
@Table(name = "establishment")
class EstablishmentEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val name: String,
    @Column(nullable = false, unique = true) val code: String,
    @Column(nullable = false) val active: Boolean = true,
    @Column(name = "groupement_id", nullable = false) val groupementId: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
) {
    fun toDomain() = Establishment(id = id, name = name, code = code, active = active, groupementId = groupementId, createdAt = createdAt)

    companion object {
        fun fromDomain(e: Establishment) = EstablishmentEntity(
            id = e.id, name = e.name, code = e.code, active = e.active, groupementId = e.groupementId, createdAt = e.createdAt
        )
    }
}

// ── Folder ────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "folder")
class FolderEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val name: String,
    @Column(name = "parent_id") val parentId: UUID? = null,
    @Column(name = "groupement_id", nullable = false) val groupementId: UUID,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
) {
    fun toDomain() = Folder(id = id, name = name, parentId = parentId, groupementId = groupementId, createdAt = createdAt)

    companion object {
        fun fromDomain(f: Folder) = FolderEntity(
            id = f.id, name = f.name, parentId = f.parentId, groupementId = f.groupementId, createdAt = f.createdAt
        )
    }
}

// ── User ──────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "app_user")
class UserEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "establishment_id", nullable = false) val establishmentId: UUID,
    @Column(nullable = false, unique = true) val email: String,
    @Column(name = "first_name", nullable = false) val firstName: String,
    @Column(name = "last_name", nullable = false) val lastName: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) val role: UserRole,
    @Column(nullable = false) val active: Boolean = true,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
) {
    fun toDomain() = User(
        id = id, establishmentId = establishmentId, email = email,
        firstName = firstName, lastName = lastName, role = role,
        active = active, createdAt = createdAt
    )

    companion object {
        fun fromDomain(u: User) = UserEntity(
            id = u.id, establishmentId = u.establishmentId, email = u.email,
            firstName = u.firstName, lastName = u.lastName, role = u.role,
            active = u.active, createdAt = u.createdAt
        )
    }
}

// ── Document ──────────────────────────────────────────────────────────────────

@Entity
@Table(name = "document")
class DocumentEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val title: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) val type: DocumentType,
    @Column(name = "uploader_id", nullable = false) val uploaderId: UUID,
    @Column(name = "establishment_id", nullable = false) val establishmentId: UUID,
    @Column(name = "folder_id") val folderId: UUID? = null,
    @Column(name = "storage_key", nullable = false, unique = true) val storageKey: String,
    @Column(name = "original_filename", nullable = false) val originalFilename: String,
    @Column(name = "mime_type", nullable = false) val mimeType: String,
    @Column(name = "size_bytes", nullable = false) val sizeBytes: Long,
    @Column(nullable = false) val version: Int = 1,
    @Column(nullable = false) val published: Boolean = true,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Document(
        id = id, title = title, type = type, uploaderId = uploaderId,
        establishmentId = establishmentId, folderId = folderId, storageKey = storageKey,
        originalFilename = originalFilename, mimeType = mimeType,
        sizeBytes = sizeBytes, version = version, published = published,
        createdAt = createdAt, updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(d: Document) = DocumentEntity(
            id = d.id, title = d.title, type = d.type, uploaderId = d.uploaderId,
            establishmentId = d.establishmentId, folderId = d.folderId, storageKey = d.storageKey,
            originalFilename = d.originalFilename, mimeType = d.mimeType,
            sizeBytes = d.sizeBytes, version = d.version, published = d.published,
            createdAt = d.createdAt, updatedAt = d.updatedAt
        )
    }
}

// ── AuditLog ──────────────────────────────────────────────────────────────────

@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) val userId: UUID,
    @Column(name = "document_id") val documentId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) val action: AuditAction,
    @Column(columnDefinition = "TEXT") val details: String? = null,
    @Column(name = "occurred_at", nullable = false) val occurredAt: Instant = Instant.now()
) {
    fun toDomain() = AuditLog(
        id = id, userId = userId, documentId = documentId,
        action = action, details = details, occurredAt = occurredAt
    )

    companion object {
        fun fromDomain(l: AuditLog) = AuditLogEntity(
            id = l.id, userId = l.userId, documentId = l.documentId,
            action = l.action, details = l.details, occurredAt = l.occurredAt
        )
    }
}

// ── Notification ──────────────────────────────────────────────────────────────

@Entity
@Table(name = "notification")
class NotificationEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "document_id", nullable = false) val documentId: UUID,
    @Column(name = "recipient_email", nullable = false) val recipientEmail: String,
    @Column(nullable = false) val subject: String,
    @Column(columnDefinition = "TEXT", nullable = false) val body: String,
    @Column(nullable = false) val sent: Boolean = false,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
) {
    fun toDomain() = Notification(
        id = id, documentId = documentId, recipientEmail = recipientEmail,
        subject = subject, body = body, sent = sent, createdAt = createdAt
    )

    companion object {
        fun fromDomain(n: Notification) = NotificationEntity(
            id = n.id, documentId = n.documentId, recipientEmail = n.recipientEmail,
            subject = n.subject, body = n.body, sent = n.sent, createdAt = n.createdAt
        )
    }
}
