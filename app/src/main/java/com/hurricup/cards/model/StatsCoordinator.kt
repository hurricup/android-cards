package com.hurricup.cards.model

import java.io.File

const val DEFAULT_SESSION_SIZE = 50
const val DEFAULT_MAX_AGE_DAYS = 28
private const val MISTAKES_CAP = 0.5

/** Known reinforcement is sampled from this multiple of the needed slots (oldest first), for variety. */
private const val KNOWN_POOL_FACTOR = 1.5

/**
 * Routes stats operations to the per-questionary-id store that owns each question
 * ([Question.questionaryId]), so a question keeps one history regardless of which
 * questionary (plain or composite) shows it.
 *
 * Session composition (three piles, oldest-first mistakes capped, new, then oldest known)
 * and per-questionary aggregation live here; the per-file storage is [QuestionaryStats].
 */
class StatsCoordinator(
    private val filesDir: File,
    private val maxAgeDays: Double = DEFAULT_MAX_AGE_DAYS.toDouble(),
) {
    private val byId = HashMap<String, QuestionaryStats>()

    private fun statsFor(id: String): QuestionaryStats = byId.getOrPut(id) {
        val safeName = id.replace(Regex("[^\\w]"), "_") + ".json"
        QuestionaryStats(File(filesDir, "stats/$safeName"), maxAgeDays)
    }

    private fun statsOf(question: Question) = statsFor(question.questionaryId)

    fun record(question: Question, correct: Boolean) =
        statsOf(question).recordAttempt(question.text, correct)

    /**
     * Composes a session over [questionary]'s questions:
     *   1. Split into mistakes (score > 0) / new (no attempts) / known (score ≤ 0).
     *   2. Up to [mistakesCap] of the slots from mistakes, least-recently-seen first.
     *   3. Fill the rest with new (random), then known sampled from an oldest-first pool
     *      ([KNOWN_POOL_FACTOR]× the needed slots) for between-session variety.
     *   4. Shuffle the result.
     * Returns indices into [Questionary.questions].
     */
    fun selectSession(
        questionary: Questionary,
        sessionSize: Int = DEFAULT_SESSION_SIZE,
        mistakesCap: Double = MISTAKES_CAP,
    ): List<Int> {
        val now = System.currentTimeMillis()
        val questions = questionary.questions
        val size = minOf(sessionSize, questions.size)

        val mistakes = mutableListOf<Int>()
        val new = mutableListOf<Int>()
        val known = mutableListOf<Int>()
        for (i in questions.indices) {
            val q = questions[i]
            val stats = statsOf(q)
            when {
                !stats.hasAttempts(q.text) -> new.add(i)
                stats.score(q.text, now) > 0 -> mistakes.add(i)
                else -> known.add(i)
            }
        }

        val result = mutableListOf<Int>()
        // up to mistakesCap of the session from mistakes, least-recently-seen first
        mistakes.sortBy { statsOf(questions[it]).lastAsked(questions[it].text) }
        result.addAll(mistakes.take((size * mistakesCap).toInt()))

        // fill rest with new, in random order
        new.shuffle()
        result.addAll(new.take(size - result.size))

        // if still slots left, reinforce known: sample from a slightly larger oldest pool so
        // reinforcement varies between sessions instead of repeating the same oldest set
        if (result.size < size) {
            val slots = size - result.size
            known.sortBy { statsOf(questions[it]).lastAsked(questions[it].text) }
            val pool = known.take((slots * KNOWN_POOL_FACTOR).toInt()).toMutableList()
            pool.shuffle()
            result.addAll(pool.take(slots))
        }

        result.shuffle()
        return result
    }

    /**
     * Distribution over [questionary]'s questions (mistakes/known/new), aggregated across the
     * questions' owning stores. [Distribution.doneRecently] is true when the total answers since
     * [recentSince] reach the session goal (min of default size and question count).
     */
    fun distribution(questionary: Questionary, recentSince: Long): Distribution {
        val now = System.currentTimeMillis()
        var mistakes = 0
        var new = 0
        var known = 0
        var recent = 0
        var oldestAnsweredAt: Long? = null
        for (q in questionary.questions) {
            val stats = statsOf(q)
            when {
                !stats.hasAttempts(q.text) -> new++
                stats.score(q.text, now) > 0 -> mistakes++
                else -> known++
            }
            if (stats.hasAttempts(q.text)) {
                val lastAsked = stats.lastAsked(q.text)
                if (oldestAnsweredAt == null || lastAsked < oldestAnsweredAt) {
                    oldestAnsweredAt = lastAsked
                }
            }
            recent += stats.answersSince(q.text, recentSince)
        }
        val goal = minOf(DEFAULT_SESSION_SIZE, questionary.size)
        return Distribution(mistakes, known, new, doneRecently = recent >= goal, oldestAnsweredAt = oldestAnsweredAt)
    }
}
