package com.bfilho.kragdemo.domain.port.out

import com.bfilho.kragdemo.domain.model.Answer
import com.bfilho.kragdemo.domain.model.Question

interface RagEnginePort {
    fun processQuestion(question: Question): Answer
}
