package com.hurricup.cards

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.hurricup.cards.model.Distribution
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.model.QuestionaryStats
import com.hurricup.cards.ui.theme.AndroidCardsTheme

class MainActivity : ComponentActivity() {
    private lateinit var questionaries: List<Questionary>
    private var distributions = mutableStateOf<Map<String, Distribution>>(emptyMap())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionaries = Questionary.readAll(assets) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        } + Questionary.generateAll()
        enableEdgeToEdge()
        setContent {
            AndroidCardsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Questionaries(
                        this,
                        questionaries,
                        distributions.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        distributions.value = questionaries.associate { q ->
            val stats = QuestionaryStats.forQuestionary(filesDir, q.title)
            q.title to stats.distribution(q)
        }
    }
}

@Composable
fun Questionaries(
    mainActivity: MainActivity,
    questionaries: List<Questionary>,
    distributions: Map<String, Distribution>,
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
            val dist = distributions[questionary.title]
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
                if (dist != null) {
                    PieChart(dist)
                }
                Text(
                    text = questionary.title,
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val pieRed = Color(0xFFBB6666)
private val pieGreen = Color(0xFF66BB66)
private val pieGray = Color(0xFFD5D5D5)

@Composable
private fun PieChart(dist: Distribution) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(end = 8.dp)) {
        Canvas(
            modifier = Modifier
                .size(30.dp)
                .clickable { showTooltip = !showTooltip }
        ) {
            val total = dist.total.toFloat()
            if (total == 0f) return@Canvas
            var startAngle = -90f
            fun drawSlice(count: Int, color: Color) {
                if (count > 0) {
                    val sweep = count / total * 360f
                    drawArc(color, startAngle, sweep, useCenter = true)
                    startAngle += sweep
                }
            }
            drawSlice(dist.mistakes, pieRed)
            drawSlice(dist.known, pieGreen)
            drawSlice(dist.new, pieGray)
        }

        if (showTooltip) {
            Popup(onDismissRequest = { showTooltip = false }) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Mistakes: ${dist.mistakes}", color = pieRed, fontSize = 14.sp)
                        Text("Known: ${dist.known}", color = pieGreen, fontSize = 14.sp)
                        Text("New: ${dist.new}", color = pieGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}