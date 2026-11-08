package io.github.romanfabio.meditate.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {
    private var now = 1_000L
    private val engine = SessionEngine { now }

    private fun advance(ms: Long) {
        now += ms
    }

    @Test
    fun countdownThenBegin() {
        engine.start(60)
        assertEquals(SessionState.Countdown(5), engine.snapshot())
        advance(1_000)
        assertEquals(SessionState.Countdown(4), engine.snapshot())
        advance(3_999)
        assertEquals(SessionState.Countdown(1), engine.snapshot())
        assertTrue(engine.tick().isEmpty())
        advance(1)
        assertEquals(listOf(EngineEvent.Began), engine.tick())
        val s = engine.snapshot() as SessionState.Running
        assertEquals(60, s.remainingSeconds)
    }

    @Test
    fun cancelDuringCountdownRecordsNothing() {
        engine.start(60)
        advance(2_000)
        assertEquals(listOf(EngineEvent.Cancelled), engine.stop())
        assertEquals(SessionState.Idle, engine.snapshot())
        assertFalse(engine.isActive)
    }

    @Test
    fun completesAtPlannedTime() {
        engine.start(10)
        advance(5_000)
        engine.tick()
        advance(9_999)
        assertTrue(engine.tick().isEmpty())
        advance(1)
        assertEquals(listOf(EngineEvent.Completed(10, 10)), engine.tick())
        assertEquals(SessionState.Finished(10, 10, completed = true), engine.snapshot())
    }

    @Test
    fun lateTickDoesNotDrift() {
        engine.start(10)
        // First tick comes very late: countdown and whole session are over.
        advance(60_000)
        val events = engine.tick()
        assertEquals(listOf(EngineEvent.Began, EngineEvent.Completed(10, 10)), events)
    }

    @Test
    fun pauseExcludedFromMeditatedTime() {
        engine.start(100)
        advance(5_000)
        engine.tick()
        advance(20_000)
        engine.pause()
        val paused = engine.snapshot() as SessionState.Paused
        assertEquals(80, paused.remainingSeconds)
        advance(600_000) // a long pause changes nothing
        assertEquals(paused, engine.snapshot())
        engine.resume()
        advance(10_000)
        val events = engine.stop()
        assertEquals(listOf(EngineEvent.Stopped(30, 100)), events)
        assertEquals(SessionState.Finished(30, 100, completed = false), engine.snapshot())
    }

    @Test
    fun stopWhilePaused() {
        engine.start(100)
        advance(5_000)
        engine.tick()
        advance(12_400)
        engine.pause()
        assertEquals(listOf(EngineEvent.Stopped(12, 100)), engine.stop())
    }

    @Test
    fun pausedSessionNeverCompletesByItself() {
        engine.start(10)
        advance(5_000)
        engine.tick()
        advance(4_000)
        engine.pause()
        advance(3_600_000)
        assertTrue(engine.tick().isEmpty())
        assertTrue(engine.isActive)
    }

    @Test
    fun resetReturnsToIdleAndAllowsNewSession() {
        engine.start(5)
        advance(10_000)
        engine.tick()
        engine.reset()
        assertEquals(SessionState.Idle, engine.snapshot())
        engine.start(5)
        assertEquals(SessionState.Countdown(5), engine.snapshot())
    }

    @Test
    fun pauseAndResumeAreIgnoredInWrongPhases() {
        engine.pause()
        engine.resume()
        assertEquals(SessionState.Idle, engine.snapshot())
        engine.start(10)
        engine.pause() // still counting down
        assertEquals(SessionState.Countdown(5), engine.snapshot())
    }

    @Test
    fun progressGrows() {
        engine.start(100)
        advance(5_000)
        engine.tick()
        advance(25_000)
        val s = engine.snapshot() as SessionState.Running
        assertEquals(0.25f, s.progress, 0.001f)
        assertEquals(75, s.remainingSeconds)
    }
}
