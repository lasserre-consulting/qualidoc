package com.qualidoc.infrastructure.storage

import com.qualidoc.domain.port.StoragePort
import io.minio.*
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class MinioStorageAdapter(
    private val minioClient: MinioClient,
    @param:Value("\${minio.bucket}") private val bucket: String
) : StoragePort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun store(
        inputStream: InputStream,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        establishmentId: java.util.UUID,
        documentType: String
    ): String {
        ensureBucketExists()

        val extension = filename.substringAfterLast(".", "bin")
        val storageKey = "$establishmentId/$documentType/${UUID.randomUUID()}.$extension"

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(storageKey)
                .stream(inputStream, sizeBytes, -1)
                .contentType(mimeType)
                .build()
        )

        log.info("Fichier stocké : $storageKey ($sizeBytes octets)")
        return storageKey
    }

    override fun retrieve(storageKey: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(storageKey)
                .build()
        )
    }

    override fun delete(storageKey: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket)
                .`object`(storageKey)
                .build()
        )
        log.info("Fichier supprimé : $storageKey")
    }

    override fun generatePresignedUrl(storageKey: String, expiresInSeconds: Int): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .`object`(storageKey)
                .expiry(expiresInSeconds, TimeUnit.SECONDS)
                .build()
        )
    }

    private fun ensureBucketExists() {
        val exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        )
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            log.info("Bucket MinIO créé : $bucket")
        }
    }
}
