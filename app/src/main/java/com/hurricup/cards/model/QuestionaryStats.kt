package com.hurricup.cards.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.exp

private const val RIGHT_WEIGHT = -0.5
private const val WRONG_WEIGHT = 1.0

/** Number of halflives after which an attempt's weight (~2%) is considered negligible and pruned. */
private const val MAX_AGE_HALFLIVES = 4.0

/**
 * Per-question answer history for a single questionary id, persisted as one JSON file.
 *
 * Each answer (right or wrong) is recorded with a timestamp. Every past attempt contributes to
 * the question's score with a weight that decays exponentially over time. The decay halflife is
 * derived from [maxAgeDays] (halflife = maxAgeDays / [MAX_AGE_HALFLIVES]):
 *
 *   score = Σ  weight × e^(-age_days / halflife)
 *
 * where weight is [WRONG_WEIGHT] (+1.0) for a wrong answer and [RIGHT_WEIGHT] (-0.5) for a
 * correct one. Attempts older than [maxAgeDays] days are pruned on load.
 *
 * Session composition and per-questionary aggregation live in [StatsCoordinator], which routes
 * each question to the store of its owning questionary id.
 */
class QuestionaryStats(private val file: File, private val maxAgeDays: Double) {
    private val halflifeDays = maxAgeDays / MAX_AGE_HALFLIVES
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

    internal fun hasAttempts(questionText: String): Boolean = attempts.containsKey(questionText)

    /** Number of answers recorded for [questionText] since [since]. */
    internal fun answersSince(questionText: String, since: Long): Int =
        attempts[questionText]?.count { it.timestamp >= since } ?: 0

    internal fun score(questionText: String, now: Long): Double {
        val list = attempts[questionText] ?: return 0.0
        return list.sumOf { attempt ->
            val ageDays = (now - attempt.timestamp) / (1000.0 * 60 * 60 * 24)
            val decay = exp(-ageDays / halflifeDays)
            val weight = if (attempt.correct) RIGHT_WEIGHT else WRONG_WEIGHT
            weight * decay
        }
    }

    private fun load() {
        if (!file.exists()) return
        val json = JSONObject(file.readText())
        val now = System.currentTimeMillis()
        val maxAgeMs = maxAgeDays * 24 * 60 * 60 * 1000
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

}

data class Distribution(val mistakes: Int, val known: Int, val new: Int, val doneRecently: Boolean = false) {
    val total get() = mistakes + known + new
}

private data class Attempt(val timestamp: Long, val correct: Boolean)
