package com.bfilho.kragdemo.adapter.out.langchain4j

import com.bfilho.kragdemo.adapter.out.langchain4j.prompt.PromptBuilder
import com.bfilho.kragdemo.adapter.out.langchain4j.prompt.PromptVariant
import com.bfilho.kragdemo.domain.model.Answer
import com.bfilho.kragdemo.domain.model.DocumentInfo
import com.bfilho.kragdemo.domain.model.Question
import com.bfilho.kragdemo.domain.port.out.ExternalContextPort
import com.bfilho.kragdemo.domain.port.out.KnowledgeBaseStorePort
import com.bfilho.kragdemo.domain.port.out.RagEnginePort
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.filter.Filter
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class LangChain4jAdapter(
    @Value("\${initial-document.pdf-path:#{null}}") private val pdfPath: String?,
    private val embeddingModel: OllamaEmbeddingModel,
    private val embeddingStore: InMemoryEmbeddingStore<TextSegment>,
    private val assistant: KnowledgeAssistant,
    private val promptBuilder: PromptBuilder,
    private val externalContextPort: ExternalContextPort,
    @Value("\${prompt.variant:BASELINE}")
    private val promptVariant: PromptVariant
) : RagEnginePort, KnowledgeBaseStorePort {

    private val log = LoggerFactory.getLogger(LangChain4jAdapter::class.java)
    private val documentsRegistry = ConcurrentHashMap<String, DocumentInfo>()

    @PostConstruct
    fun initIndexing() {
        if (pdfPath.isNullOrBlank()) {
            log.info("No initial PDF path specified. Skipping initial PDF ingestion.")
            return
        }

        log.info("Starting initial document ingestion from PDF: {}", pdfPath)
        try {
            val parser = ApachePdfBoxDocumentParser()
            val fileStream = FileInputStream(pdfPath)
            val document = parser.parse(fileStream)
            log.info("Initial PDF loaded successfully ({} characters)", document.text().length)

            val splitter = DocumentSplitters.recursive(500, 50)
            val rawSegments = splitter.split(document)
            val docId = "initial-pdf"
            val createdAt = Instant.now()

            val enrichedSegments = rawSegments.map { segment ->
                val metadata = Metadata.from(
                    mapOf(
                        "document_id" to docId,
                        "title" to "Initial Document (PDF)",
                        "source_type" to "PDF",
                        "created_at" to createdAt.toString()
                    )
                )
                TextSegment.from(segment.text(), metadata)
            }

            val docInfo = DocumentInfo(
                id = docId,
                title = "Initial Document (PDF)",
                sourceType = "PDF",
                createdAt = createdAt,
                chunkCount = enrichedSegments.size
            )

            storeDocumentSegments(docInfo, enrichedSegments)
            log.info("Initial PDF indexed into InMemoryEmbeddingStore with id: {}", docId)
        } catch (e: FileNotFoundException) {
            log.warn("PDF file not found at path '{}'. Ingestion skipped. Error: {}", pdfPath, e.message)
        } catch (e: Exception) {
            log.error("Failed to ingest initial PDF into embedding store", e)
        }
    }

    override fun storeDocumentSegments(documentInfo: DocumentInfo, segments: List<TextSegment>) {
        if (segments.isEmpty()) {
            log.warn("No segments to store for document id: {}", documentInfo.id)
            return
        }

        val embeddings = embeddingModel.embedAll(segments).content()
        embeddingStore.addAll(embeddings, segments)
        documentsRegistry[documentInfo.id] = documentInfo
        log.info("Stored {} chunks for document '{}' (id: {})", segments.size, documentInfo.title, documentInfo.id)
    }

    override fun listDocuments(): List<DocumentInfo> {
        return documentsRegistry.values.sortedByDescending { it.createdAt }
    }

    override fun deleteDocument(documentId: String): Boolean {
        val removedDoc = documentsRegistry.remove(documentId)
        if (removedDoc != null) {
            val filter: Filter = IsEqualTo("document_id", documentId)
            embeddingStore.removeAll(filter)
            log.info("Removed document '{}' (id: {}) and its embeddings", removedDoc.title, documentId)
            return true
        }
        log.warn("Attempted to delete non-existing document id: {}", documentId)
        return false
    }

    override fun processQuestion(question: Question): Answer {
        val questionText = question.value
        log.info("Processing question: {}", questionText)

        // 1. Embed question
        val queryEmbedding: Embedding = embeddingModel.embed(questionText).content()

        // 2. Similarity search
        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(4)
            .build()

        val matches = embeddingStore.search(searchRequest).matches()
        log.info("Retrieved {} relevant chunks from store", matches.size)

        val localRetrievedTexts = matches.map { match ->
            val segment = match.embedded()
            val sourceTitle = segment.metadata()?.getString("title") ?: "Unknown"
            "[Source: $sourceTitle]\n${segment.text()}"
        }

        val externalContext = externalContextPort.fetch(questionText)
        // keep structured facts so we know the tool/title that produced each fact
        val externalFactTexts = externalContext.map { fact ->
            Triple(fact.title, fact.source, "[External source: ${fact.source}]\n${fact.content}")
        }

        val retrievedTexts = localRetrievedTexts + externalFactTexts.map { it.third }

        // If any external tool returned direct successful summaries, collect them and prefer by priority
        val summaryRegex = Regex("""(?s)Summary:\s*(.*?)(?:\nDetails:|$)""")

        val summariesByTool = externalFactTexts.mapNotNull { (toolName, _, text) ->
            if (!text.contains("Status: success") || !text.contains("Summary:")) return@mapNotNull null
            val match = summaryRegex.find(text)
            val summary = match?.groups?.get(1)?.value?.trim()
            summary?.takeIf { it.isNotBlank() }?.let { toolName to it }
        }

        var toolDirectAnswer: String? = null
        if (summariesByTool.isNotEmpty()) {
            // Prefer election.search explicitly
            val election = summariesByTool.firstOrNull { it.first.equals("election.search", ignoreCase = true) }
            if (election != null) {
                toolDirectAnswer = election.second
            } else {
                // fallback: prefer web-related tools
                val webPref = summariesByTool.firstOrNull { (tool, _) ->
                    tool.contains("web", ignoreCase = true) || tool.contains("wiki", ignoreCase = true) || tool.contains("search", ignoreCase = true)
                }
                toolDirectAnswer = webPref?.second ?: summariesByTool.maxByOrNull { it.second.length }?.second
            }
        }

        if (toolDirectAnswer != null) {
            // Return early using the selected tool's answer to avoid LLM hallucination
            return Answer(
                question = questionText,
                text = toolDirectAnswer,
                retrievedChunks = retrievedTexts
            )
        }

        // 3. Build prompt
        val prompt = promptBuilder.build(retrievedTexts, questionText, promptVariant)

        // 4. Call LLM
        val llmResponseText = assistant.chat(prompt)

        return Answer(
            question = questionText,
            text = llmResponseText,
            retrievedChunks = retrievedTexts
        )
    }
}
