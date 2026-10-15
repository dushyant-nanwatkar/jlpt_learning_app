package com.nihongo.app.ui.grammar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.data.database.NihongoDatabase
import com.nihongo.app.data.database.entity.GrammarEntity
import com.nihongo.app.data.importer.CsvImporter
import com.nihongo.app.data.repository.GrammarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GrammarStats(
    val totalCount: Int = 0,
    val n5Count: Int = 0,
    val n4Count: Int = 0,
    val n3Count: Int = 0,
    val bookmarkedCount: Int = 0
)

class GrammarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NihongoDatabase.getDatabase(application)
    private val repository = GrammarRepository(database.grammarDao())
    private val csvImporter = CsvImporter(application)

    private val _selectedLevel = MutableStateFlow<String?>("All")
    val selectedLevel: StateFlow<String?> = _selectedLevel

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _bookmarkedOnly = MutableStateFlow(false)
    val bookmarkedOnly: StateFlow<Boolean> = _bookmarkedOnly

    val allGrammarList: StateFlow<List<GrammarEntity>> = repository.allGrammar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grammarList: StateFlow<List<GrammarEntity>> = combine(
        allGrammarList,
        _selectedLevel,
        _searchQuery,
        _bookmarkedOnly
    ) { list, level, query, bookmarked ->
        var filtered = list

        if (bookmarked) {
            filtered = filtered.filter { it.isBookmarked }
        }

        if (level != null && level != "All" && level != "Bookmarked") {
            filtered = filtered.filter { it.jlptLevel.equals(level, ignoreCase = true) }
        } else if (level == "Bookmarked") {
            filtered = filtered.filter { it.isBookmarked }
        }

        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = filtered.filter {
                it.grammar.lowercase().contains(q) ||
                it.reading.lowercase().contains(q) ||
                it.meaning.lowercase().contains(q) ||
                (it.notes?.lowercase()?.contains(q) == true) ||
                it.examplesJson.lowercase().contains(q)
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<GrammarStats> = allGrammarList.map { list ->
        GrammarStats(
            totalCount = list.size,
            n5Count = list.count { it.jlptLevel.equals("N5", ignoreCase = true) },
            n4Count = list.count { it.jlptLevel.equals("N4", ignoreCase = true) },
            n3Count = list.count { it.jlptLevel.equals("N3", ignoreCase = true) },
            bookmarkedCount = list.count { it.isBookmarked }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GrammarStats())

    init {
        viewModelScope.launch {
            loadGrammarData()
        }
    }

    fun selectLevel(level: String?) {
        _selectedLevel.value = level
        if (level == "Bookmarked") {
            _bookmarkedOnly.value = true
        } else if (_bookmarkedOnly.value && level != null && level != "All") {
            _bookmarkedOnly.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmarkedOnly() {
        _bookmarkedOnly.value = !_bookmarkedOnly.value
    }

    fun toggleBookmark(grammar: GrammarEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(grammar.id, grammar.isBookmarked)
        }
    }

    private suspend fun loadGrammarData() = withContext(Dispatchers.IO) {
        try {
            val count = repository.getGrammarCount()
            if (count == 0) {
                val app = getApplication<Application>()
                val stream = app.assets.open("jlpt_grammar.csv")
                val parsed = csvImporter.importGrammarFromStream(stream)
                if (parsed.isNotEmpty()) {
                    repository.insertGrammarList(parsed)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
