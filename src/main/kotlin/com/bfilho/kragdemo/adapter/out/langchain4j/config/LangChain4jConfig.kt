package com.bfilho.kragdemo.adapter.out.langchain4j.config

import com.bfilho.kragdemo.adapter.out.langchain4j.KnowledgeAssistant
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LangChain4jConfig(
    @Value("\${ollama.url}") private val ollamaUrl: String,
    @Value("\${ollama.embedding-model}") private val embeddingModelName: String,
    @Value("\${ollama.chat-model}") private val chatModelName: String
) {

    @Bean
    fun embeddingModel(): OllamaEmbeddingModel {
        return OllamaEmbeddingModel.builder()
            .baseUrl(ollamaUrl)
            .modelName(embeddingModelName)
            .build()
    }

    @Bean
    fun chatModel(): OllamaChatModel {
        return OllamaChatModel.builder()
            .baseUrl(ollamaUrl)
            .modelName(chatModelName)
            .build()
    }

    @Bean
    fun embeddingStore(): InMemoryEmbeddingStore<TextSegment> {
        return InMemoryEmbeddingStore<TextSegment>()
    }

    @Bean
    fun knowledgeAssistant(chatModel: OllamaChatModel): KnowledgeAssistant {
        return AiServices.builder(KnowledgeAssistant::class.java)
            .chatLanguageModel(chatModel)
            .build()
    }
}
