package com.hurricup.cards.model

import com.hurricup.cards.model.impl.ReferenceCompositeQuestionary
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
        val composite = ReferenceCompositeQuestionary("C", "C", listOf(part1.id, part2.id), reverse = false)

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
        val composite = ReferenceCompositeQuestionary("C", "C", listOf(part1.id, part2.id), reverse = false)

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
        val composite = ReferenceCompositeQuestionary("C", "C", listOf(part1.id, part2.id), reverse = false)
        // goal = min(DEFAULT_SESSION_SIZE, 3) = 3

        val coordinator = StatsCoordinator(tmp.root)
        coordinator.record(composite.questions.first { it.text == "a" }, correct = true)
        coordinator.record(composite.questions.first { it.text == "b" }, correct = true)
        assertFalse(coordinator.distribution(composite, recentSince = 0).doneRecently)

        coordinator.record(composite.questions.first { it.text == "c" }, correct = true)
        assertTrue(coordinator.distribution(composite, recentSince = 0).doneRecently)
    }

    @Test
    fun xmlCompositeAggregatesTrainedSubStats() {
        read("AX1", q("a", "x"), q("b", "y"))
        val sub1 = Questionary.cache["AX1"]!!
        read("AX2", q("c", "z"))
        val compositeXml = """
            <questionary>
                <id>AXC</id>
                <title>AXC</title>
                <questionaries><id>AX1</id><id>AX2</id></questionaries>
            </questionary>
        """.trimIndent()
        val composite = Questionary.readFile(compositeXml.byteInputStream(), direct = true).single()

        val coordinator = StatsCoordinator(tmp.root)
        coordinator.record(sub1.questions.first { it.text == "a" }, correct = true)

        val d = coordinator.distribution(composite, recentSince = 0)
        assertEquals(3, d.total)
        assertEquals(1, d.known)
        assertEquals(2, d.new)
    }

    @Test
    fun reverseCompositeAggregatesReverseSubStats() {
        read("RV1", q("cat", "кошка"))
        val compositeXml = """
            <questionary>
                <id>RVC</id>
                <title>RVC</title>
                <questionaries><id>RV1</id></questionaries>
            </questionary>
        """.trimIndent()
        Questionary.readFile(compositeXml.byteInputStream(), direct = true)
        val reverseComposite = Questionary.cache["RVC__reverse"]!!
        val reverseSub = Questionary.cache["RV1__reverse"]!!

        val coordinator = StatsCoordinator(tmp.root)
        coordinator.record(reverseSub.questions.single(), correct = true)

        val d = coordinator.distribution(reverseComposite, recentSince = 0)
        assertEquals(1, d.total)
        assertEquals(1, d.known)
        assertEquals(0, d.new)
    }

    @Test
    fun cyrillicIdsDoNotCollide() {
        read("аб", q("x", "1"))
        read("вг", q("y", "2"))
        val subAB = Questionary.cache["аб"]!!
        val subVG = Questionary.cache["вг"]!!

        StatsCoordinator(tmp.root).record(subAB.questions.single(), correct = true)
        StatsCoordinator(tmp.root).record(subVG.questions.single(), correct = false)

        // fresh coordinator reads from disk; аб's history must survive вг's save
        val d = StatsCoordinator(tmp.root).distribution(subAB, recentSince = 0)
        assertEquals(1, d.known)
        assertEquals(0, d.new)
    }

    @Test
    fun sessionSizeCappedToQuestionCount() {
        val part = read("P1", q("a", "x"), q("b", "y"), q("c", "z"))
        val session = StatsCoordinator(tmp.root).selectSession(part, sessionSize = 10)
        assertEquals(3, session.size)
        assertEquals(setOf(0, 1, 2), session.toSet())
    }
}
