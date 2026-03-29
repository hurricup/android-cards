package com.hurricup.cards.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.exp

private const val HALFLIFE_DAYS = 7.0
private const val MAX_AGE_DAYS = 28.0
private const val RIGHT_WEIGHT = -0.5
private const val WRONG_WEIGHT = 1.0
private const val JITTER_RANGE = 0.3

/**
 * Tracks per-question answer history and uses it to order questions for the next session.
 *
 * Each answer (right or wrong) is recorded with a timestamp. When ordering questions for a new
 * session, every past attempt contributes to the question's score with a weight that decays
 * exponentially over time (halflife = [HALFLIFE_DAYS] days):
 *
 *   score = Σ  weight × e^(-age_days / halflife)
 *
 * where weight is [WRONG_WEIGHT] (+1.0) for a wrong answer and [RIGHT_WEIGHT] (-0.5) for a
 * correct one. This means:
 *   - Recently wrong questions get high positive scores and appear first.
 *   - Correctly answered questions get negative scores and sink to the end.
 *   - New questions (no history) score 0 and land in the middle.
 *   - Old mistakes gradually dissolve — after [MAX_AGE_DAYS] days they are pruned entirely.
 *
 * A small random jitter ([JITTER_RANGE]) is added to each score so that questions with similar
 * scores appear in a different order every session.
 *
 * Stats are persisted as a JSON file per questionary in the app's internal storage.
 */
class QuestionaryStats(private val file: File) {
    private val attempts: MutableMap<String, MutableList<Attempt>> = mutableMapOf()

    init {
        load()
    }

    fun recordAttempt(questionText: String, correct: Boolean) {
        val list = attempts.getOrPut(questionText) { mutableListOf() }
        list.add(Attempt(System.currentTimeMillis(), correct))
        save()
    }

    fun sortedIndices(questions: List<Question>): List<Int> {
        val now = System.currentTimeMillis()
        return questions.indices
            .map { i -> i to score(questions[i].text, now) + Math.random() * JITTER_RANGE }
            .sortedByDescending { it.second }
            .map { it.first }
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
            val arr = json.getJSONArray(key)
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
        }
    }

    private fun save() {
        val json = JSONObject()
        for ((key, list) in attempts) {
            val arr = JSONArray()
            for (attempt in list) {
                arr.put(JSONObject().apply {
                    put("ts", attempt.timestamp)
                    put("ok", attempt.correct)
                })
            }
            json.put(key, arr)
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString())
    }

    companion object {
        fun forQuestionary(filesDir: File, title: String): QuestionaryStats {
            val safeName = title.replace(Regex("[^\\w]"), "_") + ".json"
            return QuestionaryStats(File(filesDir, "stats/$safeName"))
        }
    }
}

private data class Attempt(val timestamp: Long, val correct: Boolean)
