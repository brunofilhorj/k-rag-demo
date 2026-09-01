package com.bfilho.kragdemo.domain.service

import com.bfilho.kragdemo.domain.model.DocumentInfo
import com.bfilho.kragdemo.domain.port.`in`.IngestDocumentUseCase
import com.bfilho.kragdemo.domain.port.out.KnowledgeBaseStorePort
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class IngestDocumentService(
    private val knowledgeBaseStorePort: KnowledgeBaseStorePort
) : IngestDocumentUseCase {

    private val splitter = DocumentSplitters.recursive(500, 50)

    override fun ingestFile(
        fileName: String,
        inputStream: InputStream,
        contentType: String,
        customTitle: String?
    ): DocumentInfo {
        val title = customTitle?.takeIf { it.isNotBlank() } ?: fileName
        val documentId = UUID.randomUUID().toString()
        val sourceType = detectSourceType(fileName, contentType)

        val document: Document = when (sourceType) {
            "PDF" -> ApachePdfBoxDocumentParser().parse(inputStream)
            else -> {
                val text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                Document.from(text)
            }
        }

        return processAndStore(documentId, title, sourceType, document)
    }

    override fun ingestText(title: String, text: String): DocumentInfo {
        val documentId = UUID.randomUUID().toString()
        val document = Document.from(text)
        return processAndStore(documentId, title, "TEXT", document)
    }

    private fun processAndStore(
        documentId: String,
        title: String,
        sourceType: String,
        document: Document
    ): DocumentInfo {
        val rawSegments = splitter.split(document)
        val createdAt = Instant.now()

        val enrichedSegments = rawSegments.map { segment ->
            val metadata = Metadata.from(
                mapOf(
                    "document_id" to documentId,
                    "title" to title,
                    "source_type" to sourceType,
                    "created_at" to createdAt.toString()
                )
            )
            TextSegment.from(segment.text(), metadata)
        }

        val docInfo = DocumentInfo(
            id = documentId,
            title = title,
            sourceType = sourceType,
            createdAt = createdAt,
            chunkCount = enrichedSegments.size
        )

        knowledgeBaseStorePort.storeDocumentSegments(docInfo, enrichedSegments)
        return docInfo
    }

    private fun detectSourceType(fileName: String, contentType: String): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".pdf") || contentType.contains("pdf", ignoreCase = true) -> "PDF"
            lowerName.endsWith(".md") || lowerName.endsWith(".markdown") -> "MARKDOWN"
            else -> "TXT"
        }
    }
}
