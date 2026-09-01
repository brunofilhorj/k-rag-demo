package com.bfilho.kragdemo.adapter.`in`.web.dto

import com.bfilho.kragdemo.domain.model.Answer

data class QuestionResponse(
    val question: String,
    val answer: String,
    val retrievedChunks: List<String> = emptyList()
) {
    companion object {
        fun fromDomain(domainAnswer: Answer): QuestionResponse {
            return QuestionResponse(
                question = domainAnswer.question,
                answer = domainAnswer.text,
                retrievedChunks = domainAnswer.retrievedChunks
            )
        }
    }
}
