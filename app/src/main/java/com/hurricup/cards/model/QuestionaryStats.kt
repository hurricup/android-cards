package com.hurricup.cards.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.exp

private const val HALFLIFE_DAYS = 7.0
private const val MAX_AGE_DAYS = 28.0
private const val RIGHT_WEIGHT = -0.5
private const val WRONG_WEIGHT = 1.0
const val DEFAULT_SESSION_SIZE = 50
private const val MISTAKES_CAP = 0.7
private const val NEW_KNOWN_SPLIT = 0.5

/**
 * Tracks per-question answer history and uses it to compose learning sessions.
 *
 * Each answer (right or wrong) is recorded with a timestamp. Every past attempt contributes to
 * the question's score with a weight that decays exponentially over time
 * (halflife = [HALFLIFE_DAYS] days):
 *
 *   score = Σ  weight × e^(-age_days / halflife)
 *
 * where weight is [WRONG_WEIGHT] (+1.0) for a wrong answer and [RIGHT_WEIGHT] (-0.5) for a
 * correct one. Attempts older than [MAX_AGE_DAYS] days are pruned on load.
 *
 * Session composition ([selectSession]):
 *   1. Questions are split into three piles by score:
 *      - **Mistakes** (score > 0): recently wrong questions.
 *      - **New** (no attempts): never seen before.
 *      - **Known** (score ≤ 0): recently correct or well-learned.
 *   2. Up to [MISTAKES_CAP] (70%) of session slots go to mistakes, worst first.
 *   3. Remaining slots are split [NEW_KNOWN_SPLIT] (50/50) between new (random order)
 *      and known (oldest last-asked first).
 *   4. If one pile is too small, the other fills the leftover slots.
 *   5. The final list is shuffled.
 *
 * Also tracks last-asked timestamp per question for the known pile ordering.
 *
 * Stats are persisted as a JSON file per questionary in the app's internal storage.
 */
class QuestionaryStats(private val file: File) {
    private val attempts: MutableMap<String, MutableList<Attempt>> = mutableMapOf()
    private val lastAsked: MutableMap<String, Long> = mutableMapOf()

    init {
        load()
    }

    fun recordAttempt(questionText: String, correct: Boolean) {
        val now = System.currentTimeMillis()
        val list = attempts.getOrPut(questionText) { mutableListOf() }
        list.add(Attempt(now, correct))
        lastAsked[questionText] = now
        save()
    }

    fun lastAsked(questionText: String): Long = lastAsked[questionText] ?: 0L

    fun distribution(questionary: Questionary): Distribution {
        val now = System.currentTimeMillis()
        var mistakes = 0
        var new = 0
        var known = 0
        for (q in questionary.questions) {
            when {
                !hasAttempts(q.text) -> new++
                score(q.text, now) > 0 -> mistakes++
                else -> known++
            }
        }
        return Distribution(mistakes, known, new)
    }

    fun selectSession(questions: List<Question>, sessionSize: Int = DEFAULT_SESSION_SIZE): List<Int> {
        val now = System.currentTimeMillis()
        val size = minOf(sessionSize, questions.size)

        val scored = questions.indices.map { i -> i to score(questions[i].text, now) }
        val mistakes = mutableListOf<Int>()
        val new = mutableListOf<Int>()
        val known = mutableListOf<Int>()

        for ((i, s) in scored) {
            val text = questions[i].text
            when {
                !hasAttempts(text) -> new.add(i)
                s > 0 -> mistakes.add(i)
                else -> known.add(i)
            }
        }

        return composeSession(size, mistakes, new, known, questions, now)
    }

    private fun hasAttempts(questionText: String): Boolean = attempts.containsKey(questionText)

    private fun composeSession(
        size: Int,
        mistakes: MutableList<Int>,
        new: MutableList<Int>,
        known: MutableList<Int>,
        questions: List<Question>,
        now: Long
    ): List<Int> {
        val result = mutableListOf<Int>()

        // take up to 70% from mistakes, worst first
        mistakes.sortByDescending { score(questions[it].text, now) }
        val mistakesSlots = (size * MISTAKES_CAP).toInt()
        result.addAll(mistakes.take(mistakesSlots))

        val remaining = size - result.size

        // split remaining 50/50 between new and known
        new.shuffle()
        known.sortBy { lastAsked(questions[it].text) }

        val newSlots = (remaining * NEW_KNOWN_SPLIT).toInt()
        val knownSlots = remaining - newSlots

        val selectedNew = new.take(newSlots)
        val selectedKnown = known.take(knownSlots)
        result.addAll(selectedNew)
        result.addAll(selectedKnown)

        // fill leftover slots if one pile was short
        val leftover = size - result.size
        if (leftover > 0) {
            val unusedNew = new.drop(selectedNew.size)
            val unusedKnown = known.drop(selectedKnown.size)
            result.addAll((unusedNew + unusedKnown).take(leftover))
        }

        result.shuffle()
        return result
    }

    private fun score(questionText: String, now: Long): Double {
        val list = attempts[questionText] ?: return 0.0
        return list.sumOf { attempt ->
            val ageDays = (now - attempt.timestamp) / (1000.0 * 60 * 60 * 24)
            val decay = exp(-ageDays / HALFLIFE_DAYS)
            val weight = if (attempt.correct) RIGHT_WEIGHT else WRONG_WEIGHT
            weight * decay
        }
    }

    private fun load() {
        if (!file.exists()) return
        val json = JSONObject(file.readText())
        val now = System.currentTimeMillis()
        val maxAgeMs = MAX_AGE_DAYS * 24 * 60 * 60 * 1000
        for (key in json.keys()) {
            val entry = json.get(key)
            val arr = when (entry) {
                is JSONArray -> entry // legacy format
                is JSONObject -> entry.getJSONArray("attempts")
                else -> continue
            }
            val list = mutableListOf<Attempt>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ts = obj.getLong("ts")
                if (now - ts < maxAgeMs) {
                    list.add(Attempt(ts, obj.getBoolean("ok")))
                }
            }
            if (list.isNotEmpty()) {
                attempts[key] = list
            }
            if (entry is JSONObject && entry.has("lastAsked")) {
                lastAsked[key] = entry.getLong("lastAsked")
            } else if (list.isNotEmpty()) {
                lastAsked[key] = list.maxOf { it.timestamp }
            }
        }
    }

    private fun save() {
        val json = JSONObject()
        for ((key, list) in attempts) {
            val entry = JSONObject()
            val arr = JSONArray()
            for (attempt in list) {
                arr.put(JSONObject().apply {
                    put("ts", attempt.timestamp)
                    put("ok", attempt.correct)
                })
            }
            entry.put("attempts", arr)
            lastAsked[key]?.let { entry.put("lastAsked", it) }
            json.put(key, entry)
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString())
    }

    companion object {
        fun forQuestionary(filesDir: File, questionary: Questionary): QuestionaryStats {
            val safeName = questionary.id.replace(Regex("[^\\w]"), "_") + ".json"
            return QuestionaryStats(File(filesDir, "stats/$safeName"))
        }
    }
}

data class Distribution(val mistakes: Int, val known: Int, val new: Int) {
    val total get() = mistakes + known + new
}

private data class Attempt(val timestamp: Long, val correct: Boolean)
