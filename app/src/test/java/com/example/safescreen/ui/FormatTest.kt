package com.example.safescreen.ui

import com.example.safescreen.ui.components.formatDuration
import com.example.safescreen.ui.components.formatLimit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun durations() {
        assertEquals("0m", formatDuration(0))
        assertEquals("<1m", formatDuration(30_000))
        assertEquals("12m", formatDuration(12 * 60_000L))
        assertEquals("1h 05m", formatDuration(65 * 60_000L))
        assertEquals("2h 00m", formatDuration(120 * 60_000L))
    }

    @Test fun limits() {
        assertEquals("No limit", formatLimit(0))
        assertEquals("15m", formatLimit(15))
        assertEquals("1h", formatLimit(60))
        assertEquals("1h 30m", formatLimit(90))
    }
}
