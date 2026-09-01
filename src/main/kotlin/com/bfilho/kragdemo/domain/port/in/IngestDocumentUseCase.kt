package com.bfilho.kragdemo.domain.port.`in`

import com.bfilho.kragdemo.domain.model.DocumentInfo
import java.io.InputStream

interface IngestDocumentUseCase {
    fun ingestFile(fileName: String, inputStream: InputStream, contentType: String, customTitle: String? = null): DocumentInfo
    fun ingestText(title: String, text: String): DocumentInfo
}
