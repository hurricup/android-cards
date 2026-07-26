package com.hurricup.cards

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.edit
import com.hurricup.cards.model.*
import com.hurricup.cards.ui.theme.AndroidCardsTheme
import kotlin.math.roundToInt
import java.io.File

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val DEFAULT_RECENT_WINDOW_DAYS = 3
private const val RECENT_WINDOW_DAYS_KEY = "recent_window_days"

class MainActivity : ComponentActivity() {
    private lateinit var questionaries: List<Questionary>
    private var distributions = mutableStateOf<Map<String, Distribution>>(emptyMap())
    private var mode = mutableStateOf(Mode.DIRECT)
    private var modeOverrides = mutableStateOf<Map<String, Mode>>(emptyMap())
    private var recentWindowDays = mutableStateOf(DEFAULT_RECENT_WINDOW_DAYS)
    private var mistakesCapPercent = mutableStateOf(DEFAULT_MISTAKES_CAP_PERCENT)
    private var mistakesCapOverrides = mutableStateOf<Map<String, Int>>(emptyMap())
    private var maxAgeDays = mutableStateOf(DEFAULT_MAX_AGE_DAYS)

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

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
        loadModes()
        recentWindowDays.value = prefs.getInt(RECENT_WINDOW_DAYS_KEY, DEFAULT_RECENT_WINDOW_DAYS)
        mistakesCapPercent.value = prefs.getInt(MISTAKES_CAP_PERCENT_KEY, DEFAULT_MISTAKES_CAP_PERCENT)
        loadMistakesCapOverrides()
        maxAgeDays.value = prefs.getInt(MAX_AGE_DAYS_KEY, DEFAULT_MAX_AGE_DAYS)
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
                        mode.value,
                        modeOverrides.value,
                        ::setModeOverride,
                        mistakesCapPercent.value,
                        mistakesCapOverrides.value,
                        ::setMistakesCapOverride,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun loadModes() {
        mode.value = prefs.getString(MODE_KEY, null)?.let { runCatching { Mode.valueOf(it) }.getOrNull() }
            ?: Mode.DIRECT
        modeOverrides.value = prefs.all.entries
            .filter { it.key.startsWith(MODE_OVERRIDE_PREFIX) && it.value is String }
            .mapNotNull { e ->
                runCatching { Mode.valueOf(e.value as String) }.getOrNull()
                    ?.let { e.key.removePrefix(MODE_OVERRIDE_PREFIX) to it }
            }
            .toMap()
    }

    private fun setMode(m: Mode) {
        prefs.edit { putString(MODE_KEY, m.name) }
        mode.value = m
    }

    /** [m] null clears the override (falls back to the global mode). */
    private fun setModeOverride(id: String, m: Mode?) {
        prefs.edit {
            if (m == null) remove(MODE_OVERRIDE_PREFIX + id)
            else putString(MODE_OVERRIDE_PREFIX + id, m.name)
        }
        modeOverrides.value =
            if (m == null) modeOverrides.value - id
            else modeOverrides.value + (id to m)
    }

    private fun setRecentWindowDays(days: Int) {
        prefs.edit { putInt(RECENT_WINDOW_DAYS_KEY, days) }
        recentWindowDays.value = days
        refreshDistributions()
    }

    private fun setMistakesCapPercent(percent: Int) {
        prefs.edit { putInt(MISTAKES_CAP_PERCENT_KEY, percent) }
        mistakesCapPercent.value = percent
    }

    private fun setMaxAgeDays(days: Int) {
        prefs.edit { putInt(MAX_AGE_DAYS_KEY, days) }
        maxAgeDays.value = days
        refreshDistributions()
    }

    private fun loadMistakesCapOverrides() {
        mistakesCapOverrides.value = prefs.all.entries
            .filter { it.key.startsWith(MISTAKES_CAP_OVERRIDE_PREFIX) && it.value is Int }
            .associate { it.key.removePrefix(MISTAKES_CAP_OVERRIDE_PREFIX) to it.value as Int }
    }

    /** [percent] null clears the override (falls back to the app-level setting). */
    private fun setMistakesCapOverride(id: String, percent: Int?) {
        prefs.edit {
            if (percent == null) remove(MISTAKES_CAP_OVERRIDE_PREFIX + id)
            else putInt(MISTAKES_CAP_OVERRIDE_PREFIX + id, percent)
        }
        mistakesCapOverrides.value =
            if (percent == null) mistakesCapOverrides.value - id
            else mistakesCapOverrides.value + (id to percent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsBar() {
        var expanded by remember { mutableStateOf(false) }
        var windowSubmenu by remember { mutableStateOf(false) }
        var capSubmenu by remember { mutableStateOf(false) }
        var maxAgeSubmenu by remember { mutableStateOf(false) }
        var modeSubmenu by remember { mutableStateOf(false) }
        TopAppBar(
            title = {},
            actions = {
                IconButton(onClick = {
                    startActivity(Intent(this@MainActivity, SearchActivity::class.java))
                }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    DropdownMenu(
                        expanded = expanded && !windowSubmenu && !capSubmenu && !maxAgeSubmenu && !modeSubmenu,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export stats") },
                            onClick = { expanded = false; exportLauncher.launch("cards_stats.zip") }
                        )
                        DropdownMenuItem(
                            text = { Text("Import stats") },
                            onClick = { expanded = false; importLauncher.launch("application/zip") }
                        )
                        DropdownMenuItem(
                            text = { Text("Mode: ${mode.value.label}") },
                            onClick = { modeSubmenu = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Recent window: ${days(recentWindowDays.value)}") },
                            onClick = { windowSubmenu = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Hard questions: ${mistakesCapPercent.value}%") },
                            onClick = { capSubmenu = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Max age: ${days(maxAgeDays.value)}") },
                            onClick = { maxAgeSubmenu = true }
                        )
                    }
                    DropdownMenu(
                        expanded = modeSubmenu,
                        onDismissRequest = { modeSubmenu = false; expanded = false }
                    ) {
                        for (m in Mode.entries) {
                            DropdownMenuItem(
                                text = { Text(m.label) },
                                trailingIcon = if (m == mode.value) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    setMode(m)
                                    modeSubmenu = false
                                    expanded = false
                                }
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = windowSubmenu,
                        onDismissRequest = { windowSubmenu = false; expanded = false }
                    ) {
                        for (d in 1..7) {
                            DropdownMenuItem(
                                text = { Text(days(d)) },
                                trailingIcon = if (d == recentWindowDays.value) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    setRecentWindowDays(d)
                                    windowSubmenu = false
                                    expanded = false
                                }
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = capSubmenu,
                        onDismissRequest = { capSubmenu = false; expanded = false }
                    ) {
                        Column(modifier = Modifier.width(260.dp).padding(horizontal = 16.dp)) {
                            Text("Hard questions: ${mistakesCapPercent.value}%")
                            Slider(
                                value = mistakesCapPercent.value.toFloat(),
                                onValueChange = { mistakesCapPercent.value = it.roundToInt() },
                                onValueChangeFinished = { setMistakesCapPercent(mistakesCapPercent.value) },
                                valueRange = 0f..100f
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = maxAgeSubmenu,
                        onDismissRequest = { maxAgeSubmenu = false; expanded = false }
                    ) {
                        Column(modifier = Modifier.width(260.dp).padding(horizontal = 16.dp)) {
                            Text("Max age: ${days(maxAgeDays.value)}")
                            Slider(
                                value = maxAgeDays.value.toFloat(),
                                onValueChange = { maxAgeDays.value = it.roundToInt() },
                                onValueChangeFinished = { setMaxAgeDays(maxAgeDays.value) },
                                valueRange = 1f..90f
                            )
                        }
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
        val since = System.currentTimeMillis() - recentWindowDays.value * DAY_MS
        val coordinator = StatsCoordinator(filesDir, maxAgeDays.value.toDouble())
        distributions.value = questionaries.flatMap { q ->
            listOf(q, Questionary.reverseOf(q), Questionary.mixedOf(q)).map {
                it.id to coordinator.distribution(it, since)
            }
        }.toMap()
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
    globalMode: Mode,
    modeOverrides: Map<String, Mode>,
    onSetModeOverride: (String, Mode?) -> Unit,
    appMistakesCapPercent: Int,
    mistakesCapOverrides: Map<String, Int>,
    onSetMistakesCapOverride: (String, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(10.dp)
            .fillMaxSize(1f)
            .verticalScroll(rememberScrollState())
    ) {
        for (questionary in questionaries) {
            val effectiveMode = modeOverrides[questionary.id] ?: globalMode
            val active = when (effectiveMode) {
                Mode.DIRECT -> questionary
                Mode.REVERSE -> Questionary.reverseOf(questionary)
                Mode.MIXED -> Questionary.mixedOf(questionary)
            }
            val dist = distributions[active.id]
            val isDone = dist?.doneRecently == true
            val half = DEFAULT_SESSION_SIZE / 2
            val double = DEFAULT_SESSION_SIZE * 2
            fun launch(sessionSize: Int) {
                Intent(mainActivity, QuestionaryActivity::class.java).also {
                    active.passWith(it)
                    it.putExtra("session_size", sessionSize)
                    mainActivity.startActivity(it)
                }
            }
            TextButton(
                border = ButtonDefaults.outlinedButtonBorder,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth(0.9f),
                onClick = { launch(DEFAULT_SESSION_SIZE) }
            ) {
                if (dist != null) {
                    PieChart(dist)
                }
                val modeGlyph = when (effectiveMode) {
                    Mode.DIRECT -> null
                    Mode.REVERSE -> "←"
                    Mode.MIXED -> "⇄"
                }
                if (modeGlyph != null) {
                    Text(
                        text = modeGlyph,
                        fontSize = 28.sp,
                        modifier = Modifier.offset(x = (-1).dp, y = (-3).dp)
                    )
                }
                Text(
                    text = questionary.title,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDone) Color.Unspecified
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    modifier = Modifier.weight(1f)
                )
                SessionMenu(
                    half = half,
                    double = double,
                    overrideMode = modeOverrides[questionary.id],
                    onSetModeOverride = { onSetModeOverride(questionary.id, it) },
                    appMistakesCapPercent = appMistakesCapPercent,
                    overrideMistakesCapPercent = mistakesCapOverrides[active.id],
                    onSetMistakesCapOverride = { onSetMistakesCapOverride(active.id, it) },
                    onSelect = { launch(it) },
                )
            }
        }
    }
}

@Composable
private fun SessionMenu(
    half: Int,
    double: Int,
    overrideMode: Mode?,
    onSetModeOverride: (Mode?) -> Unit,
    appMistakesCapPercent: Int,
    overrideMistakesCapPercent: Int?,
    onSetMistakesCapOverride: (Int?) -> Unit,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var capSubmenu by remember { mutableStateOf(false) }
    var modeSubmenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Session options")
        }
        DropdownMenu(
            expanded = expanded && !capSubmenu && !modeSubmenu,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Sprint ($half)") },
                onClick = { expanded = false; onSelect(half) }
            )
            DropdownMenuItem(
                text = { Text("Marathon ($double)") },
                onClick = { expanded = false; onSelect(double) }
            )
            DropdownMenuItem(
                text = { Text("Mode: ${overrideMode?.label ?: "Global"}") },
                onClick = { modeSubmenu = true }
            )
            val capLabel = overrideMistakesCapPercent?.let { "$it%" } ?: "default"
            DropdownMenuItem(
                text = { Text("Hard questions: $capLabel") },
                onClick = { capSubmenu = true }
            )
        }
        DropdownMenu(
            expanded = modeSubmenu,
            onDismissRequest = { modeSubmenu = false; expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Global") },
                trailingIcon = if (overrideMode == null) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
                onClick = { onSetModeOverride(null); modeSubmenu = false; expanded = false }
            )
            for (m in Mode.entries) {
                DropdownMenuItem(
                    text = { Text(m.label) },
                    trailingIcon = if (m == overrideMode) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = { onSetModeOverride(m); modeSubmenu = false; expanded = false }
                )
            }
        }
        DropdownMenu(
            expanded = capSubmenu,
            onDismissRequest = { capSubmenu = false; expanded = false }
        ) {
            Column(modifier = Modifier.width(280.dp).padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = overrideMistakesCapPercent != null,
                        onCheckedChange = { checked ->
                            onSetMistakesCapOverride(if (checked) appMistakesCapPercent else null)
                        }
                    )
                    Text("Override app default")
                }
                Text(
                    overrideMistakesCapPercent?.let { "Hard questions: $it%" }
                        ?: "Hard questions: default ($appMistakesCapPercent%)"
                )
                Slider(
                    value = (overrideMistakesCapPercent ?: appMistakesCapPercent).toFloat(),
                    onValueChange = { onSetMistakesCapOverride(it.roundToInt()) },
                    valueRange = 0f..100f,
                    enabled = overrideMistakesCapPercent != null,
                )
            }
        }
    }
}

private val pieRed = Color(0xFFBB6666)
private val pieGreen = Color(0xFF66BB66)
private val pieGray = Color(0xFFD5D5D5)

private fun days(count: Int): String = "$count ${if (count == 1) "day" else "days"}"

private fun formatAge(millis: Long): String {
    val hours = (millis / (1000 * 60 * 60)).toInt()
    if (hours < 24) return "$hours ${if (hours == 1) "hour" else "hours"}"
    return days(hours / 24)
}

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
                        dist.oldestAnsweredAt?.let {
                            Text("Oldest: ${formatAge(System.currentTimeMillis() - it)}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}