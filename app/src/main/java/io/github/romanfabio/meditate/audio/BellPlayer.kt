package io.github.romanfabio.meditate.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays the bell sounds on the alarm stream, so they are audible while Do Not Disturb is on.
 * Briefly takes transient audio focus (allowing ducking) so other audio is lowered, not stopped.
 */
class BellPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener { }
        .build()

    private var player: MediaPlayer? = null

    private val _playing = MutableStateFlow(false)

    /** True while a bell is audible (including its fade-out). */
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    fun play(@RawRes resId: Int) {
        releaseNow()
        audioManager.requestAudioFocus(focusRequest)
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(attributes)
            appContext.resources.openRawResourceFd(resId).use {
                mp.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            mp.setOnCompletionListener { if (player === mp) releaseNow() }
            mp.setOnErrorListener { _, _, _ ->
                if (player === mp) releaseNow()
                true
            }
            mp.prepare()
            player = mp
            _playing.value = true
            mp.start()
        } catch (e: Exception) {
            mp.release()
            audioManager.abandonAudioFocusRequest(focusRequest)
            _playing.value = false
        }
    }

    /** Fades the current bell out over about 0.7 s, then releases it. */
    fun fadeOutAndStop() {
        val mp = player ?: return
        val steps = 14
        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                if (player !== mp) return
                step++
                val volume = (1f - step.toFloat() / steps).coerceAtLeast(0f)
                try {
                    mp.setVolume(volume, volume)
                } catch (_: IllegalStateException) {
                    return
                }
                if (step >= steps) releaseNow() else handler.postDelayed(this, 50)
            }
        }
        handler.post(runnable)
    }

    private fun releaseNow() {
        handler.removeCallbacksAndMessages(null)
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
        audioManager.abandonAudioFocusRequest(focusRequest)
        _playing.value = false
    }
}
