package com.hurricup.cards.model

import android.content.Intent
import android.content.res.AssetManager
import android.util.Log
import com.hurricup.cards.model.impl.Addition
import com.hurricup.cards.model.impl.CompositeQuestionary
import com.hurricup.cards.model.impl.Division
import com.hurricup.cards.model.impl.Multiplication
import com.hurricup.cards.model.impl.Subtraction
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParser.END_DOCUMENT
import org.xmlpull.v1.XmlPullParser.START_TAG
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

private const val INTENT_KEY = "questionary"
private const val REVERSE_SUFFIX = "__reverse"

open class Questionary(val title: String, val id: String = title) {
    protected open val _questions: MutableList<Question> = mutableListOf()
    val questions
        get() = _questions.toList()
    val size
        get() = _questions.size

    operator fun get(index: Int) = _questions[index]

    fun passWith(intent: Intent) = intent.putExtra(INTENT_KEY, id)

    companion object {
        val cache: MutableMap<String, Questionary> = mutableMapOf()

        fun from(intent: Intent): Questionary = cache[intent.getStringExtra(INTENT_KEY)]!!

        private fun cache(questionary: Questionary?): Questionary? = questionary?.also {
            cache.put(it.id, it)
        }

        fun reverseOf(questionary: Questionary): Questionary =
            cache["${questionary.id}$REVERSE_SUFFIX"]!!

        private fun cachePair(title: String, id: String, rawQuestions: List<Question>): Questionary {
            val direct = Questionary(title, id).also {
                it._questions += processVariants(rawQuestions, direct = true)
            }
            val reverse = Questionary(title, "$id$REVERSE_SUFFIX").also {
                it._questions += processVariants(rawQuestions, direct = false)
            }
            cache(direct)
            cache(reverse)
            return direct
        }

        fun generateAll(): List<Questionary> = listOf(
            CompositeQuestionary("+/−", listOf(Addition(), Subtraction())),
            CompositeQuestionary("×/÷", listOf(Multiplication(), Division())),
        ).map { cachePair(it.title, it.id, it.questions) }

        fun readAll(assetsManager: AssetManager, onError: (String) -> Unit = {}): List<Questionary> =
            assetsManager.list("xml")?.flatMap { fileName ->
                try {
                    assetsManager.open("xml/$fileName").use { readFile(it) }
                } catch (e: Exception) {
                    Log.e("Questionary", "Error reading $fileName", e)
                    onError("Error reading $fileName: ${e.message}")
                    emptyList()
                }
            } ?: emptyList()

        internal fun readFile(inputStream: InputStream, direct: Boolean = true): List<Questionary> {
            val xmlParser = XmlPullParserFactory.newInstance().newPullParser()
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            xmlParser.setInput(inputStream, null)
            val questionaries = mutableListOf<Questionary>()
            while (xmlParser.next() != END_DOCUMENT) {
                if (xmlParser.eventType == START_TAG && xmlParser.name == INTENT_KEY) {
                    readQuestionary(xmlParser)?.let {
                        questionaries.add(if (direct) it else reverseOf(it))
                    }
                }
            }
            return questionaries
        }

        private fun readQuestionary(xmlParser: XmlPullParser): Questionary? {
            var title: String? = null
            var id: String? = null
            var questions: List<Question> = emptyList()
            while (xmlParser.next() != XmlPullParser.END_TAG) {
                if (xmlParser.eventType == START_TAG) {
                    when (xmlParser.name) {
                        "title" -> title = readText(xmlParser)
                        "id" -> id = readText(xmlParser)
                        "questions" -> questions = readQuestions(xmlParser)
                    }
                }
            }
            return title?.let { cachePair(it, id ?: it, questions) }
        }

        /**
         * Expands pipe-separated variants and groups by effective question (direct)
         * or effective answer (reverse). Variants on each side produce a cross-product
         * of raw pairs; pairs are then grouped and the other side joined with "; ".
         */
        private fun processVariants(questions: List<Question>, direct: Boolean): List<Question> {
            val grouped = HashMap<String, MutableList<String>>()
            for (q in questions) {
                val questionVariants = q.text.split('|').map { it.trim() }
                val answerVariants = q.answer?.split('|')?.map { it.trim() } ?: listOf(null)
                for (qv in questionVariants) {
                    for (av in answerVariants) {
                        val key: String
                        val value: String?
                        if (direct) {
                            key = qv
                            value = av
                        } else {
                            if (av == null) continue
                            key = av
                            value = qv
                        }
                        val list = grouped.getOrPut(key) { mutableListOf() }
                        if (value != null && value !in list) list.add(value)
                    }
                }
            }
            return grouped.map { (key, values) ->
                Question(key, if (values.isEmpty()) null else values.joinToString("; "))
            }
        }

        private fun readQuestions(xmlParser: XmlPullParser): List<Question> {
            val questions = mutableListOf<Question>()
            while (xmlParser.next() != XmlPullParser.END_TAG) {
                if (xmlParser.eventType == START_TAG && xmlParser.name == "question") {
                    readQuestion(xmlParser)?.let { questions.add(it) }
                }
            }
            return questions
        }

        private fun readQuestion(xmlParser: XmlPullParser): Question? {
            var questionText: String? = null
            var answerText: String? = null
            while (xmlParser.next() != XmlPullParser.END_TAG) {
                if (xmlParser.eventType == START_TAG) {
                    when (xmlParser.name) {
                        "text" -> questionText = readText(xmlParser)
                        "answer" -> answerText = readText(xmlParser)
                    }
                }
            }
            return questionText?.let { Question(questionText, answerText) }
        }

        private fun readText(xmlParser: XmlPullParser): String {
            xmlParser.next()
            val result = xmlParser.text.trim().beautify()
            xmlParser.nextTag()
            return result
        }
    }
}

/**
 * Makes typographic adjustments:
 * - replaces a double minus with a dash
 * - replaces a three periods with an ellipsis
 * - replace a space before an ellipsis to a non-breakable space
 */
private fun String.beautify() = this
    .replace("--", "—")
    .replace("...", "…")
    .replace(" …", " …")