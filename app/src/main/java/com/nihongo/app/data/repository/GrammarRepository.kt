package com.nihongo.app.data.repository

import com.nihongo.app.data.database.dao.GrammarDao
import com.nihongo.app.data.database.entity.GrammarEntity
import kotlinx.coroutines.flow.Flow

class GrammarRepository(
    private val grammarDao: GrammarDao
) {
    val allGrammar: Flow<List<GrammarEntity>> = grammarDao.getAllGrammar()

    fun getGrammarByLevel(level: String): Flow<List<GrammarEntity>> {
        return grammarDao.getGrammarByLevel(level)
    }

    suspend fun toggleBookmark(id: Long, current: Boolean) {
        grammarDao.updateBookmark(id, !current)
    }

    suspend fun getGrammarCount(): Int {
        return grammarDao.getGrammarCount()
    }

    suspend fun insertGrammarList(list: List<GrammarEntity>) {
        grammarDao.insertGrammarList(list)
    }
}
