package com.project.readingactivities

import java.util.UUID

data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val totalPages: Int,
    val readPages: Int = 0,
    val isFinished: Boolean = false,
    val rating: Int? = null,
    val review: String? = null,
    val finishedDate: Long? = null,
    val isWishlist: Boolean = false
) {
    val progress: Float
        get() = if (totalPages > 0) readPages.toFloat() / totalPages else 0f
}

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val isCompleted: Boolean = false,
    val completionDate: Long? = null
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val content: String,
    val date: Long = System.currentTimeMillis()
)

data class DailyStat(val day: String, val pages: Int)

data class YearlyChallenge(
    val year: Int,
    val goal: Int
)
