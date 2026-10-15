package com.nihongo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grammar")
data class GrammarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grammarId: Int,
    val jlptLevel: String,
    val grammar: String,
    val reading: String,
    val meaning: String,
    val notes: String? = null,
    val source: String? = null,
    val examplesJson: String = "[]",
    val isBookmarked: Boolean = false
)

data class GrammarExample(
    val japanese: String,
    val english: String
)
