package com.bfilho.kragdemo.domain.model

import java.time.Instant

data class DocumentInfo(
    val id: String,
    val title: String,
    val sourceType: String,
    val createdAt: Instant = Instant.now(),
    val chunkCount: Int = 0
)
