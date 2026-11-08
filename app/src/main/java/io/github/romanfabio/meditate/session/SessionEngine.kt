package io.github.romanfabio.meditate.session

/**
 * Pure state machine for one meditation session. It has no Android dependencies and takes its
 * time from [clock] (milliseconds, monotonic), so it is fully unit-testable.
 *
 * The engine is passive: the owner calls [tick] regularly and gets back the transitions that
 * happened. All time is derived from clock differences, never from counting ticks, so late ticks
 * (doze, busy main thread) do not make the timer drift.
 */
class SessionEngine(private val clock: () -> Long) {

    private enum class Phase { IDLE, COUNTDOWN, RUNNING, PAUSED, FINISHED }

    private var phase = Phase.IDLE
    private var plannedMs = 0L
    private var countdownEndsAt = 0L

    /** Meditation time accumulated in finished run segments. */
    private var accumulatedMs = 0L
    private var segmentStartedAt = 0L

    private var finished: SessionState.Finished? = null

    /** True while a session is counting down, running or paused. */
    val isActive: Boolean get() = phase == Phase.COUNTDOWN || phase == Phase.RUNNING || phase == Phase.PAUSED

    fun start(plannedSeconds: Int) {
        require(plannedSeconds > 0) { "plannedSeconds must be positive" }
        check(!isActive) { "A session is already active" }
        plannedMs = plannedSeconds * 1000L
        accumulatedMs = 0
        finished = null
        countdownEndsAt = clock() + COUNTDOWN_SECONDS * 1000L
        phase = Phase.COUNTDOWN
    }

    fun pause() {
        if (phase != Phase.RUNNING) return
        accumulatedMs += clock() - segmentStartedAt
        phase = Phase.PAUSED
    }

    fun resume() {
        if (phase != Phase.PAUSED) return
        segmentStartedAt = clock()
        phase = Phase.RUNNING
    }

    /** Stops the session. During the countdown this is a cancel and nothing is recorded. */
    fun stop(): List<EngineEvent> {
        val now = clock()
        return when (phase) {
            Phase.COUNTDOWN -> {
                phase = Phase.IDLE
                listOf(EngineEvent.Cancelled)
            }
            Phase.RUNNING, Phase.PAUSED -> {
                val elapsed = elapsedMs(now).coerceAtMost(plannedMs)
                val seconds = ((elapsed + 500) / 1000).toInt()
                finishWith(seconds, completed = false)
                listOf(EngineEvent.Stopped(seconds, plannedSeconds()))
            }
            else -> emptyList()
        }
    }

    /** Advances the machine to the current time and returns what happened since the last call. */
    fun tick(): List<EngineEvent> {
        val now = clock()
        val events = mutableListOf<EngineEvent>()
        if (phase == Phase.COUNTDOWN && now >= countdownEndsAt) {
            // Anchor the run to the exact end of the countdown to avoid drift from late ticks.
            segmentStartedAt = countdownEndsAt
            phase = Phase.RUNNING
            events += EngineEvent.Began
        }
        if (phase == Phase.RUNNING && elapsedMs(now) >= plannedMs) {
            val seconds = plannedSeconds()
            finishWith(seconds, completed = true)
            events += EngineEvent.Completed(seconds, seconds)
        }
        return events
    }

    /** Current state for display. */
    fun snapshot(): SessionState {
        val now = clock()
        return when (phase) {
            Phase.IDLE -> SessionState.Idle
            Phase.COUNTDOWN -> {
                val left = ((countdownEndsAt - now + 999) / 1000).toInt().coerceIn(1, COUNTDOWN_SECONDS)
                SessionState.Countdown(left)
            }
            Phase.RUNNING -> running(elapsedMs(now)).let {
                SessionState.Running(it.first, plannedSeconds(), it.second)
            }
            Phase.PAUSED -> running(accumulatedMs).let {
                SessionState.Paused(it.first, plannedSeconds(), it.second)
            }
            Phase.FINISHED -> finished ?: SessionState.Idle
        }
    }

    /** Leaves the Finished state. */
    fun reset() {
        if (phase == Phase.FINISHED) {
            phase = Phase.IDLE
            finished = null
        }
    }

    private fun running(elapsed: Long): Pair<Int, Float> {
        val e = elapsed.coerceIn(0, plannedMs)
        val remaining = ((plannedMs - e + 999) / 1000).toInt()
        return remaining to (e.toFloat() / plannedMs)
    }

    private fun elapsedMs(now: Long): Long =
        if (phase == Phase.RUNNING) accumulatedMs + (now - segmentStartedAt) else accumulatedMs

    private fun plannedSeconds(): Int = (plannedMs / 1000).toInt()

    private fun finishWith(meditatedSeconds: Int, completed: Boolean) {
        phase = Phase.FINISHED
        finished = SessionState.Finished(meditatedSeconds, plannedSeconds(), completed)
    }

    companion object {
        const val COUNTDOWN_SECONDS = 5
    }
}
