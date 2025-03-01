package com.hurricup.cards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.ui.theme.AndroidCardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val questionaries = Questionary.readAll(resources)
        enableEdgeToEdge()
        setContent {
            AndroidCardsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Questionaries(
                        questionaries,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Questionaries(questionaries: List<Questionary>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(10.dp)) {
        for (questionary in questionaries) {
            TextButton(
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(1f),
                onClick = {
                }
            ) {
                Text(
                    text = questionary.title,
                    modifier = modifier,
                    textAlign = TextAlign.Left,
                    fontSize = 30.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidCardsTheme {
        Questionaries(listOf(Questionary("Test Questionary"), Questionary("Other Questionary")))
    }
}