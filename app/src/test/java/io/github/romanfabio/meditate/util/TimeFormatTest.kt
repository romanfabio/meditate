package io.github.romanfabio.meditate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun clock() {
        assertEquals("00:20:00", formatClock(1200))
        assertEquals("00:00:05", formatClock(5))
        assertEquals("01:01:01", formatClock(3661))
        assertEquals("23:59:59", formatClock(86399))
        assertEquals("00:00:00", formatClock(-3))
    }

    @Test
    fun duration() {
        assertEquals("45s", formatDuration(45))
        assertEquals("25m", formatDuration(1500))
        assertEquals("25m 30s", formatDuration(1530))
        assertEquals("1h 05m", formatDuration(3900))
        assertEquals("2h", formatDuration(7200))
    }

    @Test
    fun compact() {
        assertEquals("<1m", formatCompact(12))
        assertEquals("1m", formatCompact(45))
        assertEquals("25m", formatCompact(1500))
        assertEquals("1h05", formatCompact(3900))
    }
}
