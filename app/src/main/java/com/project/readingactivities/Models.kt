package com.project.readingactivities

import java.util.UUID

data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val totalPages: Int,
    val readPages: Int = 0
) {
    val progress: Float
        get() = if (totalPages > 0) readPages.toFloat() / totalPages else 0f
}

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val isCompleted: Boolean = false
)
