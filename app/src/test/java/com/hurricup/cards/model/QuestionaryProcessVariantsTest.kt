package com.hurricup.cards.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionaryProcessVariantsTest {

    private fun process(xml: String): String {
        val direct = readSorted(xml, direct = true)
        val reverse = readSorted(xml, direct = false)
        return "Direct:\n$direct\nReverse:\n$reverse"
    }

    private fun readSorted(xml: String, direct: Boolean): String {
        val questionaries = Questionary.readFile(xml.byteInputStream(), direct)
        return questionaries.single().questions
            .sortedBy { it.text }
            .joinToString("\n") { "${it.text} -> ${it.answer ?: "<none>"}" }
    }

    private fun xml(questions: String): String = """
        <questionary>
            <title>test</title>
            <questions>
                $questions
            </questions>
        </questionary>
    """.trimIndent()

    private fun q(text: String, answer: String) =
        "<question><text>$text</text><answer>$answer</answer></question>"

    @Test
    fun pipesNowhere() {
        val xml = xml("${q("a", "X")}${q("b", "Y")}")
        assertEquals(
            """
            Direct:
            a -> X
            b -> Y
            Reverse:
            X -> a
            Y -> b
            """.trimIndent(),
            process(xml)
        )
    }

    @Test
    fun pipeInQuestion() {
        val xml = xml(q("a|b", "X"))
        assertEquals(
            """
            Direct:
            a -> X
            b -> X
            Reverse:
            X -> a; b
            """.trimIndent(),
            process(xml)
        )
    }

    @Test
    fun pipeInAnswer() {
        val xml = xml(q("a", "X|Y"))
        assertEquals(
            """
            Direct:
            a -> X; Y
            Reverse:
            X -> a
            Y -> a
            """.trimIndent(),
            process(xml)
        )
    }

    @Test
    fun pipeInBoth() {
        val xml = xml(q("a|b", "X|Y"))
        assertEquals(
            """
            Direct:
            a -> X; Y
            b -> X; Y
            Reverse:
            X -> a; b
            Y -> a; b
            """.trimIndent(),
            process(xml)
        )
    }

    @Test
    fun mergedAnswers() {
        val xml = xml("${q("a", "X")}${q("a", "Y")}")
        assertEquals(
            """
            Direct:
            a -> X; Y
            Reverse:
            X -> a
            Y -> a
            """.trimIndent(),
            process(xml)
        )
    }

    @Test
    fun mergedQuestions() {
        val xml = xml("${q("a", "X")}${q("b", "X")}")
        assertEquals(
            """
            Direct:
            a -> X
            b -> X
            Reverse:
            X -> a; b
            """.trimIndent(),
            process(xml)
        )
    }

    @Test
    fun mergedBoth() {
        val xml = xml("${q("a|b", "X|Y")}${q("a", "Z")}${q("c", "X")}")
        assertEquals(
            """
            Direct:
            a -> X; Y; Z
            b -> X; Y
            c -> X
            Reverse:
            X -> a; b; c
            Y -> a; b
            Z -> a
            """.trimIndent(),
            process(xml)
        )
    }
}
