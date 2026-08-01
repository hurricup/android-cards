package com.hurricup.cards.model

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Consistency checks over the bundled XML questionary assets.
 * Runs against the real files under src/main/assets/xml.
 */
class XmlConsistencyTest {

    private val xmlDir = File("src/main/assets/xml")

    /** Accent marks dropped so accented and plain forms count as the same: Armenian emphasis (՛) and combining acute (Russian stress). */
    private val accents = Regex("[՛́]")

    private fun normalize(s: String?): String? = s
        ?.replace(accents, "")
        ?.replace(Regex("\\s+"), " ")            // collapse whitespace
        ?.replace(Regex("\\s*([\\p{P}])\\s*"), "$1") // drop spaces around punctuation
        ?.trim()

    private fun xmlFiles(): Array<File> {
        val files = xmlDir.listFiles { f -> f.extension == "xml" }
        assertTrue("No XML assets found at ${xmlDir.absolutePath}", !files.isNullOrEmpty())
        return files!!
    }

    /** Same question+answer repeated within one questionary. */
    @Test
    fun noDuplicateQuestionsWithinQuestionary() {
        val duplicates = mutableListOf<String>()
        for (file in xmlFiles()) {
            val parsed = file.inputStream().use { Questionary.parseFile(it) }
            for (questionary in parsed) {
                val seen = HashSet<Pair<String?, String?>>()
                for (q in questionary.questions) {
                    if (!seen.add(normalize(q.text) to normalize(q.answer))) {
                        duplicates.add("${file.name} / ${questionary.title}: '${q.text}' -> '${q.answer}'")
                    }
                }
            }
        }
        assertTrue("Duplicate questions found:\n${duplicates.joinToString("\n")}", duplicates.isEmpty())
    }

    /** Same question+answer appearing in more than one file — it should live in a single source. */
    @Test
    fun noQuestionSharedAcrossFiles() {
        val byCard = HashMap<Pair<String?, String?>, MutableSet<String>>() // (text, answer) -> files
        for (file in xmlFiles()) {
            val parsed = file.inputStream().use { Questionary.parseFile(it) }
            for (questionary in parsed) {
                for (q in questionary.questions) {
                    byCard.getOrPut(normalize(q.text) to normalize(q.answer)) { mutableSetOf() }.add(file.name)
                }
            }
        }
        val shared = byCard.filterValues { it.size > 1 }
        val report = shared.entries.joinToString("\n") { (card, files) ->
            "'${card.first}' -> '${card.second}': ${files.joinToString(", ")}"
        }
        assertTrue("Question+answer shared across files:\n$report", shared.isEmpty())
    }
}
