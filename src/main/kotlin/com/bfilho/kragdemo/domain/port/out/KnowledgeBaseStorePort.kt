package com.bfilho.kragdemo.domain.port.out

import com.bfilho.kragdemo.domain.model.DocumentInfo
import dev.langchain4j.data.segment.TextSegment

interface KnowledgeBaseStorePort {
    fun storeDocumentSegments(documentInfo: DocumentInfo, segments: List<TextSegment>)
    fun listDocuments(): List<DocumentInfo>
    fun deleteDocument(documentId: String): Boolean
}
