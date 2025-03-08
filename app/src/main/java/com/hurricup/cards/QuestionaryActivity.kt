package com.hurricup.cards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hurricup.cards.model.Questionary

private val darkGreen = Color(0xFF66BB66)
private val darkRed = Color(0xFFBB6666)
private val darkGray = Color(0xFFD5D5D5)

class QuestionaryActivity() : ComponentActivity() {
    private val questionary: Questionary
        get() = Questionary.from(intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val indexes = rememberSaveable(
                saver = Saver(
                save = {
                    ArrayList(it.toList())
                },
                restore = {
                    it.toMutableStateList()
                }
            )) { questionary.questions.indices.shuffled().toMutableStateList() }
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

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(10.dp)
                        .weight(1f)
                ) {
                    val text = questionary[indexes[0]].text.beautify()
                    text.split('\n').forEach {
                        Text(
                            text = it.trim(),
                            fontSize = 6.em,
                            modifier = Modifier
                                .wrapContentWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    IconButton(
                        onClick = {
                            stats.value.correct++
                            if (indexes.size == 1) {
                                finish()
                            } else {
                                indexes.removeAt(0)
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
                        onClick = {
                            stats.value.incorrect++
                            if (indexes.size == 1) {
                                finish()
                            } else {
                                indexes.removeAt(0)
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
    var correct: Int = 0
    var incorrect: Int = 0

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