package io.github.romanfabio.meditate.util

/** Clock style `HH:mm:ss`, e.g. 1200 -> "00:20:00". */
fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
}

/**
 * Compact human duration: "45s", "25m", "25m 30s", "1h 05m", "2h".
 * Used for the calendar and session lists.
 */
fun formatDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = s % 3600 / 60
    val sec = s % 60
    return when {
        h > 0 && m == 0 -> "${h}h"
        h > 0 -> "%dh %02dm".format(h, m)
        m > 0 && sec == 0 -> "${m}m"
        m > 0 -> "${m}m ${sec}s"
        else -> "${sec}s"
    }
}

/** Short form for tight calendar cells: minutes rounded, "<1m", "25m", "1h05". */
fun formatCompact(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val minutes = (s + 30) / 60
    return when {
        s < 30 -> "<1m"
        minutes < 60 -> "${minutes}m"
        else -> "%dh%02d".format(minutes / 60, minutes % 60)
    }
}
