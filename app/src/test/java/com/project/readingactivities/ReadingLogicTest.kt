package com.project.readingactivities

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingLogicTest {

    @Test
    fun testBookProgressCalculation() {
        val book = Book(
            title = "Test Book",
            author = "Author",
            totalPages = 200,
            readPages = 50
        )
        // 50 / 200 = 0.25
        assertEquals(0.25f, book.progress, 0.001f)
    }

    @Test
    fun testBookProgressFull() {
        val book = Book(
            title = "Finished Book",
            author = "Author",
            totalPages = 100,
            readPages = 200 // Should be capped by logic, but model just stores it
        )
        assertEquals(2.0f, book.progress, 0.001f)
    }

    @Test
    fun testEmptyBookProgress() {
        val book = Book(
            title = "Empty",
            author = "Author",
            totalPages = 0,
            readPages = 0
        )
        assertEquals(0f, book.progress, 0.001f)
    }
}
