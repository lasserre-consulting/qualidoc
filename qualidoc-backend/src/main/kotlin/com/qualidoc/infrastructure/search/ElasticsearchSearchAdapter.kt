package com.qualidoc.infrastructure.search

import co.elastic.clients.elasticsearch._types.query_dsl.Query
import com.qualidoc.domain.port.DocumentSearchResult
import com.qualidoc.domain.port.SearchPort
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

// ── Document Elasticsearch ────────────────────────────────────────────────────

@Document(indexName = "qualidoc_documents")
data class DocumentIndex(
    @org.springframework.data.annotation.Id
    val id: String,

    @Field(type = FieldType.Text, analyzer = "french")
    val title: String,

    @Field(type = FieldType.Keyword)
    val type: String,

    @Field(type = FieldType.Text, analyzer = "french")
    val content: String,

    @Field(type = FieldType.Keyword)
    val establishmentId: String
)

// ── Repository Elasticsearch ──────────────────────────────────────────────────

@Repository
interface DocumentIndexRepository : ElasticsearchRepository<DocumentIndex, String>

// ── Adaptateur ────────────────────────────────────────────────────────────────

@Component
class ElasticsearchSearchAdapter(
    private val documentIndexRepository: DocumentIndexRepository,
    private val elasticsearchOperations: ElasticsearchOperations
) : SearchPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun index(
        documentId: UUID,
        title: String,
        type: String,
        content: String,
        establishmentId: UUID
    ) {
        val doc = DocumentIndex(
            id = documentId.toString(),
            title = title,
            type = type,
            content = content,
            establishmentId = establishmentId.toString()
        )
        documentIndexRepository.save(doc)
        log.debug("Document indexé dans Elasticsearch : $documentId")
    }

    override fun search(query: String, establishmentIds: List<UUID>): List<DocumentSearchResult> {
        val establishmentStrings = establishmentIds.map { it.toString() }

        // NativeQuery : bool must [ multiMatch(title+content) + terms(establishmentId) ]
        val esQuery = Query.of { q ->
            q.bool { b ->
                b.must { m ->
                    m.multiMatch { mm ->
                        mm.query(query).fields("title", "content")
                    }
                }
                b.must { m ->
                    m.terms { t ->
                        t.field("establishmentId")
                            .terms { tv ->
                                tv.value(establishmentStrings.map {
                                    co.elastic.clients.elasticsearch._types.FieldValue.of(it)
                                })
                            }
                    }
                }
            }
        }

        val searchQuery = NativeQuery.builder().withQuery(esQuery).build()
        val hits = elasticsearchOperations.search(searchQuery, DocumentIndex::class.java)

        return hits.map { hit: SearchHit<DocumentIndex> ->
            DocumentSearchResult(
                documentId = UUID.fromString(hit.content.id),
                title = hit.content.title,
                type = hit.content.type,
                establishmentId = UUID.fromString(hit.content.establishmentId),
                snippet = extractSnippet(hit.content.content, query),
                score = hit.score
            )
        }.toList()
    }

    override fun delete(documentId: UUID) {
        documentIndexRepository.deleteById(documentId.toString())
        log.debug("Document supprimé de l'index Elasticsearch : $documentId")
    }

    private fun extractSnippet(content: String, query: String): String? {
        if (content.isBlank()) return null
        val idx = content.lowercase().indexOf(query.lowercase())
        if (idx == -1) return content.take(150) + "…"
        val start = maxOf(0, idx - 60)
        val end = minOf(content.length, idx + query.length + 90)
        return (if (start > 0) "…" else "") + content.substring(start, end) + (if (end < content.length) "…" else "")
    }
}
