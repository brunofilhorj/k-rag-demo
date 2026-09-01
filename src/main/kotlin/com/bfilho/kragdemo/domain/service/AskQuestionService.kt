package com.bfilho.kragdemo.domain.service

import com.bfilho.kragdemo.domain.model.Answer
import com.bfilho.kragdemo.domain.model.Question
import com.bfilho.kragdemo.domain.port.`in`.AskQuestionUseCase
import com.bfilho.kragdemo.domain.port.out.RagEnginePort
import org.springframework.stereotype.Service

@Service
class AskQuestionService(
    private val ragEnginePort: RagEnginePort
) : AskQuestionUseCase {

    override fun execute(question: Question): Answer {
        return ragEnginePort.processQuestion(question)
    }
}
