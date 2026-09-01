package com.bfilho.kragdemo.adapter.`in`.web

import com.bfilho.kragdemo.adapter.`in`.web.dto.QuestionRequest
import com.bfilho.kragdemo.adapter.`in`.web.dto.QuestionResponse
import com.bfilho.kragdemo.domain.model.Question
import com.bfilho.kragdemo.domain.port.`in`.AskQuestionUseCase
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController(
    private val askQuestionUseCase: AskQuestionUseCase
) {

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun askQuestion(@RequestBody request: QuestionRequest): QuestionResponse {
        val domainQuestion = Question(value = request.question)
        val domainAnswer = askQuestionUseCase.execute(domainQuestion)
        return QuestionResponse.fromDomain(domainAnswer)
    }
}
