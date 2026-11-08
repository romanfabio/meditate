package io.github.romanfabio.meditate.data

import android.content.Context
import android.content.SharedPreferences

/** Small key/value settings. Everything here is tiny, so plain SharedPreferences is enough. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("meditate_prefs", Context.MODE_PRIVATE)

    /** Last duration a session was *started* with. Defaults to 20 minutes. */
    var lastDurationSeconds: Int
        get() = sp.getInt(KEY_LAST_DURATION, DEFAULT_DURATION_SECONDS)
        set(value) = sp.edit().putInt(KEY_LAST_DURATION, value).apply()

    /** Whether Do Not Disturb should be enabled during sessions. */
    var dndEnabled: Boolean
        get() = sp.getBoolean(KEY_DND_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_DND_ENABLED, value).apply()

    /** True while this app has switched DND on and still has to switch it off again. */
    var dndOwned: Boolean
        get() = sp.getBoolean(KEY_DND_OWNED, false)
        set(value) = sp.edit().putBoolean(KEY_DND_OWNED, value).apply()

    /** Interruption filter that was active before this app changed it. */
    var dndPreviousFilter: Int
        get() = sp.getInt(KEY_DND_PREVIOUS, 1)
        set(value) = sp.edit().putInt(KEY_DND_PREVIOUS, value).apply()

    companion object {
        const val DEFAULT_DURATION_SECONDS = 20 * 60
        private const val KEY_LAST_DURATION = "last_duration_seconds"
        private const val KEY_DND_ENABLED = "dnd_enabled"
        private const val KEY_DND_OWNED = "dnd_owned"
        private const val KEY_DND_PREVIOUS = "dnd_previous_filter"
    }
}
