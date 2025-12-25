package com.project.readingactivities

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val totalPages: Int,
    val readPages: Int,
    val isFinished: Boolean,
    val rating: Int?,
    val review: String?,
    val finishedDate: Long?,
    val isWishlist: Boolean = false
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val description: String,
    val isCompleted: Boolean,
    val completionDate: Long?
)

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val day: String,
    val pages: Int
)

@Entity(tableName = "app_rating")
data class AppRatingEntity(
    @PrimaryKey val id: Int = 1,
    val rating: Int,
    val feedback: String,
    val date: Long
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val content: String,
    val date: Long
)

@Entity(tableName = "yearly_challenge")
data class YearlyChallengeEntity(
    @PrimaryKey val year: Int,
    val goal: Int
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeId: Int = 0 // 0: Default, 1: Ocean, 2: Sunset, 3: Lavender
)

@Dao
interface ReadingDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: String)

    @Query("SELECT * FROM daily_stats")
    fun getAllStats(): Flow<List<DailyStatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStat(stat: DailyStatEntity)

    @Query("SELECT * FROM app_rating WHERE id = 1")
    fun getAppRating(): Flow<AppRatingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppRating(rating: AppRatingEntity)

    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("SELECT * FROM yearly_challenge WHERE year = :year")
    fun getYearlyChallenge(year: Int): Flow<YearlyChallengeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYearlyChallenge(challenge: YearlyChallengeEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettingsEntity)
}

@Database(entities = [BookEntity::class, GoalEntity::class, DailyStatEntity::class, AppRatingEntity::class, NoteEntity::class, YearlyChallengeEntity::class, AppSettingsEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reading_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
