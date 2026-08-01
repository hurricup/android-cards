package com.hurricup.cards.model

import org.json.JSONObject
import java.io.File

/** Max cards taken from the top (fully-learned) tier per session, so it can't crowd out the rest. */
const val TOP_TIER_TAKE_LIMIT = 20

internal const val STATS_DIR = "stats"
internal const val TIERS_DIR = "tiers"
internal const val TIER_LOG_DIR = "tier_log"

/**
 * Leitner spaced-repetition scheduler. Routes each question to the tier store of its owning
 * questionary id ([Question.questionaryId]), so a question keeps one tier regardless of which
 * questionary (plain or composite) shows it.
 *
 * Session composition: due cards highest tier first (only the max/top tier is capped), then new
 * (tier 0) to fill the session. Tier state lives in `tiers/<id>.json`, separate from the legacy
 * `stats/<id>.json` attempts log.
 */
class TierScheduler(
    private val filesDir: File,
    private val multiplier: Double,
    private val maxTier: Int = MAX_TIER,
) {
    private val byId = HashMap<String, TierStore>()

    private fun storeFor(id: String): TierStore = byId.getOrPut(id) {
        TierStore(File(filesDir, "$TIERS_DIR/${fileName(id)}"), maxTier)
    }

    private fun storeOf(question: Question) = storeFor(question.questionaryId)

    fun record(question: Question, correct: Boolean) {
        val now = System.currentTimeMillis()
        val store = storeOf(question)
        val from = store.state(question.text)?.tier ?: 0
        store.record(question.text, correct, now)
        val to = store.state(question.text)?.tier ?: from
        appendTransition(question.questionaryId, question.text, from, to, now)
    }

    /**
     * Appends the tier transition to an append-only JSONL log for later per-word analysis.
     * Compact keys keep the ever-growing log small: l=leaf id, q=question, f=from tier,
     * t=to tier, a=timestamp in seconds.
     */
    private fun appendTransition(leafId: String, question: String, from: Int, to: Int, atMs: Long) {
        val file = File(filesDir, "$TIER_LOG_DIR/${logFileName(leafId)}")
        file.parentFile?.mkdirs()
        val line = JSONObject()
            .put("l", leafId)
            .put("q", question)
            .put("f", from)
            .put("t", to)
            .put("a", atMs / 1000)
        file.appendText("$line\n")
    }

    private fun logFileName(id: String) = id.replace(Regex("[^\\w]"), "_") + ".jsonl"

    /** Current tier of a question (0 = new/unknown). */
    fun tierOf(question: Question): Int = storeOf(question).state(question.text)?.tier ?: 0

    /** Current tier state of a question, or null if new/unanswered. */
    fun stateOf(question: Question): TierState? = storeOf(question).state(question.text)

    /** Indices into [Questionary.questions] for the next session. */
    fun selectSession(questionary: Questionary, sessionSize: Int): List<Int> {
        val now = System.currentTimeMillis()
        val questions = questionary.questions

        val newIndices = mutableListOf<Int>()
        val dueByTier = HashMap<Int, MutableList<Int>>()
        for (i in questions.indices) {
            val q = questions[i]
            val state = storeOf(q).state(q.text)
            when {
                state == null -> newIndices.add(i)
                isDue(state, now, multiplier) -> dueByTier.getOrPut(state.tier) { mutableListOf() }.add(i)
                // not due -> skipped this session
            }
        }

        val result = mutableListOf<Int>()
        for (tier in dueByTier.keys.sortedDescending()) {
            if (result.size >= sessionSize) break
            val pool = dueByTier.getValue(tier).shuffled()
            // cap only the max tier (fully learned) so it can't crowd out lower tiers and new cards
            val capped = if (tier == maxTier) pool.take(TOP_TIER_TAKE_LIMIT) else pool
            result.addAll(capped.take(sessionSize - result.size))
        }
        if (result.size < sessionSize) {
            result.addAll(newIndices.shuffled().take(sessionSize - result.size))
        }
        result.shuffle()
        return result
    }

    fun distribution(questionary: Questionary): TierDistribution {
        val now = System.currentTimeMillis()
        var new = 0
        var due = 0
        var lastTrained: Long? = null
        val perTier = HashMap<Int, Int>()
        val perTierDue = HashMap<Int, Int>()
        for (q in questionary.questions) {
            val state = storeOf(q).state(q.text)
            if (state == null) {
                new++
            } else {
                perTier[state.tier] = (perTier[state.tier] ?: 0) + 1
                if (isDue(state, now, multiplier)) {
                    due++
                    perTierDue[state.tier] = (perTierDue[state.tier] ?: 0) + 1
                }
                if (lastTrained == null || state.lastAnswered > lastTrained) lastTrained = state.lastAnswered
            }
        }
        return TierDistribution(new, perTier, due, lastTrained, perTierDue)
    }

    private fun fileName(id: String) = id.replace(Regex("[^\\w]"), "_") + ".json"
}

/**
 * Tier breakdown of a questionary's questions: new (tier 0), a count per tier, and how many are due.
 */
data class TierDistribution(
    val new: Int,
    val perTier: Map<Int, Int>,
    val due: Int,
    val lastTrained: Long? = null,
    val perTierDue: Map<Int, Int> = emptyMap(),
) {
    val total: Int get() = new + perTier.values.sum()
    val learning: Int get() = perTier.values.sum()
}
