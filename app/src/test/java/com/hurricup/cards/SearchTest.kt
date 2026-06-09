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
        // Both match at index 0; shorter leftover wins
        val results = search("ինչ", questions).map { it.text }
        assertEquals(listOf("ինչ", "ի՞նչ կա"), results)
    }

    @Test
    fun ignoresPunctuationInQuery() {
        val questions = listOf(q("ինչ"))
        val results = search("ի?նչ", questions).map { it.text }
        assertEquals(listOf("ինչ"), results)
    }

    @Test
    fun sortByMatchIndexThenLeftoverThenAlphabetical() {
        val questions = listOf(
            q("xxxap"),    // idx 3, leftover 3
            q("apricot"),  // idx 0, leftover 5
            q("ap"),       // idx 0, leftover 0
            q("apple"),    // idx 0, leftover 3
            q("xap"),      // idx 1, leftover 1
        )
        val results = search("ap", questions).map { it.text }
        // idx 0 first: shortest leftover wins (ap, then apple, then apricot)
        // then idx 1 (xap), then idx 3 (xxxap)
        assertEquals(listOf("ap", "apple", "apricot", "xap", "xxxap"), results)
    }

    @Test
    fun leftoverTiebreakAlphabetical() {
        val questions = listOf(
            q("apricot"),   // idx 0, leftover 5
            q("applepie"),  // idx 0, leftover 6
            q("apemen"),    // idx 0, leftover 4
        )
        val results = search("ap", questions).map { it.text }
        // Sort by leftover asc: apemen (4) < apricot (5) < applepie (6)
        assertEquals(listOf("apemen", "apricot", "applepie"), results)
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
