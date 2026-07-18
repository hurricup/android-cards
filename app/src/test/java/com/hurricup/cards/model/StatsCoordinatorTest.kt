package com.hurricup.cards.model

import com.hurricup.cards.model.impl.CompositeQuestionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StatsCoordinatorTest {

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
    fun compositeSharesHistoryWithParts() {
        val part1 = read("P1", q("a", "x"), q("b", "y"))
        val part2 = read("P2", q("c", "z"))
        val composite = CompositeQuestionary("C", "C", listOf(part1, part2))

        val coordinator = StatsCoordinator(tmp.root)
        // answer "a" wrong through the composite
        coordinator.record(composite.questions.first { it.text == "a" }, correct = false)

        // part1 sees it: a=mistake, b=new
        val d1 = coordinator.distribution(part1, recentSince = 0)
        assertEquals(1, d1.mistakes)
        assertEquals(1, d1.new)
        assertEquals(0, d1.known)

        // composite aggregates: a=mistake, b=new, c=new
        val dc = coordinator.distribution(composite, recentSince = 0)
        assertEquals(1, dc.mistakes)
        assertEquals(2, dc.new)
        assertEquals(0, dc.known)
    }

    @Test
    fun statsStoredPerPartNotComposite() {
        val part1 = read("P1", q("a", "x"))
        val part2 = read("P2", q("c", "z"))
        val composite = CompositeQuestionary("C", "C", listOf(part1, part2))

        StatsCoordinator(tmp.root).record(composite.questions.first { it.text == "a" }, correct = true)

        assertTrue(File(tmp.root, "stats/P1.json").exists())
        assertFalse(File(tmp.root, "stats/C.json").exists())
    }

    @Test
    fun historyPersistsAcrossCoordinators() {
        val part = read("P1", q("a", "x"))
        StatsCoordinator(tmp.root).record(part.questions.single(), correct = false)

        // fresh coordinator reads from disk
        val d = StatsCoordinator(tmp.root).distribution(part, recentSince = 0)
        assertEquals(1, d.mistakes)
    }

    @Test
    fun doneRecentlyAggregatesAcrossParts() {
        val part1 = read("P1", q("a", "x"), q("b", "y"))
        val part2 = read("P2", q("c", "z"))
        val composite = CompositeQuestionary("C", "C", listOf(part1, part2))
        // goal = min(DEFAULT_SESSION_SIZE, 3) = 3

        val coordinator = StatsCoordinator(tmp.root)
        coordinator.record(composite.questions.first { it.text == "a" }, correct = true)
        coordinator.record(composite.questions.first { it.text == "b" }, correct = true)
        assertFalse(coordinator.distribution(composite, recentSince = 0).doneRecently)

        coordinator.record(composite.questions.first { it.text == "c" }, correct = true)
        assertTrue(coordinator.distribution(composite, recentSince = 0).doneRecently)
    }

    @Test
    fun sessionSizeCappedToQuestionCount() {
        val part = read("P1", q("a", "x"), q("b", "y"), q("c", "z"))
        val session = StatsCoordinator(tmp.root).selectSession(part, sessionSize = 10)
        assertEquals(3, session.size)
        assertEquals(setOf(0, 1, 2), session.toSet())
    }
}
