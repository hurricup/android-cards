package com.hurricup.cards.model

import android.content.res.AssetManager
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParser.END_DOCUMENT
import org.xmlpull.v1.XmlPullParser.START_TAG
import java.io.InputStream

class Questionary(val title: String) {
    private val _questions: MutableList<Question> = mutableListOf()
    val questions
        get() = _questions.toList()

    companion object {
        fun readAll(assetsManager: AssetManager): List<Questionary> =
            assetsManager.list("xml")?.flatMap {
                assetsManager.open("xml/$it").use { readFile(it).asSequence() }
            }?.toList() ?: emptyList()

        private fun readFile(inputStream: InputStream): List<Questionary> {
            val xmlParser = Xml.newPullParser()
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            xmlParser.setInput(inputStream, null)
            val questionaries = mutableListOf<Questionary>()
            while (xmlParser.next() != END_DOCUMENT) {
                if (xmlParser.eventType == START_TAG && xmlParser.name == "questionary") {
                    readQuestionary(xmlParser)?.let { questionaries.add(it) }
                }
            }
            return questionaries
        }

        private fun readQuestionary(xmlParser: XmlPullParser): Questionary? {
            var questionary: Questionary? = null
            while (xmlParser.next() != XmlPullParser.END_TAG) {
                if (xmlParser.eventType == START_TAG) {
                    when (xmlParser.name) {
                        "title" -> questionary = Questionary(readText(xmlParser))
                        "questions" -> questionary!!._questions += readQuestions(xmlParser)
                    }
                }
            }
            return questionary
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
            var question: Question? = null
            while (xmlParser.next() != XmlPullParser.END_TAG) {
                if (xmlParser.eventType == START_TAG) {
                    when (xmlParser.name) {
                        "text" -> question = Question(readText(xmlParser))
                    }
                }
            }
            return question
        }

        private fun readText(xmlParser: XmlPullParser): String {
            xmlParser.next()
            val result = xmlParser.text
            xmlParser.nextTag()
            return result
        }
    }
}