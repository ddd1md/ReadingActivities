package com.project.readingactivities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).readingDao()

    val books: StateFlow<List<Book>> = dao.getAllBooks().map { entities ->
        entities.map { it.toDomain() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val goals: StateFlow<List<Goal>> = dao.getAllGoals().map { entities ->
        entities.map { it.toDomain() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notes: StateFlow<List<Note>> = dao.getAllNotes().map { entities ->
        entities.map { it.toDomain() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weeklyStats: StateFlow<List<DailyStat>> = dao.getAllStats().map { entities ->
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        days.map { day ->
            DailyStat(day, entities.find { it.day == day }?.pages ?: 0)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, List(7) { DailyStat("", 0) })

    val appRating: StateFlow<AppRatingEntity?> = dao.getAppRating()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun addBook(title: String, author: String, totalPages: Int) {
        viewModelScope.launch {
            dao.insertBook(BookEntity(java.util.UUID.randomUUID().toString(), title, author, totalPages, 0, false, null, null, null))
        }
    }

    fun updateReadPages(bookId: String, readPages: Int) {
        viewModelScope.launch {
            val book = books.value.find { it.id == bookId } ?: return@launch
            val diff = readPages - book.readPages
            if (diff > 0) updateDailyProgress(diff)
            dao.updateBook(book.copy(readPages = readPages.coerceIn(0, book.totalPages)).toEntity())
        }
    }

    private suspend fun updateDailyProgress(addedPages: Int) {
        val today = "Fri" // Fixed for demo
        val currentPages = weeklyStats.value.find { it.day == today }?.pages ?: 0
        dao.insertStat(DailyStatEntity(today, currentPages + addedPages))
    }

    fun finishBook(bookId: String, rating: Int, review: String) {
        viewModelScope.launch {
            val book = books.value.find { it.id == bookId } ?: return@launch
            dao.updateBook(book.copy(isFinished = true, rating = rating, review = review, finishedDate = Date().time, readPages = book.totalPages).toEntity())
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch { dao.deleteBook(bookId) }
    }

    fun addGoal(description: String) {
        viewModelScope.launch { 
            dao.insertGoal(GoalEntity(java.util.UUID.randomUUID().toString(), description, false, null)) 
        }
    }

    fun toggleGoal(goalId: String) {
        viewModelScope.launch {
            val goal = goals.value.find { it.id == goalId } ?: return@launch
            dao.updateGoal(goal.copy(isCompleted = !goal.isCompleted, completionDate = if (!goal.isCompleted) Date().time else null).toEntity())
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch { dao.deleteGoal(goalId) }
    }

    fun addNote(bookId: String, content: String) {
        viewModelScope.launch {
            dao.insertNote(NoteEntity(java.util.UUID.randomUUID().toString(), bookId, content, System.currentTimeMillis()))
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { dao.deleteNote(noteId) }
    }

    fun saveAppRating(rating: Int, feedback: String) {
        viewModelScope.launch {
            dao.insertAppRating(AppRatingEntity(rating = rating, feedback = feedback, date = Date().time))
        }
    }

    val totalPagesRead: Int
        get() = books.value.sumOf { it.readPages }

    val readingStreak: Int
        get() {
            var streak = 0
            for (i in 4 downTo 0) {
                if (weeklyStats.value[i].pages > 0) streak++ else break
            }
            return streak
        }
}

// Extension helpers
fun BookEntity.toDomain() = Book(id, title, author, totalPages, readPages, isFinished, rating, review, finishedDate)
fun Book.toEntity() = BookEntity(id, title, author, totalPages, readPages, isFinished, rating, review, finishedDate)
fun GoalEntity.toDomain() = Goal(id, description, isCompleted, completionDate)
fun Goal.toEntity() = GoalEntity(id, description, isCompleted, completionDate)
fun NoteEntity.toDomain() = Note(id, bookId, content, date)
fun Note.toEntity() = NoteEntity(id, bookId, content, date)
