package io.github.romanfabio.meditate.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {

    /**
     * Saves a session unless it is shorter than [MIN_RECORD_SECONDS].
     * @return true if the session was stored.
     */
    suspend fun record(
        startedAtEpochMs: Long,
        durationSeconds: Int,
        plannedSeconds: Int,
        completed: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (durationSeconds < MIN_RECORD_SECONDS) return false
        val date = Instant.ofEpochMilli(startedAtEpochMs).atZone(zone).toLocalDate()
        dao.insert(
            SessionEntity(
                startedAtEpochMs = startedAtEpochMs,
                localDate = date.toString(),
                durationSeconds = durationSeconds,
                plannedSeconds = plannedSeconds,
                completed = completed,
            ),
        )
        return true
    }

    /** Totals for every day of [month] that has at least one session, keyed by date. */
    fun observeMonth(month: YearMonth): Flow<List<DayTotal>> =
        dao.observeDayTotals(month.atDay(1).toString(), month.atEndOfMonth().toString())

    fun observeDay(date: LocalDate): Flow<List<SessionEntity>> = dao.observeSessionsOn(date.toString())

    companion object {
        const val MIN_RECORD_SECONDS = 10
    }
}
