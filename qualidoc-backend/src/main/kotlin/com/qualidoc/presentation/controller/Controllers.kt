package com.qualidoc.presentation.controller

import com.qualidoc.application.dto.DocumentDto
import com.qualidoc.application.dto.EstablishmentDto
import com.qualidoc.application.dto.SearchDocumentQuery
import com.qualidoc.application.dto.SearchResultDto
import com.qualidoc.application.dto.UploadDocumentCommand
import com.qualidoc.application.dto.FolderDto
import com.qualidoc.application.usecase.*
import com.qualidoc.domain.model.DocumentType
import com.qualidoc.infrastructure.security.AuthenticatedUser
import org.springframework.core.io.InputStreamResource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

// ── Documents ─────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Gestion des documents qualité")
@SecurityRequirement(name = "bearerAuth")
class DocumentController(
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val getEstablishmentDocumentsUseCase: GetEstablishmentDocumentsUseCase,
    private val downloadDocumentUseCase: DownloadDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val renameDocumentUseCase: RenameDocumentUseCase,
    private val moveDocumentUseCase: MoveDocumentUseCase
) {

    @GetMapping
    @Operation(summary = "Liste les documents accessibles par l'utilisateur connecté")
    fun listDocuments(
        @RequestParam(required = false) folderId: UUID?,
        @RequestParam(required = false, defaultValue = "false") all: Boolean,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<List<DocumentDto>> {
        return ResponseEntity.ok(getEstablishmentDocumentsUseCase.execute(user.id, folderId, all))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Uploade un nouveau document (éditeurs uniquement)")
    fun uploadDocument(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("title") title: String,
        @RequestParam("type") type: DocumentType,
        @RequestParam("folderId", required = false) folderId: UUID?,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<DocumentDto> {
        val command = UploadDocumentCommand(
            title = title,
            type = type,
            uploaderId = user.id,
            folderId = folderId,
            filename = file.originalFilename ?: file.name,
            mimeType = file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
            sizeBytes = file.size,
            contentStream = file.inputStream
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadDocumentUseCase.execute(command))
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Télécharge le fichier binaire d'un document")
    fun downloadDocument(
        @PathVariable documentId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<InputStreamResource> {
        val result = downloadDocumentUseCase.execute(documentId, user.id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.mimeType))
            .contentLength(result.sizeBytes)
            .header("Content-Disposition", "attachment; filename=\"${result.filename}\"")
            .body(InputStreamResource(result.inputStream))
    }

    @PatchMapping("/{documentId}/move")
    @Operation(summary = "Déplace un document dans un dossier (éditeurs uniquement)")
    fun moveDocument(
        @PathVariable documentId: UUID,
        @RequestBody body: Map<String, String?>,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<DocumentDto> {
        val targetFolderId = body["folderId"]?.let { UUID.fromString(it) }
        return ResponseEntity.ok(moveDocumentUseCase.execute(documentId, targetFolderId, user.id))
    }

    @PatchMapping("/{documentId}/rename")
    @Operation(summary = "Renomme un document (éditeurs uniquement)")
    fun renameDocument(
        @PathVariable documentId: UUID,
        @RequestBody body: Map<String, String>,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<DocumentDto> {
        val newTitle = body["title"]?.takeIf { it.isNotBlank() }
            ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(renameDocumentUseCase.execute(documentId, newTitle, user.id))
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un document (éditeurs uniquement)")
    fun deleteDocument(
        @PathVariable documentId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<Void> {
        deleteDocumentUseCase.execute(documentId, user.id)
        return ResponseEntity.noContent().build()
    }

}

// ── Recherche ─────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Recherche", description = "Recherche full-text dans les documents")
@SecurityRequirement(name = "bearerAuth")
class SearchController(
    private val searchDocumentUseCase: SearchDocumentUseCase
) {

    @GetMapping
    @Operation(summary = "Recherche full-text dans les documents accessibles")
    fun search(
        @RequestParam @NotBlank q: String,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<List<SearchResultDto>> {
        val results = searchDocumentUseCase.execute(
            SearchDocumentQuery(query = q, requestingUserId = user.id)
        )
        return ResponseEntity.ok(results)
    }
}

// ── Établissements ────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/establishments")
@Tag(name = "Établissements", description = "Gestion des établissements")
@SecurityRequirement(name = "bearerAuth")
class EstablishmentController(
    private val listActiveEstablishmentsUseCase: ListActiveEstablishmentsUseCase
) {

    @GetMapping
    @Operation(summary = "Liste tous les établissements actifs")
    fun listAll(): ResponseEntity<List<EstablishmentDto>> =
        ResponseEntity.ok(listActiveEstablishmentsUseCase.execute())
}

// ── Dossiers ─────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/folders")
@Tag(name = "Dossiers", description = "Gestion de l'arborescence de dossiers")
@SecurityRequirement(name = "bearerAuth")
class FolderController(
    private val getFoldersUseCase: GetFoldersUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val renameFolderUseCase: RenameFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase
) {
    @GetMapping
    fun listFolders(@AuthenticationPrincipal user: AuthenticatedUser): ResponseEntity<List<FolderDto>> =
        ResponseEntity.ok(getFoldersUseCase.execute(user.id))

    @PostMapping
    fun createFolder(
        @RequestBody body: Map<String, String?>,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<FolderDto> {
        val name = body["name"]?.takeIf { it.isNotBlank() }
            ?: return ResponseEntity.badRequest().build()
        val parentId = body["parentId"]?.let { UUID.fromString(it) }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createFolderUseCase.execute(name, parentId, user.id))
    }

    @PatchMapping("/{folderId}/rename")
    fun renameFolder(
        @PathVariable folderId: UUID,
        @RequestBody body: Map<String, String>,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<FolderDto> {
        val name = body["name"]?.takeIf { it.isNotBlank() }
            ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(renameFolderUseCase.execute(folderId, name, user.id))
    }

    @DeleteMapping("/{folderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFolder(
        @PathVariable folderId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<Void> {
        deleteFolderUseCase.execute(folderId, user.id)
        return ResponseEntity.noContent().build()
    }
}

// ── Health check ──────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/health")
class HealthController {
    @GetMapping
    fun health() = mapOf("status" to "UP", "service" to "QualiDoc Backend")
}
