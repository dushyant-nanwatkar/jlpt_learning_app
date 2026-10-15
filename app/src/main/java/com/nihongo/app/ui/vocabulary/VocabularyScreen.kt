package com.nihongo.app.ui.vocabulary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nihongo.app.data.database.entity.BreakdownData
import com.nihongo.app.data.database.entity.RelatedData
import com.nihongo.app.data.database.entity.VocabularyWithState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(viewModel: VocabularyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val vocabularyItems by viewModel.vocabularyList.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { inputStream ->
                viewModel.importXlsx(inputStream)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"))
            }) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import XLSX")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search Vocabulary") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null, "N5", "N4", "N3").forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { viewModel.selectLevel(level) },
                        label = { Text(level ?: "All") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (vocabularyItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No vocabulary found. Tap the upload button to import an XLSX file.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vocabularyItems) { item ->
                        VocabularyCard(item)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyCard(item: VocabularyWithState) {
    val vocab = item.vocabulary
    val state = item.learningState
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = vocab.japanese,
                    style = MaterialTheme.typography.titleLarge
                )
                Badge {
                    Text(vocab.jlptLevel)
                }
            }
            Text(
                text = "${vocab.reading} (${vocab.romaji})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${vocab.type.replaceFirstChar { it.uppercase() }}: ${vocab.meaning}",
                style = MaterialTheme.typography.bodyLarge
            )

            if (expanded) {
                val breakdown = BreakdownData.fromJson(vocab.breakdownData)
                if (breakdown != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    if (breakdown.subKanjis.isNotEmpty()) {
                        Text(
                            text = "Kanji Breakdown:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        breakdown.subKanjis.forEach { sub ->
                            Text(
                                text = "• ${sub.kanji} - ${sub.meaning}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (!breakdown.collectiveMeaning.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Idea: ${breakdown.collectiveMeaning}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                val related = RelatedData.fromJson(vocab.relatedData)
                if (related != null && related.relatedWords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Related Words & Examples:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    related.relatedWords.forEach { rel ->
                        Text(
                            text = "• ${rel.word} (${rel.reading}): ${rel.meaning}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!rel.breakdown.isNullOrBlank()) {
                            Text(
                                text = "   [${rel.breakdown}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                if (state != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Learning State:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mastery: ${(state.masteryScore * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Text("Streak: ${state.streak}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${state.learningState}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Interval: ${String.format("%.1f", state.intervalDays)} days", style = MaterialTheme.typography.bodySmall)
                        Text("Seen: ${state.timesSeen} times", style = MaterialTheme.typography.bodySmall)
                        Text("Diff: ${String.format("%.1f", state.difficulty)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val breakdown = BreakdownData.fromJson(vocab.breakdownData)
                if (breakdown != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (breakdown.subKanjis.isNotEmpty()) {
                        Text(
                            text = "Breakdown: " + breakdown.subKanjis.joinToString(", ") { "${it.kanji} (${it.meaning})" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
