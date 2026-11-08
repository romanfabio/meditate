package io.github.romanfabio.meditate

import android.app.Application
import io.github.romanfabio.meditate.audio.BellPlayer
import io.github.romanfabio.meditate.data.AppDatabase
import io.github.romanfabio.meditate.data.Prefs
import io.github.romanfabio.meditate.data.SessionRepository
import io.github.romanfabio.meditate.dnd.DndController
import io.github.romanfabio.meditate.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application class: a tiny hand-rolled service locator (no DI library needed). */
class MeditateApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs by lazy { Prefs(this) }
    val repository by lazy { SessionRepository(AppDatabase.get(this).sessionDao()) }
    val dnd by lazy { DndController(this, prefs) }
    val sessionManager by lazy {
        SessionManager(this, appScope, prefs, repository, dnd, BellPlayer(this))
    }

    override fun onCreate() {
        super.onCreate()
        // A fresh process has no running session; if a crash left Do Not Disturb on, undo it.
        dnd.restore()
    }
}
