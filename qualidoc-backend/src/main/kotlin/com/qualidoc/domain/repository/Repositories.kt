package com.qualidoc.domain.repository

import com.qualidoc.domain.model.*
import java.util.UUID

// Le domaine définit les contrats — l'infrastructure les implémente.
// Aucune dépendance vers JPA ou toute autre technologie ici.

interface FolderRepository {
    fun save(folder: Folder): Folder
    fun findById(id: UUID): Folder?
    fun findByGroupementId(groupementId: UUID): List<Folder>
    fun delete(id: UUID)
}

interface DocumentRepository {
    fun save(document: Document): Document
    fun findById(id: UUID): Document?
    fun findByEstablishmentId(establishmentId: UUID): List<Document>
    fun findByEstablishmentIdIn(establishmentIds: List<UUID>): List<Document>
    fun findByEstablishmentIdInAndFolderIdIsNull(establishmentIds: List<UUID>): List<Document>
    fun findByEstablishmentIdInAndFolderId(establishmentIds: List<UUID>, folderId: UUID): List<Document>
    fun findByFolderIdIn(folderIds: List<UUID>): List<Document>
    fun delete(id: UUID)
    fun existsById(id: UUID): Boolean
}

interface EstablishmentRepository {
    fun save(establishment: Establishment): Establishment
    fun findById(id: UUID): Establishment?
    fun findAll(): List<Establishment>
    fun findByCode(code: String): Establishment?
    fun existsByCode(code: String): Boolean
    fun findByGroupementId(groupementId: UUID): List<Establishment>
}

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findByEstablishmentId(establishmentId: UUID): List<User>
}

interface AuditLogRepository {
    fun save(log: AuditLog): AuditLog
    fun findByDocumentId(documentId: UUID): List<AuditLog>
    fun findByUserId(userId: UUID): List<AuditLog>
}

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findPending(): List<Notification>
    fun markAsSent(id: UUID)
}
