package com.hurricup.cards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hurricup.cards.model.Questionary

class QuestionaryActivity() : ComponentActivity() {
    private val questionary: Questionary
        get() = Questionary.from(intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val indexes = rememberSaveable(saver = Saver(
                save = {
                    ArrayList(it.toList())
                },
                restore = {
                    it.toMutableStateList()
                }
            )) { questionary.questions.indices.shuffled().toMutableStateList() }
            val stats = rememberSaveable(saver = Saver(
                save = {
                    ArrayList(listOf(it.value.correct, it.value.incorrect))
                },
                restore = {
                    val result = Stat()
                    result.correct = it[0]
                    result.incorrect = it[1]
                    mutableStateOf(result)
                }

            )) { mutableStateOf(Stat()) }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {

                if (stats.value.total > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            text = "${stats.value.correct} of ${stats.value.total} (${stats.value.percent}%), ${indexes.size} left",
                            fontSize = 4.em,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
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
                    val text = questionary[indexes[0]].text
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
                            Icons.Filled.Done, "Right", tint = Color.Green,
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
                            Icons.Filled.Close, "Wrong", tint = Color.Red,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                }
            }
        }
    }
}

class Stat {
    var correct: Int = 0
    var incorrect: Int = 0
    val total: Int
        get() = correct + incorrect

    val percent: Int
        get() = correct * 100 / total
}