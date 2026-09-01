package com.bfilho.kragdemo.adapter.`in`.web.dto

import com.bfilho.kragdemo.domain.model.DocumentInfo
import java.time.Instant

data class DocumentResponse(
    val id: String,
    val title: String,
    val sourceType: String,
    val createdAt: Instant,
    val chunkCount: Int
) {
    companion object {
        fun fromDomain(domain: DocumentInfo): DocumentResponse {
            return DocumentResponse(
                id = domain.id,
                title = domain.title,
                sourceType = domain.sourceType,
                createdAt = domain.createdAt,
                chunkCount = domain.chunkCount
            )
        }
    }
}
