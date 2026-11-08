package io.github.romanfabio.meditate.session

/** What the UI (and the notification) show. Produced by [SessionEngine.snapshot]. */
sealed interface SessionState {
    data object Idle : SessionState

    /** The 5 second lead-in. [secondsLeft] counts 5, 4, 3, 2, 1. */
    data class Countdown(val secondsLeft: Int) : SessionState

    data class Running(
        val remainingSeconds: Int,
        val plannedSeconds: Int,
        /** 0f..1f, how much of the planned time has elapsed. */
        val progress: Float,
    ) : SessionState

    data class Paused(
        val remainingSeconds: Int,
        val plannedSeconds: Int,
        val progress: Float,
    ) : SessionState

    /** Shown after the timer reached zero or the user stopped a running session. */
    data class Finished(
        val meditatedSeconds: Int,
        val plannedSeconds: Int,
        val completed: Boolean,
    ) : SessionState
}

/** Transitions reported by [SessionEngine] so the owner can play sounds, save, etc. */
sealed interface EngineEvent {
    /** Countdown reached zero; meditation time starts counting now. */
    data object Began : EngineEvent

    /** Timer ran down to zero. */
    data class Completed(val meditatedSeconds: Int, val plannedSeconds: Int) : EngineEvent

    /** User stopped a running or paused session. */
    data class Stopped(val meditatedSeconds: Int, val plannedSeconds: Int) : EngineEvent

    /** User cancelled during the countdown; nothing to record. */
    data object Cancelled : EngineEvent
}
