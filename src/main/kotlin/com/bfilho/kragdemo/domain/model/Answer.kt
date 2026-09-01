package com.bfilho.kragdemo.domain.model

data class Answer(
    val question: String,
    val text: String,
    val retrievedChunks: List<String> = emptyList()
)
