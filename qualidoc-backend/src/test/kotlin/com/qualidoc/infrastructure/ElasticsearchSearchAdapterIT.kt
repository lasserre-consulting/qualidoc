package com.qualidoc.infrastructure

import com.qualidoc.AbstractIntegrationTest
import com.qualidoc.domain.port.SearchPort
import com.qualidoc.infrastructure.search.DocumentIndex
import com.qualidoc.infrastructure.search.DocumentIndexRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import java.util.UUID

private val ETABLISSEMENT_A = UUID.fromString("11111111-0000-0000-0000-000000000001")
private val ETABLISSEMENT_B = UUID.fromString("11111111-0000-0000-0000-000000000002")

class ElasticsearchSearchAdapterIT(
    @param:Autowired private val searchPort: SearchPort,
    @param:Autowired private val documentIndexRepository: DocumentIndexRepository,
    @param:Autowired private val elasticsearchOperations: ElasticsearchOperations
) : AbstractIntegrationTest() {

    @BeforeEach
    fun cleanIndex() {
        documentIndexRepository.deleteAll()
        refreshIndex()
    }

    @Test
    fun `search par titre retourne le document indexé`() {
        val docId = UUID.randomUUID()
        searchPort.index(docId, "Procédure de stérilisation", "PROCEDURE", "contenu vide", ETABLISSEMENT_A)
        refreshIndex()

        val results = searchPort.search("stérilisation", listOf(ETABLISSEMENT_A))

        assertEquals(1, results.size)
        assertEquals(docId, results[0].documentId)
        assertEquals("Procédure de stérilisation", results[0].title)
    }

    @Test
    fun `search par contenu retourne le document indexé`() {
        val docId = UUID.randomUUID()
        val pdfContent = "Nettoyage des surfaces avec solution hydroalcoolique avant chaque intervention chirurgicale"
        searchPort.index(docId, "Protocole bloc opératoire", "PROCEDURE", pdfContent, ETABLISSEMENT_A)
        refreshIndex()

        val results = searchPort.search("hydroalcoolique", listOf(ETABLISSEMENT_A))

        assertEquals(1, results.size)
        assertEquals(docId, results[0].documentId)
        assertNotNull(results[0].snippet)
        assertTrue(results[0].snippet!!.contains("hydroalcoolique"))
    }

    @Test
    fun `search est isolé par établissement`() {
        searchPort.index(UUID.randomUUID(), "Fiche de sécurité incendie", "SECURITE", "procédure évacuation", ETABLISSEMENT_A)
        refreshIndex()

        // ETABLISSEMENT_B ne doit pas voir les documents de A
        val resultsB = searchPort.search("incendie", listOf(ETABLISSEMENT_B))
        assertTrue(resultsB.isEmpty())

        // ETABLISSEMENT_A les voit bien
        val resultsA = searchPort.search("incendie", listOf(ETABLISSEMENT_A))
        assertEquals(1, resultsA.size)
    }

    @Test
    fun `search sur plusieurs établissements retourne les documents des deux`() {
        val docA = UUID.randomUUID()
        val docB = UUID.randomUUID()
        searchPort.index(docA, "Protocole anesthésie", "PROCEDURE", "induction anesthésique", ETABLISSEMENT_A)
        searchPort.index(docB, "Protocole anesthésie générale", "PROCEDURE", "induction anesthésique", ETABLISSEMENT_B)
        refreshIndex()

        val results = searchPort.search("anesthésie", listOf(ETABLISSEMENT_A, ETABLISSEMENT_B))

        assertEquals(2, results.size)
        assertTrue(results.any { it.documentId == docA })
        assertTrue(results.any { it.documentId == docB })
    }

    @Test
    fun `delete supprime le document de l'index`() {
        val docId = UUID.randomUUID()
        searchPort.index(docId, "Document à supprimer", "PROCEDURE", "contenu test", ETABLISSEMENT_A)
        refreshIndex()

        searchPort.delete(docId)
        refreshIndex()

        val results = searchPort.search("supprimer", listOf(ETABLISSEMENT_A))
        assertTrue(results.isEmpty())
    }

    private fun refreshIndex() {
        elasticsearchOperations.indexOps(DocumentIndex::class.java).refresh()
    }
}
