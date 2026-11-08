package io.github.romanfabio.meditate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    /** Per-day totals for the inclusive ISO date range [fromDate, toDate]. */
    @Query(
        """
        SELECT localDate AS localDate,
               SUM(durationSeconds) AS totalSeconds,
               COUNT(*) AS sessionCount
        FROM sessions
        WHERE localDate BETWEEN :fromDate AND :toDate
        GROUP BY localDate
        """,
    )
    fun observeDayTotals(fromDate: String, toDate: String): Flow<List<DayTotal>>

    @Query("SELECT * FROM sessions WHERE localDate = :date ORDER BY startedAtEpochMs ASC")
    fun observeSessionsOn(date: String): Flow<List<SessionEntity>>
}
