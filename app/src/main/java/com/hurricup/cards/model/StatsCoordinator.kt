package com.hurricup.cards.model

import java.io.File

const val DEFAULT_SESSION_SIZE = 50
const val DEFAULT_MAX_AGE_DAYS = 28
private const val MISTAKES_CAP = 0.5

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
     *   3. Fill the rest with new (random), then oldest known.
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

        // if still slots left, take oldest known
        if (result.size < size) {
            known.sortBy { statsOf(questions[it]).lastAsked(questions[it].text) }
            result.addAll(known.take(size - result.size))
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
        for (q in questionary.questions) {
            val stats = statsOf(q)
            when {
                !stats.hasAttempts(q.text) -> new++
                stats.score(q.text, now) > 0 -> mistakes++
                else -> known++
            }
            recent += stats.answersSince(q.text, recentSince)
        }
        val goal = minOf(DEFAULT_SESSION_SIZE, questionary.size)
        return Distribution(mistakes, known, new, doneRecently = recent >= goal)
    }
}
