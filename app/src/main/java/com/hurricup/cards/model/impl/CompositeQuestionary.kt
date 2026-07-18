package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Questionary

class CompositeQuestionary(title: String, id: String, parts: List<Questionary>) : Questionary(title, id) {
    override val _questions = parts.flatMap { it.questions }.toMutableList()
}
