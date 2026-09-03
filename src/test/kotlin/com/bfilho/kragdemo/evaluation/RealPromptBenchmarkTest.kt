package com.bfilho.kragdemo.evaluation

import com.bfilho.kragdemo.adapter.out.langchain4j.prompt.PromptVariant
import com.bfilho.kragdemo.domain.model.Question
import com.bfilho.kragdemo.domain.port.`in`.AskQuestionUseCase
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * This test class runs real benchmarks against a local Ollama service.
 * It requires a running Ollama service on http://localhost:11434.
 * The tests are disabled by default to avoid failures in environments without the service.
 *
 * To run the benchmarks, ensure that the Ollama service is running locally and remove the @Disabled annotation.
 * To execute:
docker compose up -d
 * ./gradlew test --tests "com.bfilho.kragdemo.evaluation.RealPromptBenchmarkBaselineTest"
 * ./gradlew test --tests "com.bfilho.kragdemo.evaluation.RealPromptBenchmarkGroundedOnlyTest"
 * ./gradlew test --tests "com.bfilho.kragdemo.evaluation.RealPromptBenchmarkConciseAnswerTest"
 */
@SpringBootTest
@Disabled("Requires a local Ollama service on http://localhost:11434")
abstract class RealPromptBenchmarkTestBase {

    @Autowired
    protected lateinit var askQuestionService: AskQuestionUseCase

    @Autowired
    protected lateinit var embeddingModel: OllamaEmbeddingModel

    @Autowired
    protected lateinit var embeddingStore: InMemoryEmbeddingStore<TextSegment>

    protected val runner = PromptEvaluationRunner()

    protected fun runBenchmark(promptVariant: PromptVariant) {
        val benchmarkCases = runner.loadCases("benchmarks/prompt-evaluation-dataset.json")

        val benchmarkDocumentText = listOf(
            "The hexagonal architecture separates business rules from input and output adapters.",
            "The core domain remains isolated from transport and persistence concerns.",
            "The project uses Ollama with the nomic-embed-text embedding model.",
            "Embeddings transform text into vectors for semantic similarity search.",
            "The knowledge base stores documents and chunks so the system can answer questions using relevant context.",
            "The retrieval stage selects relevant passages before the LLM formulates the answer."
        )

        val segments = benchmarkDocumentText.mapIndexed { index, text ->
            TextSegment.from(
                text,
                Metadata.from(
                    mapOf(
                        "document_id" to "benchmark-doc-$index",
                        "title" to "Benchmark Knowledge Base",
                        "source_type" to "TEXT"
                    )
                )
            )
        }

        embeddingStore.removeAll()
        val embeddings = embeddingModel.embedAll(segments).content()
        embeddingStore.addAll(embeddings, segments)

        val summary = runner.evaluateAll(benchmarkCases) { benchmarkCase ->
            askQuestionService.execute(Question(benchmarkCase.question)).text
        }

        println("${promptVariant.name}: averageScore=${summary.averageScore}, passed=${summary.passedCases}/${summary.totalCases}")
        println("Results: ${summary.results.joinToString { "${it.caseId}=${it.score}" }}")
    }
}

@SpringBootTest(properties = ["prompt.variant=BASELINE"])
@Disabled("Requires a local Ollama service on http://localhost:11434")
class RealPromptBenchmarkBaselineTest : RealPromptBenchmarkTestBase() {
    @Test
    fun benchmarkBaselinePrompt() = runBenchmark(PromptVariant.BASELINE)
}

@SpringBootTest(properties = ["prompt.variant=GROUNDED_ONLY"])
@Disabled("Requires a local Ollama service on http://localhost:11434")
class RealPromptBenchmarkGroundedOnlyTest : RealPromptBenchmarkTestBase() {
    @Test
    fun benchmarkGroundedOnlyPrompt() = runBenchmark(PromptVariant.GROUNDED_ONLY)
}

@SpringBootTest(properties = ["prompt.variant=CONCISE_ANSWER"])
@Disabled("Requires a local Ollama service on http://localhost:11434")
class RealPromptBenchmarkConciseAnswerTest : RealPromptBenchmarkTestBase() {
    @Test
    fun benchmarkConciseAnswerPrompt() = runBenchmark(PromptVariant.CONCISE_ANSWER)
}
