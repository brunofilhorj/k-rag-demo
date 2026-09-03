package com.bfilho.kragdemo.evaluation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStream
import java.util.Locale

class PromptEvaluationRunner {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun loadCases(resourcePath: String): List<BenchmarkCase> {
        val inputStream: InputStream = Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream(resourcePath)
            ?: error("Benchmark resource not found: $resourcePath")

        return inputStream.use { stream ->
            objectMapper.readValue(stream, Array<BenchmarkCase>::class.java).toList()
        }
    }

    fun evaluateAll(
        cases: List<BenchmarkCase>,
        answerProvider: (BenchmarkCase) -> String
    ): EvaluationSummary {
        val results = cases.map { benchmarkCase ->
            val answer = answerProvider(benchmarkCase)
            val score = evaluateCase(benchmarkCase, answer)
            EvaluationResult(
                caseId = benchmarkCase.id,
                question = benchmarkCase.question,
                score = score,
                passed = score >= 0.7
            )
        }

        val averageScore = if (results.isEmpty()) 0.0 else results.map { it.score }.average()
        return EvaluationSummary(
            totalCases = results.size,
            averageScore = averageScore,
            results = results,
            passedCases = results.count { it.passed }
        )
    }

    fun evaluateCase(benchmarkCase: BenchmarkCase, answer: String): Double {
        val normalizedAnswer = normalize(benchmarkCase.expectedAnswer + " " + answer)
        val normalizedExpected = normalize(benchmarkCase.expectedAnswer)
        val keywords = benchmarkCase.expectedKeywords.map { normalize(it) }.filter { it.isNotBlank() }

        val keywordCoverage = if (keywords.isEmpty()) {
            1.0
        } else {
            val matchedKeywords = keywords.count { normalizedAnswer.contains(it) }
            matchedKeywords.toDouble() / keywords.size.toDouble()
        }

        val exactMatch = if (normalizedExpected.isBlank()) 0.0 else {
            if (normalizedAnswer.contains(normalizedExpected)) 1.0 else 0.0
        }

        return (0.6 * keywordCoverage + 0.4 * exactMatch).coerceIn(0.0, 1.0)
    }

    private fun normalize(value: String): String {
        return value.lowercase(Locale.getDefault())
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}

data class EvaluationSummary(
    val totalCases: Int,
    val averageScore: Double,
    val results: List<EvaluationResult>,
    val passedCases: Int
)

data class EvaluationResult(
    val caseId: String,
    val question: String,
    val score: Double,
    val passed: Boolean
)
