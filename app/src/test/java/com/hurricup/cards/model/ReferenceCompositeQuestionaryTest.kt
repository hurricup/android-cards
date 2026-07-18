package com.hurricup.cards.model

import com.hurricup.cards.model.impl.ReferenceCompositeQuestionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceCompositeQuestionaryTest {

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
    fun aggregatesReferencedParts() {
        read("R1", q("a", "x"), q("b", "y"))
        read("R2", q("c", "z"))
        val composite = ReferenceCompositeQuestionary("RC", "RC", listOf("R1", "R2"), reverse = false)

        assertEquals(setOf("a", "b", "c"), composite.questions.mapTo(HashSet()) { it.text })
        // questions keep their part ids
        assertEquals("R1", composite.questions.first { it.text == "a" }.questionaryId)
        assertEquals("R2", composite.questions.first { it.text == "c" }.questionaryId)
    }

    @Test
    fun xmlDeclaredCompositeAggregatesParts() {
        read("XR1", q("a", "x"), q("b", "y"))
        read("XR2", q("c", "z"))
        val compositeXml = """
            <questionary>
                <id>XRC</id>
                <title>XRC</title>
                <questionaries>
                    <id>XR1</id>
                    <id>XR2</id>
                </questionaries>
            </questionary>
        """.trimIndent()
        val composite = Questionary.readFile(compositeXml.byteInputStream(), direct = true).single()
        assertEquals(setOf("a", "b", "c"), composite.questions.mapTo(HashSet()) { it.text })
    }

    @Test
    fun missingReferenceIsSkipped() {
        read("R3", q("a", "x"))
        val composite = ReferenceCompositeQuestionary("RC2", "RC2", listOf("R3", "nope"), reverse = false)
        assertEquals(setOf("a"), composite.questions.mapTo(HashSet()) { it.text })
    }

    @Test
    fun reverseReferencesPartReverses() {
        read("R4", q("cat", "кошка"))
        val composite = ReferenceCompositeQuestionary("RC3", "RC3", listOf("R4"), reverse = true)
        // reverse of R4: question becomes the answer
        assertEquals(setOf("кошка"), composite.questions.mapTo(HashSet()) { it.text })
    }

    @Test
    fun cycleResolutionTerminates() {
        val a = ReferenceCompositeQuestionary("A", "cycleA", listOf("cycleB"), reverse = false)
        val b = ReferenceCompositeQuestionary("B", "cycleB", listOf("cycleA"), reverse = false)
        Questionary.cache["cycleA"] = a
        Questionary.cache["cycleB"] = b
        // must not overflow; a cycle yields no questions
        assertEquals(emptyList<Question>(), a.questions)
    }

    @Test
    fun validateReportsMissingAndCycles() {
        val a = ReferenceCompositeQuestionary("A", "vcA", listOf("vcB"), reverse = false)
        val b = ReferenceCompositeQuestionary("B", "vcB", listOf("vcA", "ghost"), reverse = false)
        Questionary.cache["vcA"] = a
        Questionary.cache["vcB"] = b

        val errors = mutableListOf<String>()
        Questionary.validateComposites(listOf(a, b)) { errors.add(it) }

        assertTrue(errors.any { it.contains("ghost") })
        assertTrue(errors.any { it.contains("Recursive") })
    }
}
