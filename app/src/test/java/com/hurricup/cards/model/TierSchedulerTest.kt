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
    fun topTierTakeLimitReservesRoomForLowerTiers() {
        // one tier-3 card (older, due) + one tier-1 card (due); with a tiny session both appear,
        // but the top tier is capped so it can't monopolize.
        // Build many due top-tier cards and a couple lower — verify lower/new still included.
        val texts = (1..(TOP_TIER_TAKE_LIMIT + 5)).map { q("top$it", "$it") }
        val questionary = read("Q", *texts.toTypedArray(), q("low", "L"), q("newone", "N"))
        val scheduler = TierScheduler(tmp.root, multiplier = 2.0)
        val now = System.currentTimeMillis()
        // make top$ cards tier 5 answered long ago (due), low card tier 1 answered long ago (due)
        val store = TierStore(File(tmp.root, "tiers/Q.json"))
        val states = HashMap<String, TierState>()
        for (i in 1..(TOP_TIER_TAKE_LIMIT + 5)) states["top$i"] = TierState(5, now - 100L * 24 * 60 * 60 * 1000, true)
        states["low"] = TierState(1, now - 100L * 24 * 60 * 60 * 1000, true)
        store.replaceAll(states)

        val session = TierScheduler(tmp.root, multiplier = 2.0)
            .selectSession(questionary, sessionSize = 50)
        val questions = questionary.questions
        val chosen = session.map { questions[it].text }.toSet()
        // top tier capped, so not all top cards taken, leaving room for the low-tier and the new card
        assertTrue("low tier card should be included", "low" in chosen)
        assertTrue("new card should be included", "newone" in chosen)
    }
}
