package com.hurricup.cards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.ui.theme.AndroidCardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val questionaries = Questionary.readAll(assets) + Questionary.generateAll()
        enableEdgeToEdge()
        setContent {
            AndroidCardsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Questionaries(
                        this,
                        questionaries,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Questionaries(
    mainActivity: MainActivity,
    questionaries: List<Questionary>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(10.dp)
            .fillMaxSize(1f)
    ) {
        for (questionary in questionaries) {
            TextButton(
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(0.9f),
                onClick = {
                    Intent(mainActivity, QuestionaryActivity::class.java).also {
                        questionary.passWith(it)
                        mainActivity.startActivity(it)
                    }
                }
            ) {
                Text(
                    text = questionary.title,
                    textAlign = TextAlign.Left,
                    fontSize = 30.sp
                )
            }
        }
    }
}