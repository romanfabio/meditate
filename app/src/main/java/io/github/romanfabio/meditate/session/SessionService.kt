package io.github.romanfabio.meditate.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.romanfabio.meditate.MainActivity
import io.github.romanfabio.meditate.MeditateApp
import io.github.romanfabio.meditate.R
import io.github.romanfabio.meditate.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process alive (and the CPU awake) while a session runs, so the
 * bells ring on time with the screen off. It mirrors the session in an ongoing notification with
 * Pause/Resume and Stop actions, and stops itself when the session and its last bell are over.
 */
class SessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var manager: SessionManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastKey: Any? = null

    override fun onCreate() {
        super.onCreate()
        manager = (application as MeditateApp).sessionManager
        createChannel()
        // Must call startForeground quickly after startForegroundService().
        startForeground(
            NOTIFICATION_ID,
            buildNotification(manager.state.value, false),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        scope.launch {
            combine(manager.state, manager.bell.playing) { s, p -> s to p }.collect { (s, p) -> onState(s, p) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> manager.pause()
            ACTION_RESUME -> manager.resume()
            ACTION_STOP -> manager.stop()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private fun onState(state: SessionState, bellPlaying: Boolean) {
        val inSession = state is SessionState.Countdown || state is SessionState.Running || state is SessionState.Paused
        if (!inSession && !bellPlaying) {
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Re-post the notification only when its content changes (the chronometer ticks itself).
        val key = when (state) {
            is SessionState.Countdown -> "countdown"
            is SessionState.Running -> "running"
            is SessionState.Paused -> "paused-${state.remainingSeconds}"
            else -> "ending"
        }
        if (key != lastKey) {
            lastKey = key
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(state, bellPlaying))
        }

        when (state) {
            is SessionState.Countdown -> holdWakeLock(60_000L)
            is SessionState.Running -> if (wakeLock?.isHeld != true) holdWakeLock(state.remainingSeconds * 1000L + 90_000L)
            is SessionState.Paused -> releaseWakeLock()
            else -> if (wakeLock?.isHeld != true) holdWakeLock(40_000L) // let the end bell ring out
        }
    }

    private fun holdWakeLock(timeoutMs: Long) {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "meditate:session")
            .also { it.setReferenceCounted(false); wakeLock = it }
        lock.acquire(timeoutMs)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(state: SessionState, bellPlaying: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bowl)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        when (state) {
            is SessionState.Countdown -> {
                builder.setContentText(getString(R.string.notification_starting))
                builder.addAction(0, getString(R.string.action_cancel), serviceAction(ACTION_STOP))
            }
            is SessionState.Running -> {
                val endsAt = System.currentTimeMillis() + state.remainingSeconds * 1000L
                builder.setContentText(getString(R.string.notification_running))
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setShowWhen(true)
                    .setWhen(endsAt)
                builder.addAction(0, getString(R.string.action_pause), serviceAction(ACTION_PAUSE))
                builder.addAction(0, getString(R.string.action_stop), serviceAction(ACTION_STOP))
            }
            is SessionState.Paused -> {
                builder.setContentText(
                    getString(R.string.notification_paused, formatClock(state.remainingSeconds)),
                )
                builder.addAction(0, getString(R.string.action_resume), serviceAction(ACTION_RESUME))
                builder.addAction(0, getString(R.string.action_stop), serviceAction(ACTION_STOP))
            }
            else -> builder.setContentText(getString(R.string.notification_complete)).setOngoing(bellPlaying)
        }
        return builder.build()
    }

    private fun serviceAction(action: String): PendingIntent =
        PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, SessionService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        private const val CHANNEL_ID = "session"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PAUSE = "io.github.romanfabio.meditate.PAUSE"
        private const val ACTION_RESUME = "io.github.romanfabio.meditate.RESUME"
        private const val ACTION_STOP = "io.github.romanfabio.meditate.STOP"
    }
}
