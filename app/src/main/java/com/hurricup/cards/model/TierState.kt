package com.hurricup.cards.model

/** Leitner state of one question: its tier, when it was last answered, and that answer's result. */
data class TierState(val tier: Int, val lastAnswered: Long, val lastCorrect: Boolean)
