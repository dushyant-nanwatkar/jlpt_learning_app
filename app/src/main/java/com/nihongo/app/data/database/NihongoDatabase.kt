package com.nihongo.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nihongo.app.data.database.dao.GrammarDao
import com.nihongo.app.data.database.dao.LearningStateDao
import com.nihongo.app.data.database.dao.TestDao
import com.nihongo.app.data.database.dao.VocabularyDao
import com.nihongo.app.data.database.entity.GrammarEntity
import com.nihongo.app.data.database.entity.LearningStateEntity
import com.nihongo.app.data.database.entity.QuestionOptionEntity
import com.nihongo.app.data.database.entity.TestEntity
import com.nihongo.app.data.database.entity.TestQuestionEntity
import com.nihongo.app.data.database.entity.VocabularyEntity

@Database(
    entities = [
        VocabularyEntity::class,
        LearningStateEntity::class,
        TestEntity::class,
        TestQuestionEntity::class,
        QuestionOptionEntity::class,
        GrammarEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class NihongoDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun learningStateDao(): LearningStateDao
    abstract fun testDao(): TestDao
    abstract fun grammarDao(): GrammarDao

    companion object {
        @Volatile
        private var INSTANCE: NihongoDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN breakdownData TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE learning_state ADD COLUMN intervalDays REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE vocabulary SET breakdownData = NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN relatedData TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE vocabulary SET breakdownData = NULL")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE vocabulary SET breakdownData = NULL")
            }
        }

        fun getDatabase(context: Context): NihongoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NihongoDatabase::class.java,
                    "nihongo_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
