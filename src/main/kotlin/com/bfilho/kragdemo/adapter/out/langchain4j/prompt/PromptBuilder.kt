package com.bfilho.kragdemo.adapter.out.langchain4j.prompt

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PromptBuilder(
    @Value("\${prompt.template.user:CONTEXT:\\n\\n{{context}}\\n\\nQUESTION:\\n\\n{{question}}}")
    private val userTemplate: String
) {
    fun build(
        retrievedTexts: List<String>,
        question: String,
        variant: PromptVariant = PromptVariant.BASELINE
    ): String {
        val context = if (retrievedTexts.isNotEmpty()) {
            retrievedTexts.joinToString("\n\n---\n\n")
        } else {
            "No relevant context was found in the knowledge base."
        }

        val template = when (variant) {
            PromptVariant.BASELINE -> userTemplate
            PromptVariant.GROUNDED_ONLY -> """
                Use only the information available in the context below.
                Do not invent facts. If the answer is not present in the context, say that the information is not available in the knowledge base.

                CONTEXT:
                {{context}}

                QUESTION:
                {{question}}
            """.trimIndent()
            PromptVariant.CONCISE_ANSWER -> """
                Answer using only the provided context and keep the answer concise.
                If the context does not contain the answer, say so clearly.

                CONTEXT:
                {{context}}

                QUESTION:
                {{question}}
            """.trimIndent()
        }

        return template
            .replace("{{context}}", context)
            .replace("{{question}}", question)
            .trim()
    }
}
