package com.wake.alarm.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.SystemClock

/**
 * Plays the alarm sound (looping, with a gradual volume ramp) and vibrates.
 * Lives inside RingService, never inside an activity, so it survives navigation.
 *
 * Reliability fallbacks: chosen asset -> system default alarm tone -> system notification tone.
 * While playing, the ALARM stream is raised to max and restored afterwards.
 */
class AlarmAudio(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var player: MediaPlayer? = null
    private var previousStreamVolume = -1
    private var rampStart = 0L
    private var rampMs = 0L

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val rampTick = object : Runnable {
        override fun run() {
            val p = player ?: return
            val t = ((SystemClock.elapsedRealtime() - rampStart).toFloat() / rampMs).coerceIn(0f, 1f)
            val v = 0.05f + 0.95f * t
            runCatching { p.setVolume(v, v) }
            if (t < 1f) handler.postDelayed(this, 500)
        }
    }

    fun start(assetPath: String, rampSeconds: Int, vibrate: Boolean) {
        stop()
        previousStreamVolume = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_ALARM) }.getOrDefault(-1)
        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0
            )
        }

        val candidates = listOf<() -> MediaPlayer?>(
            { if (assetPath.isNotBlank()) fromAsset(assetPath) else null },
            { fromUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) },
            { fromUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) }
        )
        for (make in candidates) {
            val p = runCatching { make() }.getOrNull() ?: continue
            player = p
            break
        }
        player?.let { p ->
            if (rampSeconds > 0) {
                p.setVolume(0.05f, 0.05f)
                rampStart = SystemClock.elapsedRealtime()
                rampMs = rampSeconds * 1000L
                handler.post(rampTick)
            } else {
                p.setVolume(1f, 1f)
            }
            runCatching { p.start() }
        }
        if (vibrate) startVibration()
    }

    private fun fromAsset(path: String): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attrs)
            context.assets.openFd(path).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            prepared(mp)
        } catch (e: Exception) {
            mp.release()
            null
        }
    }

    private fun fromUri(uri: android.net.Uri?): MediaPlayer? {
        if (uri == null) return null
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attrs)
            mp.setDataSource(context, uri)
            prepared(mp)
        } catch (e: Exception) {
            mp.release()
            null
        }
    }

    private fun prepared(mp: MediaPlayer): MediaPlayer {
        mp.isLooping = true
        mp.setOnErrorListener { _, _, _ -> true } // swallow; the service keeps vibrating/ringing what it can
        mp.prepare()
        return mp
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val v = vibrator() ?: return
        if (!v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
        runCatching { v.vibrate(effect, attrs) }
    }

    fun stop() {
        handler.removeCallbacks(rampTick)
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
        runCatching { vibrator()?.cancel() }
        if (previousStreamVolume >= 0) {
            runCatching { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousStreamVolume, 0) }
            previousStreamVolume = -1
        }
    }
}
