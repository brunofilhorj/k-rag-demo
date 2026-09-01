package com.bfilho.kragdemo.domain.service

import com.bfilho.kragdemo.domain.model.DocumentInfo
import com.bfilho.kragdemo.domain.port.out.KnowledgeBaseStorePort
import dev.langchain4j.data.segment.TextSegment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class IngestDocumentServiceTest {

    private val fakeStorePort = object : KnowledgeBaseStorePort {
        val storedDocs = mutableListOf<DocumentInfo>()
        val storedSegments = mutableListOf<List<TextSegment>>()

        override fun storeDocumentSegments(documentInfo: DocumentInfo, segments: List<TextSegment>) {
            storedDocs.add(documentInfo)
            storedSegments.add(segments)
        }

        override fun listDocuments(): List<DocumentInfo> = storedDocs

        override fun deleteDocument(documentId: String): Boolean {
            return storedDocs.removeIf { it.id == documentId }
        }
    }

    private val service = IngestDocumentService(fakeStorePort)

    @Test
    fun `should ingest text content successfully`() {
        val title = "Manual de Arquitetura"
        val text = "A arquitetura hexagonal separa as regras de negócio de portas e adaptadores de entrada e saída."

        val docInfo = service.ingestText(title, text)

        assertNotNull(docInfo.id)
        assertEquals(title, docInfo.title)
        assertEquals("TEXT", docInfo.sourceType)
        assertEquals(1, docInfo.chunkCount)
        assertEquals(1, fakeStorePort.storedDocs.size)
    }

    @Test
    fun `should ingest TXT file successfully`() {
        val fileName = "notas.txt"
        val content = "Notas sobre Spring Boot e LangChain4j."
        val inputStream = ByteArrayInputStream(content.toByteArray())

        val docInfo = service.ingestFile(fileName, inputStream, "text/plain", "Notas Tecnicas")

        assertNotNull(docInfo.id)
        assertEquals("Notas Tecnicas", docInfo.title)
        assertEquals("TXT", docInfo.sourceType)
        assertEquals(1, docInfo.chunkCount)
    }
}
