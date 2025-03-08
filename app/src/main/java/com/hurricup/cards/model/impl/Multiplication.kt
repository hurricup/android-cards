package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary

class Multiplication : Questionary("Таблица умножения") {
    override val _questions: MutableList<Question> by lazy {
        val result = mutableListOf<Question>()
        for (i in 0..10) {
            for (j in 0..10) {
                result += Question("$i x $j = ?")
            }
        }
        result
    }
}