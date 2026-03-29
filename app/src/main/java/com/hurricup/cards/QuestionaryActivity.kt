package com.hurricup.cards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.model.QuestionaryStats

private val darkGreen = Color(0xFF66BB66)
private val darkRed = Color(0xFFBB6666)
private val darkGray = Color(0xFFD5D5D5)

class QuestionaryActivity() : ComponentActivity() {
    private val questionary: Questionary
        get() = Questionary.from(intent)

    private val stats: QuestionaryStats by lazy {
        QuestionaryStats.forQuestionary(filesDir, questionary.title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val answerRevealed = rememberSaveable { mutableStateOf(false) }
            val indexes = rememberSaveable(
                saver = Saver(
                    save = {
                        ArrayList(it.toList())
                    },
                    restore = {
                        it.toMutableStateList()
                    }
                )) { stats.sortedIndices(questionary.questions).toMutableStateList() }
            val stats = rememberSaveable(
                saver = Saver(
                    save = {
                        ArrayList(listOf(it.value.correct, it.value.incorrect))
                    },
                    restore = {
                        val result = Stat(questionary.size)
                        result.correct = it[0]
                        result.incorrect = it[1]
                        mutableStateOf(result)
                    }

                )) { mutableStateOf(Stat(questionary.size)) }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                ProgressBar(stats)

                val currentQuestion = questionary[indexes[0]]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(10.dp)
                        .weight(1f)
                ) {
                    Question(currentQuestion)
                    Controls(currentQuestion, answerRevealed, stats, indexes)
                    Answer(currentQuestion, answerRevealed)
                }
            }
        }
    }

    @Composable
    private fun Controls(
        currentQuestion: Question,
        answerRevealed: MutableState<Boolean>,
        stats: MutableState<Stat>,
        indexes: SnapshotStateList<Int>
    ) {
        val isDisabled = currentQuestion.answer != null && !answerRevealed.value
        val controlsModifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, vertical = 30.dp)
            .wrapContentHeight().let {
                if (isDisabled) {
                    it.alpha(0f)
                } else {
                    it
                }
            }

        Row(modifier = controlsModifier) {
            IconButton(
                enabled = !isDisabled,
                onClick = {
                    this@QuestionaryActivity.stats.recordAttempt(currentQuestion.text, true)
                    stats.value.correct++
                    if (indexes.size == 1) {
                        finish()
                    } else {
                        indexes.removeAt(0)
                        answerRevealed.value = false
                    }
                }, modifier = Modifier
                    .weight(1f)
                    .size(100.dp)
            ) {
                Icon(
                    Icons.Filled.Done, "Right", tint = darkGreen,
                    modifier = Modifier.size(80.dp)
                )
            }
            IconButton(
                enabled = !isDisabled,
                onClick = {
                    this@QuestionaryActivity.stats.recordAttempt(currentQuestion.text, false)
                    stats.value.incorrect++
                    if (indexes.size == 1) {
                        finish()
                    } else {
                        indexes.removeAt(0)
                        answerRevealed.value = false
                    }
                }, modifier = Modifier
                    .weight(1f)
                    .size(100.dp)
            ) {
                Icon(
                    Icons.Filled.Close, "Wrong", tint = darkRed,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }

    @Composable
    private fun Answer(
        currentQuestion: Question,
        answerRevealed: MutableState<Boolean>
    ) {
        currentQuestion.answer?.let { answer ->
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxWidth()
                    .blur(if (answerRevealed.value) 0.dp else 16.dp)
                    .clickable(enabled = !answerRevealed.value) {
                        answerRevealed.value = true
                    }
            ) {
                val answerText = if (answerRevealed.value) answer else currentQuestion.text
                answerText.beautify().split('\n').forEach {
                    Text(
                        text = it.trim(),
                        fontSize = 6.em,
                        modifier = Modifier
                            .padding(horizontal = 0.dp, vertical = 20.dp)
                            .wrapContentWidth()
                    )
                }
            }
        }
    }

    @Composable
    private fun Question(currentQuestion: Question) {
        val questionText = currentQuestion.text.beautify()
        questionText.split('\n').forEach {
            Text(
                text = it.trim(),
                fontSize = 6.em,
                modifier = Modifier
                    .padding(horizontal = 0.dp, vertical = 20.dp)
                    .wrapContentWidth(),
            )
        }
    }

    @Composable
    private fun ProgressBar(stats: MutableState<Stat>) {
        Box(
            modifier = Modifier
                .padding(10.dp, 30.dp)
                .fillMaxWidth()

        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(shape = RoundedCornerShape(10.dp))
            ) {
                stats.value.let {
                    if (it.correct > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(it.correctWeight)
                                .fillMaxHeight()
                                .background(darkGreen)
                        ) {
                            Text(it.correct.toString())
                        }
                    }
                    if (it.incorrect > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(it.incorrectWeight)
                                .fillMaxHeight()
                                .background(darkRed)
                        ) {
                            Text(it.incorrect.toString())
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(it.leftWeight)
                            .fillMaxHeight()
                            .background(darkGray)
                    ) {
                        Text(it.left.toString())
                    }
                }
            }
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

class Stat(val total: Int) {
    var correct: Int by mutableStateOf(0)
    var incorrect: Int by mutableStateOf(0)

    val done: Int
        get() = correct + incorrect

    val left: Int
        get() = total - done

    val leftWeight: Float
        get() = left.toFloat() / total.toFloat()
    val correctWeight: Float
        get() = correct.toFloat() / total.toFloat()
    val incorrectWeight: Float
        get() = incorrect.toFloat() / total.toFloat()
}