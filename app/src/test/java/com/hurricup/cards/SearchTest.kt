package com.hurricup.cards

import com.hurricup.cards.model.Question
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {

    private fun q(text: String, answer: String = "x") = Question(text, answer)

    @Test
    fun emptyQuery_returnsEmpty() {
        val questions = listOf(q("apple"), q("banana"))
        assertEquals(emptyList<Question>(), search("", questions))
    }

    @Test
    fun blankQuery_returnsEmpty() {
        val questions = listOf(q("apple"), q("banana"))
        assertEquals(emptyList<Question>(), search("   ", questions))
    }

    @Test
    fun queryOnlyPunctuation_returnsEmpty() {
        val questions = listOf(q("apple"), q("banana"))
        assertEquals(emptyList<Question>(), search("???", questions))
    }

    @Test
    fun substringMatch() {
        val questions = listOf(q("apple"), q("banana"), q("apricot"))
        val results = search("ap", questions).map { it.text }
        assertEquals(listOf("apple", "apricot"), results)
    }

    @Test
    fun caseInsensitive() {
        val questions = listOf(q("Apple"), q("BANANA"))
        val results = search("AP", questions).map { it.text }
        assertEquals(listOf("Apple"), results)
    }

    @Test
    fun ignoresPunctuationInText() {
        val questions = listOf(q("ի՞նչ կա"), q("ինչ"))
        // Both match at index 0; sort alphabetically (՞ U+055E < ն U+0576)
        val results = search("ինչ", questions).map { it.text }
        assertEquals(listOf("ի՞նչ կա", "ինչ"), results)
    }

    @Test
    fun ignoresPunctuationInQuery() {
        val questions = listOf(q("ինչ"))
        val results = search("ի?նչ", questions).map { it.text }
        assertEquals(listOf("ինչ"), results)
    }

    @Test
    fun sortByMatchIndexThenAlphabetical() {
        val questions = listOf(
            q("xxxap"),   // match at 3
            q("apple"),   // match at 0
            q("apricot"), // match at 0
            q("xap"),     // match at 1
        )
        val results = search("ap", questions).map { it.text }
        assertEquals(listOf("apple", "apricot", "xap", "xxxap"), results)
    }

    @Test
    fun noMatch_returnsEmpty() {
        val questions = listOf(q("apple"), q("banana"))
        assertEquals(emptyList<Question>(), search("xyz", questions))
    }

    @Test
    fun russianAndEnglishPunctuationStripped() {
        val questions = listOf(q("hello, world!"), q("hello world"))
        val results = search("hello world", questions).map { it.text }
        // both should match — "hello, world!" normalizes to "hello world"
        assertEquals(listOf("hello world", "hello, world!"), results)
    }
}
