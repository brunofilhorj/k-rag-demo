package com.bfilho.kragdemo.evaluation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptEvaluationRunnerTest {

    private val runner = PromptEvaluationRunner()

    @Test
    fun `should load the benchmark dataset and evaluate sample answers`() {
        val cases = runner.loadCases("benchmarks/prompt-evaluation-dataset.json")

        assertEquals(3, cases.size)

        val answersByCaseId = mapOf(
            "hexagonal-architecture" to "The hexagonal architecture separates business rules from input and output adapters, keeping the core domain isolated.",
            "embedding-model" to "The embedding model uses Ollama with the nomic-embed-text embedding model to convert text into vectors.",
            "knowledge-base-purpose" to "The knowledge base is used to answer questions from relevant documents and provide grounded context to the LLM."
        )

        val summary = runner.evaluateAll(cases) { benchmarkCase ->
            answersByCaseId[benchmarkCase.id] ?: ""
        }

        assertEquals(3, summary.totalCases)
        assertTrue(summary.averageScore >= 0.75)
        assertEquals(3, summary.passedCases)
    }
}
