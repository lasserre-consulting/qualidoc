package com.qualidoc.infrastructure.persistence.repository

import com.qualidoc.infrastructure.persistence.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FolderJpaRepository : JpaRepository<FolderEntity, UUID> {
    fun findAllByGroupementId(groupementId: UUID): List<FolderEntity>
}

@Repository
interface EstablishmentJpaRepository : JpaRepository<EstablishmentEntity, UUID> {
    fun findByCode(code: String): EstablishmentEntity?
    fun existsByCode(code: String): Boolean
    fun findAllByGroupementId(groupementId: UUID): List<EstablishmentEntity>
}

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findAllByEstablishmentId(establishmentId: UUID): List<UserEntity>
}

@Repository
interface DocumentJpaRepository : JpaRepository<DocumentEntity, UUID> {
    fun findAllByEstablishmentId(establishmentId: UUID): List<DocumentEntity>
    fun findAllByEstablishmentIdIn(establishmentIds: List<UUID>): List<DocumentEntity>
    fun findAllByEstablishmentIdInAndFolderIdIsNull(establishmentIds: List<UUID>): List<DocumentEntity>
    fun findAllByEstablishmentIdInAndFolderId(establishmentIds: List<UUID>, folderId: UUID): List<DocumentEntity>
    fun findAllByFolderIdIn(folderIds: List<UUID>): List<DocumentEntity>
}

@Repository
interface AuditLogJpaRepository : JpaRepository<AuditLogEntity, UUID> {
    fun findAllByDocumentId(documentId: UUID): List<AuditLogEntity>
    fun findAllByUserId(userId: UUID): List<AuditLogEntity>
}

@Repository
interface NotificationJpaRepository : JpaRepository<NotificationEntity, UUID> {
    fun findAllBySentFalse(): List<NotificationEntity>

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.sent = true WHERE n.id = :id")
    fun markAsSent(@Param("id") id: UUID)
}
