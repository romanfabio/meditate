package io.github.romanfabio.meditate.session

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.romanfabio.meditate.R
import io.github.romanfabio.meditate.audio.BellPlayer
import io.github.romanfabio.meditate.data.Prefs
import io.github.romanfabio.meditate.data.SessionRepository
import io.github.romanfabio.meditate.dnd.DndController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Process-wide owner of the running session. The UI sends commands here and observes [state];
 * [SessionService] keeps the process alive and mirrors the state in a notification.
 * Must be used from the main thread.
 */
class SessionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val prefs: Prefs,
    private val repository: SessionRepository,
    private val dnd: DndController,
    val bell: BellPlayer,
    private val wallClock: () -> Long = System::currentTimeMillis,
) {
    private val engine = SessionEngine(SystemClock::elapsedRealtime)
    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var meditationStartedAt = 0L

    /** Starts the countdown. Ignored when a session is already active. */
    fun start(plannedSeconds: Int) {
        if (engine.isActive || plannedSeconds <= 0) return
        engine.reset()
        prefs.lastDurationSeconds = plannedSeconds
        if (prefs.dndEnabled) dnd.enable()
        engine.start(plannedSeconds)
        publish()
        ContextCompat.startForegroundService(context, Intent(context, SessionService::class.java))
        startTicking()
    }

    fun pause() {
        engine.pause()
        publish()
    }

    fun resume() {
        engine.resume()
        publish()
    }

    fun stop() {
        handle(engine.stop())
        publish()
    }

    /** Leaves the "finished" screen. */
    fun dismissFinished() {
        engine.reset()
        publish()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && engine.isActive) {
                handle(engine.tick())
                publish()
                delay(TICK_MS)
            }
        }
    }

    private fun publish() {
        _state.value = engine.snapshot()
    }

    private fun handle(events: List<EngineEvent>) {
        for (event in events) {
            when (event) {
                EngineEvent.Began -> {
                    meditationStartedAt = wallClock()
                    bell.play(R.raw.bell_start)
                }
                is EngineEvent.Completed -> {
                    dnd.restore()
                    bell.play(R.raw.bell_end)
                    save(event.meditatedSeconds, event.plannedSeconds, completed = true)
                }
                is EngineEvent.Stopped -> {
                    dnd.restore()
                    bell.fadeOutAndStop()
                    save(event.meditatedSeconds, event.plannedSeconds, completed = false)
                }
                EngineEvent.Cancelled -> {
                    dnd.restore()
                    bell.fadeOutAndStop()
                }
            }
        }
    }

    private fun save(meditatedSeconds: Int, plannedSeconds: Int, completed: Boolean) {
        val startedAt = meditationStartedAt
        scope.launch {
            repository.record(startedAt, meditatedSeconds, plannedSeconds, completed)
        }
    }

    companion object {
        private const val TICK_MS = 200L
    }
}
