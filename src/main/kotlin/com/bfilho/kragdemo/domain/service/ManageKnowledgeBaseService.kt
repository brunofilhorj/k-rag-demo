package com.bfilho.kragdemo.domain.service

import com.bfilho.kragdemo.domain.model.DocumentInfo
import com.bfilho.kragdemo.domain.port.`in`.ManageKnowledgeBaseUseCase
import com.bfilho.kragdemo.domain.port.out.KnowledgeBaseStorePort
import org.springframework.stereotype.Service

@Service
class ManageKnowledgeBaseService(
    private val knowledgeBaseStorePort: KnowledgeBaseStorePort
) : ManageKnowledgeBaseUseCase {

    override fun listDocuments(): List<DocumentInfo> {
        return knowledgeBaseStorePort.listDocuments()
    }

    override fun deleteDocument(documentId: String): Boolean {
        return knowledgeBaseStorePort.deleteDocument(documentId)
    }
}
