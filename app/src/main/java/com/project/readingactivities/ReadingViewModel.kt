package com.project.readingactivities

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReadingViewModel : ViewModel() {
    private val _books = MutableStateFlow(listOf(
        Book(title = "The Great Gatsby", author = "F. Scott Fitzgerald", totalPages = 180, readPages = 45),
        Book(title = "1984", author = "George Orwell", totalPages = 328, readPages = 120)
    ))
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _goals = MutableStateFlow(listOf(
        Goal(description = "Read 20 pages today"),
        Goal(description = "Finish 1984 by Friday", isCompleted = true)
    ))
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    fun addBook(title: String, author: String, totalPages: Int) {
        _books.update { it + Book(title = title, author = author, totalPages = totalPages) }
    }

    fun updateProgress(bookId: String, pages: Int) {
        _books.update { currentBooks ->
            currentBooks.map { book ->
                if (book.id == bookId) {
                    val newReadPages = (book.readPages + pages).coerceAtMost(book.totalPages)
                    book.copy(readPages = newReadPages)
                } else book
            }
        }
    }

    fun deleteBook(bookId: String) {
        _books.update { it.filter { book -> book.id != bookId } }
    }

    fun toggleGoal(goalId: String) {
        _goals.update { currentGoals ->
            currentGoals.map { goal ->
                if (goal.id == goalId) goal.copy(isCompleted = !goal.isCompleted) else goal
            }
        }
    }
    
    val totalPagesRead: Int
        get() = _books.value.sumOf { it.readPages }
}
