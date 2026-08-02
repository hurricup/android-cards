package com.hurricup.cards.model

import org.json.JSONObject
import java.io.File

/**
 * Per-question tier state for a single questionary id, persisted as one JSON file
 * (separate from the legacy attempts-log stats). Absent entry = tier 0 (unknown/new).
 */
class TierStore(private val file: File, private val maxTier: Int = MAX_TIER) {
    private val states: MutableMap<String, TierState> = mutableMapOf()

    init {
        load()
    }

    fun state(questionText: String): TierState? = states[questionText]

    fun record(questionText: String, correct: Boolean, now: Long) {
        val current = states[questionText]?.tier ?: 0
        states[questionText] = TierState(nextTier(current, correct, maxTier), now, correct)
        save()
    }

    /** Bulk replace (used by the import that derives tiers from the legacy stats). */
    fun replaceAll(newStates: Map<String, TierState>) {
        states.clear()
        states.putAll(newStates)
        save()
    }

    /** Removes all questions currently at [tier] (back to unknown/tier 0). Returns how many. */
    fun clearTier(tier: Int): Int {
        val keys = states.filterValues { it.tier == tier }.keys.toList()
        keys.forEach { states.remove(it) }
        if (keys.isNotEmpty()) save()
        return keys.size
    }

    private fun load() {
        if (!file.exists()) return
        val json = JSONObject(file.readText())
        for (key in json.keys()) {
            val obj = json.getJSONObject(key)
            states[key] = TierState(obj.getInt("t"), obj.getLong("ts"), obj.getBoolean("ok"))
        }
    }

    private fun save() {
        val json = JSONObject()
        for ((key, s) in states) {
            json.put(key, JSONObject().apply {
                put("t", s.tier)
                put("ts", s.lastAnswered)
                put("ok", s.lastCorrect)
            })
        }
        file.parentFile?.mkdirs()
        file.writeText(json.toString())
    }
}
