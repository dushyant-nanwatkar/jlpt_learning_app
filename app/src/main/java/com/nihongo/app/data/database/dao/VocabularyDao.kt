package com.nihongo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nihongo.app.data.database.entity.VocabularyEntity
import com.nihongo.app.data.database.entity.VocabularyWithState
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(vocabulary: VocabularyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabularyList(vocabularyList: List<VocabularyEntity>)

    @Query("SELECT * FROM vocabulary")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE jlptLevel = :level")
    fun getVocabularyByLevel(level: String): Flow<List<VocabularyEntity>>

    @Transaction
    @Query("SELECT * FROM vocabulary")
    fun getAllVocabularyWithState(): Flow<List<VocabularyWithState>>

    @Transaction
    @Query("SELECT * FROM vocabulary WHERE jlptLevel = :level")
    fun getVocabularyWithStateByLevel(level: String): Flow<List<VocabularyWithState>>

    @Query("DELETE FROM vocabulary")
    suspend fun deleteAllVocabulary()

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getVocabularyCount(): Int

    @Query("SELECT * FROM vocabulary")
    suspend fun getAllVocabularySync(): List<VocabularyEntity>

    @Query("UPDATE vocabulary SET breakdownData = :breakdownData WHERE id = :id")
    suspend fun updateBreakdownData(id: Long, breakdownData: String?)

    @Update
    suspend fun updateVocabulary(vocabulary: VocabularyEntity)

    @Update
    suspend fun updateVocabularyList(vocabularyList: List<VocabularyEntity>)

    @Query("DELETE FROM vocabulary WHERE id IN (:ids)")
    suspend fun deleteVocabularyByIds(ids: List<Long>)
}
