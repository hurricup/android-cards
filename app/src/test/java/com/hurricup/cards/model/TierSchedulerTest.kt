package com.hurricup.cards.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TierSchedulerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun q(text: String, answer: String) =
        "<question><text>$text</text><answer>$answer</answer></question>"

    private fun read(id: String, vararg questions: String): Questionary {
        val xml = """
            <questionary>
                <id>$id</id>
                <title>$id</title>
                <questions>${questions.joinToString("")}</questions>
            </questionary>
        """.trimIndent()
        return Questionary.readFile(xml.byteInputStream(), direct = true).single()
    }

    @Test
    fun nextTierTransitions() {
        assertEquals(1, nextTier(0, correct = false)) // unknown enters at 1 regardless
        assertEquals(1, nextTier(0, correct = true))
        assertEquals(2, nextTier(1, correct = true))
        assertEquals(1, nextTier(1, correct = false)) // reset to 1
        assertEquals(3, nextTier(2, correct = true))
        assertEquals(1, nextTier(5, correct = false)) // wrong resets to 1 from any tier
        assertEquals(MAX_TIER, nextTier(MAX_TIER, correct = true)) // capped
    }

    @Test
    fun recordPromotesAndPersists() {
        val q = read("Q", q("a", "x")).questions.single()
        TierScheduler(tmp.root, multiplier = 2.0).record(q, correct = true)

        // fresh scheduler reads from disk: tier 1
        val dist = TierScheduler(tmp.root, multiplier = 2.0).distribution(read("Q", q("a", "x")))
        assertEquals(1, dist.perTier[1])
        assertEquals(0, dist.new)
    }

    @Test
    fun newCardsFillSessionWhenNothingDue() {
        val questionary = read("Q", q("a", "1"), q("b", "2"), q("c", "3"))
        val session = TierScheduler(tmp.root, multiplier = 2.0).selectSession(questionary, sessionSize = 10)
        // all new, nothing due -> all pulled in
        assertEquals(setOf(0, 1, 2), session.toSet())
    }

    @Test
    fun tierOneCardIsDueAfterOneDay() {
        val questionary = read("Q", q("a", "1"))
        val scheduler = TierScheduler(tmp.root, multiplier = 2.0)
        val question = questionary.questions.single()
        scheduler.record(question, correct = true) // tier 1, answered now

        // right after answering, not due (interval 1 day)
        val store = TierStore(File(tmp.root, "tiers/Q.json"))
        val state = store.state("a")!!
        assertEquals(1, state.tier)
        assertFalse(isDue(state, System.currentTimeMillis(), 2.0))
        // a day + later, due
        assertTrue(isDue(state, state.lastAnswered + 25L * 60 * 60 * 1000, 2.0))
    }

    @Test
    fun maxTierIsCappedLeavingRoomForLowerAndNew() {
        // many due MAX_TIER cards + a due tier-1 card + a new card
        val topTexts = (1..(TOP_TIER_TAKE_LIMIT + 5)).map { q("top$it", "$it") }
        val questionary = read("Q", *topTexts.toTypedArray(), q("low", "L"), q("newone", "N"))
        val old = System.currentTimeMillis() - 1000L * 24 * 60 * 60 * 1000
        val states = HashMap<String, TierState>()
        for (i in 1..(TOP_TIER_TAKE_LIMIT + 5)) states["top$i"] = TierState(MAX_TIER, old, true)
        states["low"] = TierState(1, old, true)
        TierStore(File(tmp.root, "tiers/Q.json")).replaceAll(states)

        val chosen = TierScheduler(tmp.root, multiplier = 2.0)
            .selectSession(questionary, sessionSize = 50)
            .map { questionary.questions[it].text }.toSet()
        assertTrue("lower-tier card should be included", "low" in chosen)
        assertTrue("new card should be included", "newone" in chosen)
    }

    @Test
    fun nonMaxTierIsNotCapped() {
        // a due tier-2 backlog larger than the session must fill it entirely — no lower tier leaks in
        val topTexts = (1..(TOP_TIER_TAKE_LIMIT + 5)).map { q("t2_$it", "$it") }
        val questionary = read("Q", *topTexts.toTypedArray(), q("low", "L"))
        val old = System.currentTimeMillis() - 1000L * 24 * 60 * 60 * 1000
        val states = HashMap<String, TierState>()
        for (i in 1..(TOP_TIER_TAKE_LIMIT + 5)) states["t2_$i"] = TierState(2, old, true)
        states["low"] = TierState(1, old, true)
        TierStore(File(tmp.root, "tiers/Q.json")).replaceAll(states)

        val chosen = TierScheduler(tmp.root, multiplier = 2.0)
            .selectSession(questionary, sessionSize = TOP_TIER_TAKE_LIMIT)
            .map { questionary.questions[it].text }.toSet()
        assertFalse("tier-1 card should not appear while the tier-2 backlog fills the session", "low" in chosen)
    }
}
