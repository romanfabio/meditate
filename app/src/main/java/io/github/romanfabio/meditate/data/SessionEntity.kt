package io.github.romanfabio.meditate.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One finished meditation session.
 *
 * [localDate] is the ISO date (yyyy-MM-dd) of the day the session started, stored at save time so
 * that past entries do not move if the device time zone changes later.
 */
@Entity(tableName = "sessions", indices = [Index("localDate")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val localDate: String,
    /** Actual meditated seconds, excluding pauses and the start countdown. */
    val durationSeconds: Int,
    val plannedSeconds: Int,
    /** True when the timer ran down to zero, false when the user stopped early. */
    val completed: Boolean,
)

/** Aggregated totals for a single day. */
data class DayTotal(
    val localDate: String,
    val totalSeconds: Int,
    val sessionCount: Int,
)
