package com.hurricup.cards.model

import android.content.res.Resources

class Questionary(val title: String) {
    val questions: MutableList<Question> = mutableListOf()

    companion object {
        fun readAll(resources: Resources): List<Questionary> {
            return emptyList()
        }
    }
}