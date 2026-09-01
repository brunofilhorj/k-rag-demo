package com.bfilho.kragdemo.adapter.`in`.web

import com.bfilho.kragdemo.adapter.`in`.web.dto.TextIngestRequest
import com.bfilho.kragdemo.domain.model.DocumentInfo
import com.bfilho.kragdemo.domain.port.`in`.IngestDocumentUseCase
import com.bfilho.kragdemo.domain.port.`in`.ManageKnowledgeBaseUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.InputStream
import java.time.Instant

class DocumentControllerTest {

    private lateinit var mockMvc: MockMvc
    private val ingestDocumentUseCase: IngestDocumentUseCase = mock(IngestDocumentUseCase::class.java)
    private val manageKnowledgeBaseUseCase: ManageKnowledgeBaseUseCase = mock(ManageKnowledgeBaseUseCase::class.java)
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val controller = DocumentController(ingestDocumentUseCase, manageKnowledgeBaseUseCase)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `should ingest text via API`() {
        val request = TextIngestRequest(title = "Políticas de Segurança", text = "Conteúdo importante sobre segurança.")
        val expectedDoc = DocumentInfo(
            id = "doc-123",
            title = "Políticas de Segurança",
            sourceType = "TEXT",
            createdAt = Instant.now(),
            chunkCount = 1
        )

        given(ingestDocumentUseCase.ingestText(request.title, request.text)).willReturn(expectedDoc)

        mockMvc.perform(
            post("/api/v1/documents/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("doc-123"))
            .andExpect(jsonPath("$.title").value("Políticas de Segurança"))
            .andExpect(jsonPath("$.sourceType").value("TEXT"))
    }

    @Test
    fun `should upload document file via API`() {
        val mockFile = MockMultipartFile("file", "documento.pdf", "application/pdf", "conteudo pdf".toByteArray())
        val expectedDoc = DocumentInfo(
            id = "doc-456",
            title = "documento.pdf",
            sourceType = "PDF",
            createdAt = Instant.now(),
            chunkCount = 2
        )

        given(ingestDocumentUseCase.ingestFile(
            fileName = any(String::class.java) ?: "documento.pdf",
            inputStream = any(InputStream::class.java) ?: mockFile.inputStream,
            contentType = any(String::class.java) ?: "application/pdf",
            customTitle = any()
        )).willReturn(expectedDoc)

        mockMvc.perform(
            multipart("/api/v1/documents/upload")
                .file(mockFile)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `should list documents`() {
        val doc1 = DocumentInfo("doc-1", "Doc 1", "TXT", Instant.now(), 1)
        given(manageKnowledgeBaseUseCase.listDocuments()).willReturn(listOf(doc1))

        mockMvc.perform(get("/api/v1/documents"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("doc-1"))
            .andExpect(jsonPath("$[0].title").value("Doc 1"))
    }

    @Test
    fun `should delete document`() {
        given(manageKnowledgeBaseUseCase.deleteDocument("doc-1")).willReturn(true)

        mockMvc.perform(delete("/api/v1/documents/doc-1"))
            .andExpect(status().isNoContent)

        verify(manageKnowledgeBaseUseCase).deleteDocument("doc-1")
    }
}
