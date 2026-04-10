package com.qualidoc.application.usecase

import com.qualidoc.application.dto.*
import com.qualidoc.domain.model.*
import com.qualidoc.domain.port.SearchPort
import com.qualidoc.domain.port.StoragePort
import com.qualidoc.domain.repository.AuditLogRepository
import com.qualidoc.domain.repository.DocumentRepository
import com.qualidoc.domain.repository.EstablishmentRepository
import com.qualidoc.domain.repository.FolderRepository
import com.qualidoc.domain.repository.UserRepository
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// ── Upload d'un document ───────────────────────────────────────────────────────

@Service
class UploadDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val storagePort: StoragePort,
    private val searchPort: SearchPort,
    private val auditLogRepository: AuditLogRepository
) {
    @Transactional
    fun execute(command: UploadDocumentCommand): DocumentDto {
        val uploader = userRepository.findById(command.uploaderId)
            ?: throw IllegalArgumentException("Utilisateur introuvable : ${command.uploaderId}")

        check(uploader.canUpload()) { "L'utilisateur ${uploader.email} n'a pas le droit d'uploader des documents." }

        // Bufferisation du stream pour pouvoir l'utiliser deux fois (stockage + parsing)
        val bytes = command.contentStream.readBytes()

        // Stockage du fichier dans MinIO
        val storageKey = storagePort.store(
            inputStream = bytes.inputStream(),
            filename = command.filename,
            mimeType = command.mimeType,
            sizeBytes = command.sizeBytes,
            establishmentId = uploader.establishmentId,
            documentType = command.type.name
        )

        // Persistance des métadonnées
        val document = documentRepository.save(
            Document(
                title = command.title,
                type = command.type,
                uploaderId = command.uploaderId,
                establishmentId = uploader.establishmentId,
                folderId = command.folderId,
                storageKey = storageKey,
                originalFilename = command.filename,
                mimeType = command.mimeType,
                sizeBytes = command.sizeBytes
            )
        )

        // Extraction du contenu textuel pour les PDFs
        val pdfContent = if (command.mimeType == "application/pdf") extractPdfText(bytes) else null

        // Indexation dans Elasticsearch
        searchPort.index(
            documentId = document.id,
            title = document.title,
            type = document.type.name,
            content = pdfContent ?: document.title,
            establishmentId = document.establishmentId
        )

        // Audit RGPD
        auditLogRepository.save(
            AuditLog(
                userId = command.uploaderId,
                documentId = document.id,
                action = AuditAction.DOCUMENT_UPLOADED,
                details = "Fichier : ${command.filename}"
            )
        )

        return document.toDto()
    }
}

// ── Recherche full-text ───────────────────────────────────────────────────────

@Service
class SearchDocumentUseCase(
    private val userRepository: UserRepository,
    private val searchPort: SearchPort
) {
    fun execute(query: SearchDocumentQuery): List<SearchResultDto> {
        val user = userRepository.findById(query.requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable")

        // Un utilisateur ne voit que les documents de son établissement
        // (+ ceux partagés avec lui — filtré côté Elasticsearch)
        val results = searchPort.search(
            query = query.query,
            establishmentIds = listOf(user.establishmentId)
        )

        return results.map {
            SearchResultDto(
                documentId = it.documentId,
                title = it.title,
                type = it.type,
                establishmentId = it.establishmentId,
                snippet = it.snippet
            )
        }
    }
}

// ── Récupération des documents d'un établissement ────────────────────────────

@Service
class GetEstablishmentDocumentsUseCase(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val establishmentRepository: EstablishmentRepository
) {
    fun execute(requestingUserId: UUID, folderId: UUID?, all: Boolean = false): List<DocumentDto> {
        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable")

        val userEstablishment = establishmentRepository.findById(user.establishmentId)
            ?: throw IllegalArgumentException("Établissement introuvable")

        val groupementEstablishmentIds = establishmentRepository
            .findByGroupementId(userEstablishment.groupementId)
            .map { it.id }

        val documents = when {
            all     -> documentRepository.findByEstablishmentIdIn(groupementEstablishmentIds)
            folderId != null -> documentRepository.findByEstablishmentIdInAndFolderId(groupementEstablishmentIds, folderId)
            else    -> documentRepository.findByEstablishmentIdInAndFolderIdIsNull(groupementEstablishmentIds)
        }

        return documents.sortedByDescending { it.createdAt }.map { it.toDto() }
    }
}

// ── Téléchargement d'un document ─────────────────────────────────────────────

data class DownloadResult(
    val inputStream: java.io.InputStream,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long
)

@Service
class DownloadDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val storagePort: StoragePort,
    private val auditLogRepository: AuditLogRepository
) {
    fun execute(documentId: UUID, requestingUserId: UUID): DownloadResult {
        val document = documentRepository.findById(documentId)
            ?: throw IllegalArgumentException("Document introuvable : $documentId")

        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable : $requestingUserId")

        check(user.establishmentId == document.establishmentId) {
            "L'utilisateur ${user.email} n'a pas accès aux documents d'un autre groupement."
        }

        auditLogRepository.save(
            AuditLog(
                userId = requestingUserId,
                documentId = document.id,
                action = AuditAction.DOCUMENT_DOWNLOADED,
                details = "Fichier : ${document.originalFilename}"
            )
        )

        return DownloadResult(
            inputStream = storagePort.retrieve(document.storageKey),
            filename = document.originalFilename,
            mimeType = document.mimeType,
            sizeBytes = document.sizeBytes
        )
    }
}

// ── Suppression d'un document ─────────────────────────────────────────────────

@Service
class DeleteDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val storagePort: StoragePort,
    private val searchPort: SearchPort,
    private val auditLogRepository: AuditLogRepository
) {
    @Transactional
    fun execute(documentId: UUID, requestingUserId: UUID) {
        val document = documentRepository.findById(documentId)
            ?: throw IllegalArgumentException("Document introuvable : $documentId")

        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable : $requestingUserId")

        check(user.establishmentId == document.establishmentId) {
            "L'utilisateur ${user.email} ne peut supprimer que les documents de son propre établissement."
        }

        storagePort.delete(document.storageKey)
        searchPort.delete(document.id)
        documentRepository.delete(document.id)

        auditLogRepository.save(
            AuditLog(
                userId = requestingUserId,
                documentId = document.id,
                action = AuditAction.DOCUMENT_DELETED,
                details = "Fichier : ${document.originalFilename}"
            )
        )
    }
}

// ── Renommage d'un document ───────────────────────────────────────────────────

@Service
class RenameDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val searchPort: SearchPort,
    private val auditLogRepository: AuditLogRepository
) {
    @Transactional
    fun execute(documentId: UUID, newTitle: String, requestingUserId: UUID): DocumentDto {
        val document = documentRepository.findById(documentId)
            ?: throw IllegalArgumentException("Document introuvable : $documentId")

        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable : $requestingUserId")

        check(user.establishmentId == document.establishmentId) {
            "L'utilisateur ${user.email} ne peut renommer que les documents de son propre établissement."
        }

        val updated = documentRepository.save(document.copy(title = newTitle))

        searchPort.index(
            documentId = updated.id,
            title = updated.title,
            type = updated.type.name,
            content = updated.title,
            establishmentId = updated.establishmentId
        )

        auditLogRepository.save(
            AuditLog(
                userId = requestingUserId,
                documentId = updated.id,
                action = AuditAction.DOCUMENT_UPLOADED,
                details = "Renommé en : $newTitle"
            )
        )

        return updated.toDto()
    }
}

// ── PDF text extraction ───────────────────────────────────────────────────────

private fun extractPdfText(bytes: ByteArray): String? =
    try {
        Loader.loadPDF(bytes).use { doc ->
            PDFTextStripper().getText(doc).trim().takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) { null }

// ── Déplacement d'un document ────────────────────────────────────────────────

@Service
class MoveDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun execute(documentId: UUID, targetFolderId: UUID?, requestingUserId: UUID): DocumentDto {
        val document = documentRepository.findById(documentId)
            ?: throw IllegalArgumentException("Document introuvable : $documentId")
        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable : $requestingUserId")
        check(user.establishmentId == document.establishmentId) {
            "L'utilisateur ${user.email} ne peut déplacer que les documents de son établissement."
        }
        return documentRepository.save(document.copy(folderId = targetFolderId)).toDto()
    }
}

// ── Dossiers ─────────────────────────────────────────────────────────────────

@Service
class GetFoldersUseCase(
    private val folderRepository: FolderRepository,
    private val userRepository: UserRepository,
    private val establishmentRepository: EstablishmentRepository
) {
    fun execute(requestingUserId: UUID): List<FolderDto> {
        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable")
        val establishment = establishmentRepository.findById(user.establishmentId)
            ?: throw IllegalArgumentException("Établissement introuvable")
        return folderRepository.findByGroupementId(establishment.groupementId)
            .map { it.toDto() }
    }
}

@Service
class CreateFolderUseCase(
    private val folderRepository: FolderRepository,
    private val userRepository: UserRepository,
    private val establishmentRepository: EstablishmentRepository
) {
    fun execute(name: String, parentId: UUID?, requestingUserId: UUID): FolderDto {
        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable")
        val establishment = establishmentRepository.findById(user.establishmentId)
            ?: throw IllegalArgumentException("Établissement introuvable")
        return folderRepository.save(
            Folder(name = name, parentId = parentId, groupementId = establishment.groupementId)
        ).toDto()
    }
}

@Service
class RenameFolderUseCase(
    private val folderRepository: FolderRepository,
    private val userRepository: UserRepository,
    private val establishmentRepository: EstablishmentRepository
) {
    fun execute(folderId: UUID, newName: String, requestingUserId: UUID): FolderDto {
        val folder = folderRepository.findById(folderId)
            ?: throw IllegalArgumentException("Dossier introuvable : $folderId")
        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable")
        val establishment = establishmentRepository.findById(user.establishmentId)
            ?: throw IllegalArgumentException("Établissement introuvable")
        check(folder.groupementId == establishment.groupementId) {
            "L'utilisateur ne peut renommer que les dossiers de son groupement."
        }
        return folderRepository.save(folder.copy(name = newName)).toDto()
    }
}

@Service
class DeleteFolderUseCase(
    private val folderRepository: FolderRepository,
    private val documentRepository: DocumentRepository,
    private val storagePort: StoragePort,
    private val searchPort: SearchPort,
    private val userRepository: UserRepository,
    private val establishmentRepository: EstablishmentRepository
) {
    @Transactional
    fun execute(folderId: UUID, requestingUserId: UUID) {
        val folder = folderRepository.findById(folderId)
            ?: throw IllegalArgumentException("Dossier introuvable : $folderId")
        val user = userRepository.findById(requestingUserId)
            ?: throw IllegalArgumentException("Utilisateur introuvable")
        val establishment = establishmentRepository.findById(user.establishmentId)
            ?: throw IllegalArgumentException("Établissement introuvable")
        check(folder.groupementId == establishment.groupementId) {
            "L'utilisateur ne peut supprimer que les dossiers de son groupement."
        }

        val allFolders = folderRepository.findByGroupementId(folder.groupementId)

        // Collecte récursive de tous les IDs de dossiers à supprimer
        val toDelete = mutableSetOf<UUID>()
        fun collect(id: UUID) {
            toDelete.add(id)
            allFolders.filter { it.parentId == id }.forEach { collect(it.id) }
        }
        collect(folderId)

        // Suppression des documents contenus (MinIO + Elasticsearch + DB)
        documentRepository.findByFolderIdIn(toDelete.toList()).forEach { doc ->
            storagePort.delete(doc.storageKey)
            searchPort.delete(doc.id)
            documentRepository.delete(doc.id)
        }

        // Suppression du dossier racine (CASCADE supprime les sous-dossiers en DB)
        folderRepository.delete(folderId)
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────────

fun Folder.toDto() = FolderDto(
    id = id, name = name, parentId = parentId, groupementId = groupementId, createdAt = createdAt
)

fun Document.toDto() = DocumentDto(
    id = id,
    title = title,
    type = type,
    typeLabel = type.label,
    uploaderId = uploaderId,
    establishmentId = establishmentId,
    folderId = folderId,
    originalFilename = originalFilename,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    version = version,
    createdAt = createdAt
)
