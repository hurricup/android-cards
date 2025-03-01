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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            var index: Int by remember { mutableStateOf(0) }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = questionary[index].text,
                        fontSize = 8.em,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    IconButton(
                        onClick = {
                            if (index == questionary.size - 1) {
                                finish()
                            } else {
                                index++
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
                            if (index == questionary.size - 1) {
                                finish()
                            } else {
                                index++
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