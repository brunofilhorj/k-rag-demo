package com.bfilho.kragdemo.domain.port.`in`

import com.bfilho.kragdemo.domain.model.Answer
import com.bfilho.kragdemo.domain.model.Question

interface AskQuestionUseCase {
    fun execute(question: Question): Answer
}
