package com.hurricup.cards.model

import kotlin.math.pow

/** Highest tier a card can reach. */
const val MAX_TIER = 8

/** Default interval growth multiplier between tiers. */
const val DEFAULT_INTERVAL_MULTIPLIER = 2.0

private const val TIER_DAY_MS = 24L * 60 * 60 * 1000

/**
 * Tier transition for an answer.
 * Tier 0 (unknown): a correct first answer jumps to tier 2 (already known, skip the 1-day step),
 * a wrong one enters at tier 1; either way it never returns to 0.
 * From tier ≥ 1: correct promotes (capped at [maxTier]); wrong resets to tier 1 — a retrieval
 * failure means the memory needs relearning soon, so it goes back to the shortest interval.
 */
fun nextTier(tier: Int, correct: Boolean, maxTier: Int = MAX_TIER): Int = when {
    tier <= 0 -> if (correct) minOf(maxTier, 2) else 1
    correct -> minOf(maxTier, tier + 1)
    else -> 1
}

/** Review interval in ms for a tier: tier 1 = 1 day, then × [multiplier] per tier. */
fun tierIntervalMs(tier: Int, multiplier: Double): Long =
    (multiplier.pow((tier - 1).coerceAtLeast(0)) * TIER_DAY_MS).toLong()

/**
 * A card is due when its interval has elapsed since it was last answered.
 * Tier 1 (just learning) is always due, so it recurs every session until it graduates.
 */
fun isDue(state: TierState, now: Long, multiplier: Double): Boolean =
    state.tier <= 1 || now - state.lastAnswered >= tierIntervalMs(state.tier, multiplier)
