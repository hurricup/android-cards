package com.hurricup.cards.model.impl

import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.model.REVERSE_SUFFIX

/**
 * A composite that aggregates the questions of other questionaries referenced by id.
 * Parts are resolved lazily from the cache on first access, so referenced questionaries may be
 * declared in any file/order. Missing references are skipped; reference cycles are broken (each
 * id is entered at most once per resolution).
 */
class ReferenceCompositeQuestionary(
    title: String,
    id: String,
    val refIds: List<String>,
    private val reverse: Boolean,
) : Questionary(title, id) {

    override val _questions: MutableList<Question> by lazy {
        leafQuestionaries().flatMap { it.questions }.toMutableList()
    }

    override fun leafQuestionaries(): List<Questionary> = collectLeaves(HashSet())

    private fun targetId(refId: String) = if (reverse) "$refId$REVERSE_SUFFIX" else refId

    private fun collectLeaves(stack: MutableSet<String>): List<Questionary> {
        if (!stack.add(id)) return emptyList() // recursion — stop this branch
        val result = refIds.flatMap { refId ->
            when (val part = Questionary.cache[targetId(refId)]) {
                null -> emptyList()
                is ReferenceCompositeQuestionary -> part.collectLeaves(stack)
                else -> listOf(part)
            }
        }
        stack.remove(id)
        return result
    }
}
