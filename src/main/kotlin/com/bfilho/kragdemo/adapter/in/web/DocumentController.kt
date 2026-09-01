package com.bfilho.kragdemo.adapter.`in`.web

import com.bfilho.kragdemo.adapter.`in`.web.dto.DocumentResponse
import com.bfilho.kragdemo.adapter.`in`.web.dto.TextIngestRequest
import com.bfilho.kragdemo.domain.port.`in`.IngestDocumentUseCase
import com.bfilho.kragdemo.domain.port.`in`.ManageKnowledgeBaseUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/documents")
class DocumentController(
    private val ingestDocumentUseCase: IngestDocumentUseCase,
    private val manageKnowledgeBaseUseCase: ManageKnowledgeBaseUseCase
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadDocument(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("title", required = false) title: String?
    ): DocumentResponse {
        require(!file.isEmpty) { "O arquivo enviado não pode estar vazio." }
        val originalFilename = file.originalFilename ?: "documento"
        val contentType = file.contentType ?: "application/octet-stream"

        val docInfo = file.inputStream.use { inputStream ->
            ingestDocumentUseCase.ingestFile(
                fileName = originalFilename,
                inputStream = inputStream,
                contentType = contentType,
                customTitle = title
            )
        }
        return DocumentResponse.fromDomain(docInfo)
    }

    @PostMapping("/text", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun ingestText(@RequestBody request: TextIngestRequest): DocumentResponse {
        require(request.title.isNotBlank()) { "O título não pode estar em branco." }
        require(request.text.isNotBlank()) { "O texto não pode estar em branco." }

        val docInfo = ingestDocumentUseCase.ingestText(
            title = request.title,
            text = request.text
        )
        return DocumentResponse.fromDomain(docInfo)
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listDocuments(): List<DocumentResponse> {
        return manageKnowledgeBaseUseCase.listDocuments().map { DocumentResponse.fromDomain(it) }
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(@PathVariable("id") id: String): ResponseEntity<Void> {
        val deleted = manageKnowledgeBaseUseCase.deleteDocument(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
