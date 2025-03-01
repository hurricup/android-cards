package com.hurricup.cards.model

import android.content.res.AssetManager

class Questionary(val title: String) {
    val questions: MutableList<Question> = mutableListOf()

    companion object {
        fun readAll(assetsManager: AssetManager): List<Questionary> =
            assetsManager.list("xml")?.map {
                Questionary(it)
            }?.toList() ?: emptyList()

    }
}