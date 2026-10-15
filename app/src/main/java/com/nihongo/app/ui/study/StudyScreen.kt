package com.nihongo.app.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.data.database.entity.BreakdownData
import com.nihongo.app.data.database.entity.RelatedData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: StudyViewModel = viewModel()) {
    val isConfiguring by viewModel.isConfiguring.collectAsState()
    val sessionComplete by viewModel.sessionComplete.collectAsState()
    val deck by viewModel.deck.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isRevealed by viewModel.isRevealed.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedOrdering by viewModel.selectedOrdering.collectAsState()

    if (isConfiguring) {
        StudyConfigView(
            selectedLevel = selectedLevel,
            selectedOrdering = selectedOrdering,
            onLevelSelected = { viewModel.setLevel(it) },
            onOrderingSelected = { viewModel.setOrdering(it) },
            onStartClicked = { viewModel.startSession() }
        )
    } else if (sessionComplete || deck.isEmpty()) {
        SessionCompleteView(
            emptyDeck = deck.isEmpty(),
            onRestartClicked = { viewModel.resetSessionConfig() }
        )
    } else {
        val currentCard = deck.getOrNull(currentIndex)
        if (currentCard != null) {
            FlashcardView(
                card = currentCard,
                currentIndex = currentIndex,
                totalCards = deck.size,
                isRevealed = isRevealed,
                onToggleReveal = { viewModel.toggleReveal() },
                onPronounce = { viewModel.pronounceWord(currentCard.item.vocabulary.japanese) },
                onPrevious = { viewModel.previousCard() },
                onKnow = { viewModel.answerCard(Answer.GOOD) },
                onMeh = { viewModel.answerCard(Answer.HARD) },
                onDontKnow = { viewModel.answerCard(Answer.AGAIN) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyConfigView(
    selectedLevel: String,
    selectedOrdering: DeckOrdering,
    onLevelSelected: (String) -> Unit,
    onOrderingSelected: (DeckOrdering) -> Unit,
    onStartClicked: () -> Unit
) {
    var orderingExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Flashcard Study Session", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Select JLPT Level", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("N5", "N4", "N3").forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Card Ordering Strategy", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = orderingExpanded,
            onExpandedChange = { orderingExpanded = !orderingExpanded }
        ) {
            OutlinedTextField(
                value = selectedOrdering.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderingExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = orderingExpanded,
                onDismissRequest = { orderingExpanded = false }
            ) {
                DeckOrdering.values().forEach { ordering ->
                    DropdownMenuItem(
                        text = { Text(ordering.displayName) },
                        onClick = {
                            onOrderingSelected(ordering)
                            orderingExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStartClicked,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Start Flashcards", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardView(
    card: StudyCard,
    currentIndex: Int,
    totalCards: Int,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onPronounce: () -> Unit,
    onPrevious: () -> Unit,
    onKnow: () -> Unit,
    onMeh: () -> Unit,
    onDontKnow: () -> Unit
) {
    val vocab = card.item.vocabulary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Card ${currentIndex + 1} of $totalCards", style = MaterialTheme.typography.bodyMedium)
            Badge { Text(vocab.jlptLevel) }
        }

        Card(
            onClick = onToggleReveal,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    IconButton(onClick = onPronounce) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Pronounce Word", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = vocab.japanese,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )

                    if (isRevealed) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "${vocab.reading} (${vocab.romaji})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = vocab.meaning,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type: ${vocab.type}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )

                        val breakdown = BreakdownData.fromJson(vocab.breakdownData)
                        if (breakdown != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (breakdown.subKanjis.isNotEmpty()) {
                                Text(
                                    text = "Kanji Breakdown:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                breakdown.subKanjis.forEach { sub ->
                                    Text(
                                        text = "${sub.kanji} - ${sub.meaning}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (!breakdown.collectiveMeaning.isNullOrBlank()) {
                                Text(
                                    text = "Collective Meaning:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = breakdown.collectiveMeaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        val related = RelatedData.fromJson(vocab.relatedData)
                        if (related != null && related.relatedWords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Related Vocabulary & Examples:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            related.relatedWords.take(5).forEach { rel ->
                                Text(
                                    text = "• ${rel.word} (${rel.reading}): ${rel.meaning}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                if (!rel.breakdown.isNullOrBlank()) {
                                    Text(
                                        text = "  [${rel.breakdown}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Tap anywhere on card to reveal answer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentIndex > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("Last")
                }
            }
            Button(
                onClick = onDontKnow,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Again")
            }
            Button(
                onClick = onMeh,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Meh")
            }
            Button(
                onClick = onKnow,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Know It")
            }
        }
    }
}

@Composable
fun SessionCompleteView(emptyDeck: Boolean, onRestartClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (emptyDeck) {
            Text("No vocabulary found for this level.", style = MaterialTheme.typography.titleLarge)
        } else {
            Text("🎉 Study Session Complete!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Great job reviewing your vocabulary cards.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRestartClicked,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Back to Study Setup")
        }
    }
}
