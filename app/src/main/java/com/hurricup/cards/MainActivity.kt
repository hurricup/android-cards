package com.hurricup.cards

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
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
import com.hurricup.cards.model.DEFAULT_SESSION_SIZE
import com.hurricup.cards.model.StatsBackup
import java.io.File
import com.hurricup.cards.model.Distribution
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.model.QuestionaryStats
import com.hurricup.cards.ui.theme.AndroidCardsTheme

class MainActivity : ComponentActivity() {
    private lateinit var questionaries: List<Questionary>
    private var distributions = mutableStateOf<Map<String, Distribution>>(emptyMap())

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { exportStats(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importStats(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionaries = Questionary.readAll(assets) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        } + Questionary.generateAll()
        enableEdgeToEdge()
        setContent {
            AndroidCardsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { SettingsBar() }
                ) { innerPadding ->
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsBar() {
        var expanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = {},
            actions = {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Export stats") },
                            onClick = { expanded = false; exportLauncher.launch("cards_stats.zip") }
                        )
                        DropdownMenuItem(
                            text = { Text("Import stats") },
                            onClick = { expanded = false; importLauncher.launch("application/zip") }
                        )
                    }
                }
            }
        )
    }

    private fun exportStats(uri: Uri) {
        try {
            val statsDir = File(filesDir, "stats")
            if (!statsDir.exists() || statsDir.listFiles().isNullOrEmpty()) {
                Toast.makeText(this, "No stats to export", Toast.LENGTH_SHORT).show()
                return
            }
            contentResolver.openOutputStream(uri)?.use { output ->
                StatsBackup.zip(statsDir, output)
            }
            Toast.makeText(this, "Stats exported", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun importStats(uri: Uri) {
        try {
            val statsDir = File(filesDir, "stats")
            contentResolver.openInputStream(uri)?.use { input ->
                StatsBackup.unzip(input, statsDir)
            }
            Toast.makeText(this, "Stats imported", Toast.LENGTH_SHORT).show()
            refreshDistributions()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshDistributions() {
        distributions.value = questionaries.associate { q ->
            val stats = QuestionaryStats.forQuestionary(filesDir, q)
            q.id to stats.distribution(q)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDistributions()
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
            val dist = distributions[questionary.id]
            val half = DEFAULT_SESSION_SIZE / 2
            val double = DEFAULT_SESSION_SIZE * 2
            fun launch(sessionSize: Int) {
                Intent(mainActivity, QuestionaryActivity::class.java).also {
                    questionary.passWith(it)
                    it.putExtra("session_size", sessionSize)
                    mainActivity.startActivity(it)
                }
            }
            TextButton(
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(0.9f),
                onClick = { launch(DEFAULT_SESSION_SIZE) }
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
                SessionMenu(half, double) { launch(it) }
            }
        }
    }
}

@Composable
private fun SessionMenu(half: Int, double: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Session options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Sprint ($half)") },
                onClick = { expanded = false; onSelect(half) }
            )
            DropdownMenuItem(
                text = { Text("Marathon ($double)") },
                onClick = { expanded = false; onSelect(double) }
            )
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