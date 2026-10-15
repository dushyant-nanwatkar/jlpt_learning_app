package com.nihongo.app.data.database.dao

import androidx.room.*
import com.nihongo.app.data.database.entity.GrammarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrammarList(grammarList: List<GrammarEntity>)

    @Query("SELECT * FROM grammar ORDER BY grammarId ASC")
    fun getAllGrammar(): Flow<List<GrammarEntity>>

    @Query("SELECT * FROM grammar WHERE jlptLevel = :level ORDER BY grammarId ASC")
    fun getGrammarByLevel(level: String): Flow<List<GrammarEntity>>

    @Query("SELECT * FROM grammar WHERE id = :id")
    suspend fun getGrammarById(id: Long): GrammarEntity?

    @Query("SELECT COUNT(*) FROM grammar")
    suspend fun getGrammarCount(): Int

    @Query("UPDATE grammar SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, isBookmarked: Boolean)

    @Query("DELETE FROM grammar")
    suspend fun deleteAllGrammar()
}
