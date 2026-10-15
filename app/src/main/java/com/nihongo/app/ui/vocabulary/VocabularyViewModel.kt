package com.nihongo.app.ui.vocabulary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.data.database.NihongoDatabase
import com.nihongo.app.data.database.entity.LearningStateEntity
import com.nihongo.app.data.database.entity.VocabularyEntity
import com.nihongo.app.data.database.entity.VocabularyWithState
import com.nihongo.app.data.importer.CsvImporter
import com.nihongo.app.data.importer.XlsxImporter
import com.nihongo.app.data.repository.VocabularyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NihongoDatabase.getDatabase(application)
    private val repository = VocabularyRepository(
        database.vocabularyDao(),
        database.learningStateDao(),
        XlsxImporter(application)
    )
    private val csvImporter = CsvImporter(application)

    private val _selectedLevel = MutableStateFlow<String?>("N5")
    val selectedLevel: StateFlow<String?> = _selectedLevel

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val vocabularyList: StateFlow<List<VocabularyWithState>> = combine(
        _selectedLevel,
        _searchQuery
    ) { level, query ->
        Pair(level, query)
    }.flatMapLatest { (level, query) ->
        val flow = if (level != null) {
            repository.getVocabularyByLevel(level)
        } else {
            repository.allVocabularyWithState
        }
        flow.map { list ->
            if (query.isBlank()) {
                list
            } else {
                val lowerQuery = query.lowercase()
                list.filter {
                    it.vocabulary.japanese.lowercase().contains(lowerQuery) ||
                    it.vocabulary.reading.lowercase().contains(lowerQuery) ||
                    it.vocabulary.romaji.lowercase().contains(lowerQuery) ||
                    it.vocabulary.meaning.lowercase().contains(lowerQuery)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            loadMasterCsvData()
        }
    }

    fun selectLevel(level: String?) {
        _selectedLevel.value = level
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun importXlsx(inputStream: InputStream) {
        viewModelScope.launch {
            repository.importFromStream(inputStream)
        }
    }

    private suspend fun loadMasterCsvData() = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val assetManager = app.assets
        val levels = listOf(
            Triple("N5", "jlpt_n5_breakdown.csv", "jlpt_n5_related_vocab.csv"),
            Triple("N4", "jlpt_n4_breakdown.csv", "jlpt_n4_related_vocab.csv"),
            Triple("N3", "jlpt_n3_breakdown.csv", "jlpt_n3_related_vocab.csv")
        )

        val existingVocabList = database.vocabularyDao().getAllVocabularySync().toMutableList()
        val existingStatesSet = database.learningStateDao().getAllLearningStatesSync().map { it.vocabularyId }.toSet()

        val vocabMap = mutableMapOf<String, VocabularyEntity>()
        existingVocabList.forEach { vocab ->
            vocabMap["${vocab.jlptLevel}_${vocab.japanese}"] = vocab
        }

        for ((level, breakdownFile, relatedFile) in levels) {
            try {
                val relatedMap = try {
                    val relStream = assetManager.open(relatedFile)
                    csvImporter.importRelatedWordsMapFromStream(relStream)
                } catch (e: Exception) {
                    emptyMap()
                }

                val inputStream = assetManager.open(breakdownFile)
                val csvVocabList = csvImporter.importCsvFromStream(inputStream, level)
                val csvJapaneseSet = csvVocabList.map { it.japanese }.toSet()

                // Identify orphans for this level
                val orphanIds = existingVocabList
                    .filter { it.jlptLevel.equals(level, ignoreCase = true) && !csvJapaneseSet.contains(it.japanese) }
                    .map { it.id }

                if (orphanIds.isNotEmpty()) {
                    database.vocabularyDao().deleteVocabularyByIds(orphanIds)
                    existingVocabList.removeAll { orphanIds.contains(it.id) }
                    val itMap = vocabMap.entries.iterator()
                    while (itMap.hasNext()) {
                        if (orphanIds.contains(itMap.next().value.id)) {
                            itMap.remove()
                        }
                    }
                }

                val toUpdate = mutableListOf<VocabularyEntity>()
                val toInsert = mutableListOf<VocabularyEntity>()

                for (csvVocab in csvVocabList) {
                    val key = "${level}_${csvVocab.japanese}"
                    val match = vocabMap[key] ?: existingVocabList.find { it.japanese == csvVocab.japanese }
                    val relatedJson = relatedMap[csvVocab.japanese] ?: relatedMap[csvVocab.reading]

                    if (match != null) {
                        val updated = match.copy(
                            reading = csvVocab.reading,
                            romaji = csvVocab.romaji,
                            type = csvVocab.type,
                            meaning = csvVocab.meaning,
                            jlptLevel = level,
                            breakdownData = csvVocab.breakdownData,
                            relatedData = relatedJson ?: match.relatedData
                        )
                        if (updated != match) {
                            toUpdate.add(updated)
                            vocabMap[key] = updated
                        }
                    } else {
                        val newVocab = csvVocab.copy(relatedData = relatedJson)
                        toInsert.add(newVocab)
                    }
                }

                if (toUpdate.isNotEmpty()) {
                    database.vocabularyDao().updateVocabularyList(toUpdate)
                }

                if (toInsert.isNotEmpty()) {
                    val newStates = mutableListOf<LearningStateEntity>()
                    for (newItem in toInsert) {
                        val newId = database.vocabularyDao().insertVocabulary(newItem)
                        val insertedEntity = newItem.copy(id = newId)
                        vocabMap["${level}_${newItem.japanese}"] = insertedEntity
                        existingVocabList.add(insertedEntity)
                        if (!existingStatesSet.contains(newId)) {
                            newStates.add(LearningStateEntity(vocabularyId = newId))
                        }
                    }
                    if (newStates.isNotEmpty()) {
                        database.learningStateDao().insertLearningStateList(newStates)
                    }
                }
            } catch (e: Exception) {
                // Ignore missing file errors
            }
        }
    }
}
