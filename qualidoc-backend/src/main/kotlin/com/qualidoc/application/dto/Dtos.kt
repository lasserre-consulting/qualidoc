package com.qualidoc.application.dto

import com.qualidoc.domain.model.DocumentType
import com.qualidoc.domain.model.UserRole
import java.time.Instant
import java.util.UUID

// ── Commandes (entrées des use cases) ────────────────────────────────────────

data class UploadDocumentCommand(
    val title: String,
    val type: DocumentType,
    val uploaderId: UUID,
    val folderId: UUID? = null,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentStream: java.io.InputStream
)

data class SearchDocumentQuery(
    val query: String,
    val requestingUserId: UUID
)

// ── Résultats (sorties des use cases) ─────────────────────────────────────────

data class FolderDto(
    val id: UUID,
    val name: String,
    val parentId: UUID?,
    val groupementId: UUID,
    val createdAt: Instant
)

data class DocumentDto(
    val id: UUID,
    val title: String,
    val type: DocumentType,
    val typeLabel: String,
    val uploaderId: UUID,
    val establishmentId: UUID,
    val folderId: UUID?,
    val originalFilename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val version: Int,
    val createdAt: Instant
)

data class EstablishmentDto(
    val id: UUID,
    val name: String,
    val code: String,
    val active: Boolean
)

data class UserDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val establishmentId: UUID
)

data class SearchResultDto(
    val documentId: UUID,
    val title: String,
    val type: String,
    val establishmentId: UUID,
    val snippet: String?
)
