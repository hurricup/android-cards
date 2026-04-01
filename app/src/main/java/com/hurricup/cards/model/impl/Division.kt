package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary
import kotlin.random.Random

class Division : Questionary("Деление") {
    override val _questions: MutableList<Question> by lazy {
        val rng = Random(43)
        val result = mutableListOf<Question>()
        for (i in 2..10) {
            for (j in 2..10) {
                result += Question("${i * j} / $j", "$i")
            }
        }
        // division involving 0: "0 / n = 0" (n > 0) and "n / 0 = N/A"
        val zeroNumbers = (1..100).shuffled(rng).take(3)
        for (p in zeroNumbers) {
            result += Question("0 / $p", "0")
        }
        val divByZeroNumbers = (0..100).shuffled(rng).take(3)
        for (p in divByZeroNumbers) {
            result += Question("$p / 0", "N/A")
        }
        // division involving 1: mix of "n / 1 = n" and "n / n = 1"
        val onePartners = (2..100).shuffled(rng).take(6)
        for (p in onePartners) {
            if (rng.nextBoolean()) {
                result += Question("$p / 1", "$p")
            } else {
                result += Question("$p / $p", "1")
            }
        }
        result
    }
}
