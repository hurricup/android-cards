package com.hurricup.cards.model

import java.io.File

/** Max cards taken from the single highest due tier, so sessions don't become all top-tier. */
const val TOP_TIER_TAKE_LIMIT = 20

private const val TIERS_DIR = "tiers"
private const val STATS_DIR = "stats"

/**
 * Leitner spaced-repetition scheduler. Routes each question to the tier store of its owning
 * questionary id ([Question.questionaryId]), so a question keeps one tier regardless of which
 * questionary (plain or composite) shows it.
 *
 * Session composition: due cards highest tier first (with a cap on the top tier), then new
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

    fun record(question: Question, correct: Boolean) =
        storeOf(question).record(question.text, correct, System.currentTimeMillis())

    /** Current tier of a question (0 = new/unknown). */
    fun tierOf(question: Question): Int = storeOf(question).state(question.text)?.tier ?: 0

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
        val tiersDesc = dueByTier.keys.sortedDescending()
        for ((rank, tier) in tiersDesc.withIndex()) {
            if (result.size >= sessionSize) break
            val pool = dueByTier.getValue(tier).shuffled()
            // cap the highest due tier so it can't crowd out lower tiers and new cards
            val capped = if (rank == 0) pool.take(TOP_TIER_TAKE_LIMIT) else pool
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
        for (q in questionary.questions) {
            val state = storeOf(q).state(q.text)
            if (state == null) {
                new++
            } else {
                perTier[state.tier] = (perTier[state.tier] ?: 0) + 1
                if (isDue(state, now, multiplier)) due++
                if (lastTrained == null || state.lastAnswered > lastTrained) lastTrained = state.lastAnswered
            }
        }
        return TierDistribution(new, perTier, due, lastTrained)
    }

    /**
     * Derives tier state from the legacy attempts-log stats (`stats/<id>.json`) into `tiers/<id>.json`,
     * replaying each question's history through the tier machine. Returns the number of files imported.
     */
    fun importFromStats(): Int {
        val statsDir = File(filesDir, STATS_DIR)
        val files = statsDir.listFiles { f -> f.extension == "json" } ?: return 0
        var imported = 0
        for (file in files) {
            val stats = QuestionaryStats(file, maxAgeDays = 100_000.0) // don't prune when importing
            val derived = stats.attemptsByText()
                .mapNotNull { (text, attempts) -> deriveTier(attempts, maxTier)?.let { text to it } }
                .toMap()
            TierStore(File(filesDir, "$TIERS_DIR/${file.name}"), maxTier).replaceAll(derived)
            imported++
        }
        byId.clear() // drop cached stores so fresh state is read
        return imported
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
) {
    val total: Int get() = new + perTier.values.sum()
    val learning: Int get() = perTier.values.sum()
}
