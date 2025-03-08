package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary

class Division : Questionary("Деление") {
    override val _questions: MutableList<Question> by lazy {
        val result = mutableListOf<Question>()
        for (i in 1..10) {
            for (j in 1..10) {
                result += Question("${i * j} / $j = ?")
            }
        }
        result
    }
}