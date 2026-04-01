package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary
import kotlin.random.Random

class Multiplication : Questionary("Умножение") {
    override val _questions: MutableList<Question> by lazy {
        val rng = Random(42)
        val result = mutableListOf<Question>()
        for (i in 2..10) {
            for (j in 2..10) {
                result += Question("$i × $j", "${i * j}")
            }
        }
        for (n in listOf(0, 1)) {
            val partners = (0..9).shuffled(rng).take(6)
            for (p in partners) {
                val (a, b) = if (rng.nextBoolean()) n to p else p to n
                result += Question("$a × $b", "${a * b}")
            }
        }
        result
    }
}
