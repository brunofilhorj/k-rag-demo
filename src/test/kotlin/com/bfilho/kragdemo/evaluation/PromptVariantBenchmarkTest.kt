package com.bfilho.kragdemo.evaluation

import com.bfilho.kragdemo.adapter.out.langchain4j.KnowledgeAssistant
import com.bfilho.kragdemo.adapter.out.langchain4j.prompt.PromptBuilder
import com.bfilho.kragdemo.adapter.out.langchain4j.prompt.PromptVariant
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptVariantBenchmarkTest {

    private val runner = PromptEvaluationRunner()
    private val promptBuilder = PromptBuilder(
        """
        CONTEXT:
        {{context}}

        QUESTION:
        {{question}}
        """.trimIndent()
    )

    private val benchmarkContextById = mapOf(
        "hexagonal-architecture" to listOf(
            "The hexagonal architecture separates business rules from input and output adapters.",
            "The core domain remains isolated from transport and persistence concerns."
        ),
        "embedding-model" to listOf(
            "The project uses Ollama with the nomic-embed-text embedding model.",
            "Embeddings transform text into vectors for semantic similarity search."
        ),
        "knowledge-base-purpose" to listOf(
            "The knowledge base stores documents and chunks so the system can answer questions using relevant context.",
            "The retrieval stage selects relevant passages before the LLM formulates the answer."
        )
    )

    private val fakeAssistant = object : KnowledgeAssistant {
        override fun chat(userMessage: String): String {
            val normalized = userMessage.lowercase()
            return when {
                normalized.contains("hexagonal") -> "The hexagonal architecture separates business rules from input and output adapters."
                normalized.contains("nomic") || normalized.contains("embedding") -> "The project uses Ollama with the nomic-embed-text embedding model."
                normalized.contains("knowledge base") -> "The knowledge base stores documents and chunks so the system can answer questions using relevant context."
                else -> "The information is not available in the knowledge base."
            }
        }
    }

    @Test
    fun `should compare prompt variants with the real RAG prompt-building flow`() {
        val cases = runner.loadCases("benchmarks/prompt-evaluation-dataset.json")

        val resultsByVariant = PromptVariant.entries.associateWith { variant ->
            runner.evaluateAll(cases) { benchmarkCase ->
                val context = benchmarkContextById[benchmarkCase.id] ?: emptyList()
                val prompt = promptBuilder.build(context, benchmarkCase.question, variant)
                fakeAssistant.chat(prompt)
            }
        }

        val baselineScore = resultsByVariant[PromptVariant.BASELINE]?.averageScore ?: 0.0
        val groundedOnlyScore = resultsByVariant[PromptVariant.GROUNDED_ONLY]?.averageScore ?: 0.0
        val conciseAnswerScore = resultsByVariant[PromptVariant.CONCISE_ANSWER]?.averageScore ?: 0.0

        assertTrue(baselineScore >= 0.75)
        assertTrue(groundedOnlyScore >= 0.75)
        assertTrue(conciseAnswerScore >= 0.75)

        println("Prompt benchmark results:")
        PromptVariant.entries.forEach { variant ->
            val summary = resultsByVariant[variant] ?: return@forEach
            println("${variant.name}: averageScore=${summary.averageScore}, passedCases=${summary.passedCases}/${summary.totalCases}")
        }
    }
}
