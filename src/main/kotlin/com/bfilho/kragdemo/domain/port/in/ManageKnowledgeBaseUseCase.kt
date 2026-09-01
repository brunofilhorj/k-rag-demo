package com.bfilho.kragdemo.domain.port.`in`

import com.bfilho.kragdemo.domain.model.DocumentInfo

interface ManageKnowledgeBaseUseCase {
    fun listDocuments(): List<DocumentInfo>
    fun deleteDocument(documentId: String): Boolean
}
