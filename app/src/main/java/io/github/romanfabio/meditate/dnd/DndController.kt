package io.github.romanfabio.meditate.dnd

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.github.romanfabio.meditate.data.Prefs

/**
 * Switches Do Not Disturb to "Priority only" for the duration of a session and puts the previous
 * state back afterwards.
 *
 * Safety rules:
 *  - If DND is already on when the session starts, it is left alone (and not turned off later).
 *  - On restore, the filter is only changed back if it is still the one this app set, so a change
 *    made by the user in the meantime is never overwritten.
 *  - Ownership is persisted in [Prefs], so a crash cannot leave DND stuck on: [restore] is also
 *    called on app start.
 */
class DndController(context: Context, private val prefs: Prefs) {
    private val appContext = context.applicationContext
    private val nm = appContext.getSystemService(NotificationManager::class.java)

    fun hasAccess(): Boolean = nm.isNotificationPolicyAccessGranted

    /** Intent for the system screen where the user grants Do Not Disturb access. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Turns DND on if allowed and not already on. Safe to call without access (no-op). */
    fun enable() {
        if (!hasAccess() || prefs.dndOwned) return
        val current = nm.currentInterruptionFilter
        if (current != NotificationManager.INTERRUPTION_FILTER_ALL) return // already on / unknown
        prefs.dndPreviousFilter = current
        prefs.dndOwned = true
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    /** Restores the state saved by [enable], if this app changed it. */
    fun restore() {
        if (!prefs.dndOwned) return
        if (hasAccess() && nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            nm.setInterruptionFilter(prefs.dndPreviousFilter)
        }
        prefs.dndOwned = false
    }
}
