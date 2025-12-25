package com.project.readingactivities

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

class ReadingViewModel : ViewModel() {
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _weeklyStats = MutableStateFlow(listOf(
        DailyStat("Mon", 0),
        DailyStat("Tue", 0),
        DailyStat("Wed", 0),
        DailyStat("Thu", 0),
        DailyStat("Fri", 0),
        DailyStat("Sat", 0),
        DailyStat("Sun", 0)
    ))
    val weeklyStats: StateFlow<List<DailyStat>> = _weeklyStats.asStateFlow()

    fun addBook(title: String, author: String, totalPages: Int) {
        _books.update { it + Book(title = title, author = author, totalPages = totalPages) }
    }

    fun updateReadPages(bookId: String, readPages: Int) {
        _books.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    val diff = readPages - book.readPages
                    if (diff > 0) updateDailyProgress(diff)
                    book.copy(readPages = readPages.coerceIn(0, book.totalPages))
                } else book
            }
        }
    }

    private fun updateDailyProgress(addedPages: Int) {
        _weeklyStats.update { stats ->
            stats.mapIndexed { index, stat ->
                // Simplified: assuming index 4 (Fri) is today for the demo consistency
                if (index == 4) stat.copy(pages = stat.pages + addedPages) else stat
            }
        }
    }

    fun finishBook(bookId: String, rating: Int, review: String) {
        _books.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    book.copy(
                        isFinished = true,
                        rating = rating,
                        review = review,
                        finishedDate = Date().time,
                        readPages = book.totalPages
                    )
                } else book
            }
        }
    }

    fun deleteBook(bookId: String) {
        _books.update { it.filter { it.id != bookId } }
    }

    fun addGoal(description: String) {
        _goals.update { it + Goal(description = description) }
    }

    fun toggleGoal(goalId: String) {
        _goals.update { currentGoals ->
            currentGoals.map { goal ->
                if (goal.id == goalId) {
                    goal.copy(
                        isCompleted = !goal.isCompleted,
                        completionDate = if (!goal.isCompleted) Date().time else null
                    )
                } else goal
            }
        }
    }

    fun deleteGoal(goalId: String) {
        _goals.update { it.filter { it.id != goalId } }
    }
    
    val totalPagesRead: Int
        get() = _books.value.sumOf { it.readPages }

    val readingStreak: Int
        get() {
            var streak = 0
            // Counting backwards from Friday (index 4) as our "today"
            for (i in 4 downTo 0) {
                if (_weeklyStats.value[i].pages > 0) {
                    streak++
                } else {
                    break
                }
            }
            return streak
        }
}
