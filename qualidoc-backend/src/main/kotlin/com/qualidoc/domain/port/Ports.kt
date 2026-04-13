package com.qualidoc.domain.port

import com.qualidoc.domain.model.User
import java.io.InputStream
import java.util.UUID

// ── Port JWT (génération / validation de tokens) ─────────────────────────────

interface JwtPort {
    fun generateAccessToken(user: User): String
    fun generateRefreshToken(): String
    fun refreshTokenExpirationSeconds(): Long
}

// ── Port stockage objet (MinIO / S3) ──────────────────────────────────────────

interface StoragePort {
    /**
     * Stocke un fichier et retourne la clé de stockage unique.
     * La clé est structurée : {establishmentId}/{documentType}/{uuid}.{ext}
     */
    fun store(
        inputStream: InputStream,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        establishmentId: UUID,
        documentType: String
    ): String

    /**
     * Retourne un flux de lecture du fichier.
     */
    fun retrieve(storageKey: String): InputStream

    /**
     * Supprime le fichier du stockage.
     */
    fun delete(storageKey: String)

    /**
     * Génère une URL pré-signée valide [expiresInSeconds] secondes.
     */
    fun generatePresignedUrl(storageKey: String, expiresInSeconds: Int = 3600): String
}

// ── Port recherche full-text (Elasticsearch) ──────────────────────────────────

data class DocumentSearchResult(
    val documentId: UUID,
    val title: String,
    val type: String,
    val establishmentId: UUID,
    val snippet: String?,
    val score: Float
)

interface SearchPort {
    fun index(documentId: UUID, title: String, type: String, content: String, establishmentId: UUID)
    fun search(query: String, establishmentIds: List<UUID>): List<DocumentSearchResult>
    fun delete(documentId: UUID)
}

// ── Port notifications (email SMTP) ───────────────────────────────────────────

data class EmailMessage(
    val to: String,
    val subject: String,
    val body: String,
    val isHtml: Boolean = true
)

interface NotificationPort {
    fun send(message: EmailMessage)
    fun sendBulk(messages: List<EmailMessage>)
}
