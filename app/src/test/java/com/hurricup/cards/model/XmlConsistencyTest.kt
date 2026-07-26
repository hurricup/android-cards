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

    @Test
    fun noDuplicateQuestions() {
        val files = xmlDir.listFiles { f -> f.extension == "xml" }
        assertTrue("No XML assets found at ${xmlDir.absolutePath}", !files.isNullOrEmpty())

        val duplicates = mutableListOf<String>()
        for (file in files!!) {
            val parsed = file.inputStream().use { Questionary.parseFile(it) }
            for (questionary in parsed) {
                val seen = HashSet<Pair<String, String?>>()
                for (q in questionary.questions) {
                    if (!seen.add(q.text to q.answer)) {
                        duplicates.add("${file.name} / ${questionary.title}: '${q.text}' -> '${q.answer}'")
                    }
                }
            }
        }
        assertTrue("Duplicate questions found:\n${duplicates.joinToString("\n")}", duplicates.isEmpty())
    }
}
