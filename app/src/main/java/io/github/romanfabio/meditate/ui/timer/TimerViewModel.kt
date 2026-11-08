package io.github.romanfabio.meditate.ui.timer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.romanfabio.meditate.MeditateApp

/** Holds the configured duration so it survives tab switches and rotation. */
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MeditateApp

    var durationSeconds by mutableIntStateOf(app.prefs.lastDurationSeconds)
        private set

    fun setDuration(seconds: Int) {
        durationSeconds = seconds
    }

    fun setPart(hours: Int = durationSeconds / 3600, minutes: Int = durationSeconds % 3600 / 60, seconds: Int = durationSeconds % 60) {
        durationSeconds = hours * 3600 + minutes * 60 + seconds
    }
}
