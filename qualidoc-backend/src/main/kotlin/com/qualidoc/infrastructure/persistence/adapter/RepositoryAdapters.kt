package com.qualidoc.infrastructure.persistence.adapter

import com.qualidoc.domain.model.*
import com.qualidoc.domain.repository.*
import com.qualidoc.infrastructure.persistence.entity.*
import com.qualidoc.infrastructure.persistence.repository.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

// Chaque adaptateur implémente une interface du domaine (DIP).
// Le domaine ne connaît ni JPA ni Spring Data.

@Component
class FolderRepositoryAdapter(
    private val jpa: FolderJpaRepository
) : FolderRepository {
    override fun save(folder: Folder) = jpa.save(FolderEntity.fromDomain(folder)).toDomain()
    override fun findById(id: UUID) = jpa.findById(id).map { it.toDomain() }.orElse(null)
    override fun findByGroupementId(groupementId: UUID) =
        jpa.findAllByGroupementId(groupementId).map { it.toDomain() }
    override fun delete(id: UUID) = jpa.deleteById(id)
}

@Component
class EstablishmentRepositoryAdapter(
    private val jpa: EstablishmentJpaRepository
) : EstablishmentRepository {
    override fun save(establishment: Establishment) =
        jpa.save(EstablishmentEntity.fromDomain(establishment)).toDomain()

    override fun findById(id: UUID) = jpa.findById(id).map { it.toDomain() }.orElse(null)
    override fun findAll() = jpa.findAll().map { it.toDomain() }
    override fun findByCode(code: String) = jpa.findByCode(code)?.toDomain()
    override fun existsByCode(code: String) = jpa.existsByCode(code)
    override fun findByGroupementId(groupementId: UUID) =
        jpa.findAllByGroupementId(groupementId).map { it.toDomain() }
}

@Component
class UserRepositoryAdapter(
    private val jpa: UserJpaRepository
) : UserRepository {
    override fun save(user: User) = jpa.save(UserEntity.fromDomain(user)).toDomain()
    override fun findById(id: UUID) = jpa.findById(id).map { it.toDomain() }.orElse(null)
    override fun findByEmail(email: String) = jpa.findByEmail(email)?.toDomain()
    override fun findByEstablishmentId(establishmentId: UUID) =
        jpa.findAllByEstablishmentId(establishmentId).map { it.toDomain() }
    override fun findAll() = jpa.findAll().map { it.toDomain() }
    override fun deleteById(id: UUID) = jpa.deleteById(id)
}

@Component
class DocumentRepositoryAdapter(
    private val jpa: DocumentJpaRepository
) : DocumentRepository {
    override fun save(document: Document) = jpa.save(DocumentEntity.fromDomain(document)).toDomain()
    override fun findById(id: UUID) = jpa.findById(id).map { it.toDomain() }.orElse(null)
    override fun findByEstablishmentId(establishmentId: UUID) =
        jpa.findAllByEstablishmentId(establishmentId).map { it.toDomain() }
    override fun findByEstablishmentIdIn(establishmentIds: List<UUID>) =
        jpa.findAllByEstablishmentIdIn(establishmentIds).map { it.toDomain() }
    override fun findByEstablishmentIdInAndFolderIdIsNull(establishmentIds: List<UUID>) =
        jpa.findAllByEstablishmentIdInAndFolderIdIsNull(establishmentIds).map { it.toDomain() }
    override fun findByEstablishmentIdInAndFolderId(establishmentIds: List<UUID>, folderId: UUID) =
        jpa.findAllByEstablishmentIdInAndFolderId(establishmentIds, folderId).map { it.toDomain() }
    override fun findByFolderIdIn(folderIds: List<UUID>) =
        jpa.findAllByFolderIdIn(folderIds).map { it.toDomain() }
    override fun delete(id: UUID) = jpa.deleteById(id)
    override fun existsById(id: UUID) = jpa.existsById(id)
}

@Component
class AuditLogRepositoryAdapter(
    private val jpa: AuditLogJpaRepository
) : AuditLogRepository {
    override fun save(log: AuditLog) = jpa.save(AuditLogEntity.fromDomain(log)).toDomain()
    override fun findByDocumentId(documentId: UUID) =
        jpa.findAllByDocumentId(documentId).map { it.toDomain() }
    override fun findByUserId(userId: UUID) =
        jpa.findAllByUserId(userId).map { it.toDomain() }
}

@Component
class NotificationRepositoryAdapter(
    private val jpa: NotificationJpaRepository
) : NotificationRepository {
    override fun save(notification: Notification) =
        jpa.save(NotificationEntity.fromDomain(notification)).toDomain()
    override fun findPending() = jpa.findAllBySentFalse().map { it.toDomain() }
    override fun markAsSent(id: UUID) = jpa.markAsSent(id)
}

@Component
class RefreshTokenRepositoryAdapter(
    private val jpa: RefreshTokenJpaRepository
) : RefreshTokenRepository {
    override fun save(token: RefreshToken) =
        jpa.save(RefreshTokenEntity.fromDomain(token)).toDomain()
    override fun findByTokenHash(tokenHash: String) =
        jpa.findByTokenHash(tokenHash)?.toDomain()
    @Transactional
    override fun revokeAllForUser(userId: UUID) = jpa.revokeAllByUserId(userId)
    @Transactional
    override fun deleteExpired() = jpa.deleteExpired(LocalDateTime.now())
}
