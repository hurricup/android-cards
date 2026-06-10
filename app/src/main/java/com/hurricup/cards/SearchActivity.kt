package com.hurricup.cards

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hurricup.cards.model.Question
import com.hurricup.cards.model.Questionary
import com.hurricup.cards.ui.theme.AndroidCardsTheme

private val PUNCT_REGEX = "\\p{P}+".toRegex()

internal fun normalize(s: String): String =
    s.lowercase().replace(PUNCT_REGEX, "")

private data class Match(val question: Question, val matchIndex: Int, val leftover: Int)

internal fun search(query: String, questions: List<Question>): List<Question> {
    if (query.isBlank()) return emptyList()
    val normalizedQuery = normalize(query)
    if (normalizedQuery.isEmpty()) return emptyList()
    return questions
        .mapNotNull { q ->
            val normalized = normalize(q.text)
            val idx = normalized.indexOf(normalizedQuery)
            if (idx >= 0) Match(q, idx, normalized.length - normalizedQuery.length) else null
        }
        .sortedWith(compareBy({ it.matchIndex }, { it.leftover }, { it.question.text }))
        .map { it.question }
}

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val allQuestions = Questionary.cache.values.flatMap { it.questions }
        enableEdgeToEdge()
        setContent {
            AndroidCardsTheme {
                SearchScreen(allQuestions)
            }
        }
    }
}

@Composable
private fun SearchScreen(allQuestions: List<Question>) {
    var query by remember { mutableStateOf("") }

    val results = remember(query) { search(query, allQuestions) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search...") },
                singleLine = true
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { question ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = question.text,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 3
                        )
                        Text(
                            text = question.answer ?: "",
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 3
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
