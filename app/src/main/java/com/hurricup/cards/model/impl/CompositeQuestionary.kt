package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Questionary

class CompositeQuestionary(title: String, questionaries: List<Questionary>) : Questionary(title) {
    override val _questions = questionaries.flatMap { it.questions }.toMutableList()
}
