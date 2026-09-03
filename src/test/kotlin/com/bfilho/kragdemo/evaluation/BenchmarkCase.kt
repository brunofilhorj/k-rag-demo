package com.bfilho.kragdemo.evaluation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BenchmarkCase(
    val id: String,
    val question: String,
    val expectedAnswer: String,
    val expectedKeywords: List<String>
)
