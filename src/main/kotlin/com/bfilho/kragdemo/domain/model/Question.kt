package com.bfilho.kragdemo.domain.model

data class Question(
    val value: String
) {
    init {
        require(value.isNotBlank()) { "Question content cannot be blank" }
    }
}
