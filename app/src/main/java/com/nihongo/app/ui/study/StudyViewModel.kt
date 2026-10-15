package com.nihongo.app.ui.study

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.data.database.NihongoDatabase
import com.nihongo.app.data.database.entity.LearningStateEntity
import com.nihongo.app.data.database.entity.VocabularyWithState
import com.nihongo.app.tts.TtsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DeckOrdering(val displayName: String) {
    ANKI_DAILY("Daily Study (Due + 20 New)"),
    REVIEW_DUE("All Due Reviews"),
    NEW_ONLY("New Words Only"),
    RANDOM("Random (All Words)"),
    ASCENDING("Database Ascending")
}

enum class Answer {
    AGAIN,
    HARD,
    GOOD
}

data class StudyCard(
    val item: VocabularyWithState,
    var sessionAttempts: Int = 0,
    var consecutiveKnown: Int = 0
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NihongoDatabase.getDatabase(application)
    private val vocabularyDao = database.vocabularyDao()
    private val learningStateDao = database.learningStateDao()
    private val prefs = application.getSharedPreferences("nihongo_prefs", Context.MODE_PRIVATE)
    private val ttsManager = TtsManager(application)

    private val _selectedLevel = MutableStateFlow("N5")
    val selectedLevel: StateFlow<String> = _selectedLevel

    private val _selectedOrdering = MutableStateFlow(DeckOrdering.ANKI_DAILY)
    val selectedOrdering: StateFlow<DeckOrdering> = _selectedOrdering

    private val _isConfiguring = MutableStateFlow(true)
    val isConfiguring: StateFlow<Boolean> = _isConfiguring

    private val _deck = MutableStateFlow<List<StudyCard>>(emptyList())
    val deck: StateFlow<List<StudyCard>> = _deck

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isRevealed = MutableStateFlow(false)
    val isRevealed: StateFlow<Boolean> = _isRevealed

    private val _sessionComplete = MutableStateFlow(false)
    val sessionComplete: StateFlow<Boolean> = _sessionComplete

    fun setLevel(level: String) {
        _selectedLevel.value = level
    }

    fun setOrdering(ordering: DeckOrdering) {
        _selectedOrdering.value = ordering
    }

    fun startSession() {
        viewModelScope.launch {
            val level = _selectedLevel.value
            val ordering = _selectedOrdering.value

            vocabularyDao.getVocabularyWithStateByLevel(level).first().let { rawList ->
                if (rawList.isEmpty()) {
                    _deck.value = emptyList()
                    _isConfiguring.value = false
                    return@let
                }

                val now = System.currentTimeMillis()
                
                val sortedList = when (ordering) {
                    DeckOrdering.ANKI_DAILY -> {
                        val dueCards = rawList.filter { 
                            val state = it.learningState
                            state != null && state.learningState != "NEW" && state.nextReview <= now 
                        }
                        val newCards = rawList.filter { 
                            val state = it.learningState
                            state == null || state.learningState == "NEW" 
                        }.take(20)
                        (dueCards + newCards).shuffled()
                    }
                    DeckOrdering.REVIEW_DUE -> {
                        rawList.filter { 
                            val state = it.learningState
                            state != null && state.learningState != "NEW" && state.nextReview <= now 
                        }.sortedBy { it.learningState?.nextReview ?: 0L }
                    }
                    DeckOrdering.NEW_ONLY -> {
                        rawList.filter { 
                            val state = it.learningState
                            state == null || state.learningState == "NEW" 
                        }.shuffled()
                    }
                    DeckOrdering.RANDOM -> rawList.shuffled()
                    DeckOrdering.ASCENDING -> rawList.sortedBy { it.vocabulary.id }
                }

                _deck.value = sortedList.map { StudyCard(item = it) }
                _currentIndex.value = 0
                _isRevealed.value = false
                _sessionComplete.value = false
                _isConfiguring.value = false
            }
        }
    }

    fun toggleReveal() {
        _isRevealed.value = !_isRevealed.value
    }

    fun pronounceWord(text: String) {
        val speed = prefs.getFloat("speech_rate", 1.0f)
        ttsManager.speak(text, speed)
    }

    fun previousCard() {
        if (_currentIndex.value > 0) {
            _currentIndex.value = _currentIndex.value - 1
            _isRevealed.value = false
            _sessionComplete.value = false
        }
    }

    fun answerCard(answer: Answer) {
        val currentDeck = _deck.value
        val index = _currentIndex.value
        if (index >= currentDeck.size) return

        val studyCard = currentDeck[index]
        studyCard.sessionAttempts++

        viewModelScope.launch {
            val vocabId = studyCard.item.vocabulary.id
            val currentState = studyCard.item.learningState ?: LearningStateEntity(vocabularyId = vocabId)

            var newDifficulty = currentState.difficulty
            var newStreak = currentState.streak
            var newInterval = currentState.intervalDays

            when (answer) {
                Answer.AGAIN -> {
                    newDifficulty = (newDifficulty - 0.2f).coerceAtLeast(1.3f)
                    newStreak = 0
                    newInterval = 0f 
                }
                Answer.HARD -> {
                    newDifficulty = (newDifficulty - 0.15f).coerceAtLeast(1.3f)
                    if (newStreak == 0) {
                        newInterval = 1f
                    } else {
                        newInterval *= 1.2f
                    }
                    newStreak++
                }
                Answer.GOOD -> {
                    if (newStreak == 0) {
                        newInterval = 1f
                    } else if (newStreak == 1) {
                        newInterval = 6f
                    } else {
                        newInterval *= newDifficulty
                    }
                    newStreak++
                }
            }

            val newTimesSeen = currentState.timesSeen + 1
            val nextReviewMs = System.currentTimeMillis() + (newInterval * 24 * 60 * 60 * 1000).toLong()

            val newMastery = (newInterval / 21f).coerceIn(0f, 1f)

            val newStateStr = when {
                newInterval >= 21f -> "MASTERED"
                newInterval >= 7f -> "REVIEW"
                newInterval > 0f -> "LEARNING"
                else -> "NEW"
            }

            val knownAdd = if (answer != Answer.AGAIN) 1 else 0
            val unknownAdd = if (answer == Answer.AGAIN) 1 else 0

            val updatedState = currentState.copy(
                timesSeen = newTimesSeen,
                knownCount = currentState.knownCount + knownAdd,
                unknownCount = currentState.unknownCount + unknownAdd,
                masteryScore = newMastery,
                intervalDays = newInterval,
                difficulty = newDifficulty,
                lastSeen = System.currentTimeMillis(),
                nextReview = nextReviewMs,
                streak = newStreak,
                learningState = newStateStr
            )

            learningStateDao.updateLearningState(updatedState)

            val mutableDeck = currentDeck.toMutableList()
            if (answer == Answer.AGAIN && mutableDeck.size > 1) {
                mutableDeck.removeAt(index)
                val remaining = mutableDeck.size - index
                val offset = 10.coerceAtMost(remaining.coerceAtLeast(1))
                val reinsertIndex = (index + offset).coerceAtMost(mutableDeck.size)
                mutableDeck.add(reinsertIndex, studyCard)
                _deck.value = mutableDeck
            } else {
                if (index + 1 >= mutableDeck.size) {
                    _sessionComplete.value = true
                } else {
                    _currentIndex.value = index + 1
                    _isRevealed.value = false
                }
            }
        }
    }

    fun resetSessionConfig() {
        _isConfiguring.value = true
        _sessionComplete.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
