package com.bfilho.kragdemo.domain.port.out

import com.bfilho.kragdemo.domain.model.ExternalContextFact

interface ExternalContextPort {
    fun fetch(question: String): List<ExternalContextFact>
}
