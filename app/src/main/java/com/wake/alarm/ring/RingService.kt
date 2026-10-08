package com.wake.alarm.ring

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.wake.alarm.data.Alarm
import com.wake.alarm.data.AlarmRepository
import com.wake.alarm.data.WakeStore
import com.wake.alarm.schedule.AlarmScheduler
import com.wake.alarm.util.Times
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Owns the sound for the whole wake-up. The UI only sends it small commands.
 *
 * Phases:  RINGING (sound on)  ->  first interaction with a mission  ->  MISSIONS (sound off)
 *
 * While in MISSIONS a watchdog restarts the sound if the user walks away (leaves the
 * screen, or stops interacting for a couple of minutes) before finishing. That is what
 * stops "start the challenge, then fall back asleep".
 */
class RingService : Service() {

    companion object {
        const val ACTION_START = "com.wake.alarm.ring.START"
        const val ACTION_INTERACT = "com.wake.alarm.ring.INTERACT"
        const val ACTION_SNOOZE = "com.wake.alarm.ring.SNOOZE"
        const val ACTION_FINISH = "com.wake.alarm.ring.FINISH"
        const val ACTION_MISSIONS_DONE = "com.wake.alarm.ring.MISSIONS_DONE"
        const val ACTION_GONE = "com.wake.alarm.ring.GONE"
        const val ACTION_BACK = "com.wake.alarm.ring.BACK"
        const val EXTRA_GRACE_MS = "grace_ms"

        private const val NOTIF_ID = 4242
        private const val IDLE_LIMIT_MS = 150_000L
        private const val AWAY_LIMIT_MS = 20_000L
        private const val WATCHDOG_MS = 5_000L

        fun start(context: Context, alarmId: Int, test: Boolean, json: String?) {
            val i = Intent(context, RingService::class.java).apply {
                action = ACTION_START
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_TEST, test)
                if (json != null) putExtra(AlarmScheduler.EXTRA_JSON, json)
            }
            ContextCompat.startForegroundService(context, i)
        }

        /** Small commands from the UI. Only call while the app is visible. */
        fun send(context: Context, action: String, graceMs: Long = 0L) {
            val i = Intent(context, RingService::class.java).apply {
                this.action = action
                putExtra(EXTRA_GRACE_MS, graceMs)
            }
            runCatching { context.startService(i) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var audio: AlarmAudio? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var alarm: Alarm? = null
    private var isTest = false
    private var alarmJson: String? = null

    private var soundOn = false
    private var missionsStarted = false
    /** All configured missions are finished: the sound is off for good, whatever the user does next. */
    private var missionsDone = false
    private var lastInteraction = 0L
    private var goneSince = 0L
    private var graceUntil = 0L

    private val watchdog = object : Runnable {
        override fun run() {
            if (alarm == null) return
            if (missionsStarted && !missionsDone && !soundOn) {
                val now = SystemClock.elapsedRealtime()
                val idle = now - lastInteraction > IDLE_LIMIT_MS
                val away = goneSince != 0L && now - goneSince > AWAY_LIMIT_MS
                if (now > graceUntil && (idle || away)) startSound(reRing = true)
            }
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WakeStore.init(this)
        val command = intent?.action
        when (command) {
            ACTION_START, null -> handleStart(intent)
            ACTION_INTERACT -> onInteract(intent?.getLongExtra(EXTRA_GRACE_MS, 0L) ?: 0L)
            ACTION_SNOOZE -> snooze()
            ACTION_MISSIONS_DONE -> onMissionsDone()
            ACTION_FINISH -> finishWakeUp()
            ACTION_GONE -> goneSince = SystemClock.elapsedRealtime()
            ACTION_BACK -> {
                goneSince = 0L
                lastInteraction = SystemClock.elapsedRealtime()
            }
        }
        // A stray command (e.g. sent after the wake-up ended) must not leave an idle service behind.
        if (command != null && command != ACTION_START && alarm == null) stopSelf()
        return START_STICKY
    }

    // ------------------------------------------------------------------ start

    private fun handleStart(intent: Intent?) {
        val now = System.currentTimeMillis()
        val test = intent?.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false) ?: false
        val json = intent?.getStringExtra(AlarmScheduler.EXTRA_JSON)
        val id = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1

        val requested: Alarm? = when {
            test && json != null -> runCatching { WakeStore.json.decodeFromString<Alarm>(json) }.getOrNull()
            id >= 0 -> WakeStore.get(id)
            // Service was killed and restarted by the system: resume the wake-up in progress.
            else -> WakeStore.alarms().firstOrNull { Times.isWakeUpInProgress(it, now) && it.snoozeAt == 0L }
        }
        val target = alarm ?: requested
        if (target == null) {
            // startForegroundService() obliges us to call startForeground() before bailing out.
            promote(RingNotifications.placeholder(this))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        RingNotifications.ensureChannels(this)
        promote(RingNotifications.ring(this, target, if (alarm != null) isTest else test, if (alarm != null) alarmJson else json))
        getSystemService(NotificationManager::class.java).cancel(RingNotifications.ID_FALLBACK)

        if (alarm != null) return // already ringing; nothing more to do

        alarm = target
        isTest = test
        alarmJson = if (test) (json ?: WakeStore.json.encodeToString(target)) else null
        missionsStarted = false
        missionsDone = false
        goneSince = 0L
        graceUntil = 0L
        lastInteraction = SystemClock.elapsedRealtime()

        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:ring").apply {
            acquire(2 * 60 * 60 * 1000L)
        }
        RingState.flow.value = RingUiState(alarm = target, isTest = test, soundOn = false, finished = false)
        startSound(reRing = false)
        handler.postDelayed(watchdog, WATCHDOG_MS)
        // Sound is already playing. Now also try to open the screen directly (the notification's
        // full-screen intent is the other route; see Readiness for when each one works).
        RingLauncher.openScreen(this, target, test, alarmJson)
    }

    private fun promote(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    // ------------------------------------------------------------------ sound

    private fun startSound(reRing: Boolean) {
        val a = alarm ?: return
        audio?.stop()
        audio = AlarmAudio(this).also { it.start(a.audio, if (reRing) 8 else a.rampSeconds, a.vibrate) }
        soundOn = true
        RingState.flow.update { it.copy(soundOn = true) }
        if (reRing) {
            // Post again so the full-screen intent can bring the screen back.
            runCatching {
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIF_ID, RingNotifications.ring(this, a, isTest, alarmJson))
            }
            RingLauncher.openScreen(this, a, isTest, alarmJson)
        }
    }

    private fun stopSound() {
        audio?.stop()
        audio = null
        soundOn = false
        RingState.flow.update { it.copy(soundOn = false) }
    }

    // ------------------------------------------------------------------ commands

    /** The user touched a mission: the sound stops immediately. */
    private fun onInteract(graceMs: Long) {
        if (alarm == null) return
        val now = SystemClock.elapsedRealtime()
        missionsStarted = true
        lastInteraction = now
        goneSince = 0L
        if (graceMs > 0) graceUntil = maxOf(graceUntil, now + graceMs)
        if (soundOn) stopSound()
    }

    /**
     * The user finished every configured mission. From here the alarm cannot ring again: the sound is
     * off and the "walked away" watchdog no longer restarts it (it keeps running only until the
     * service ends). The success is recorded now, so the streak counts even if "I'M WIDE AWAKE"
     * is never pressed.
     */
    private fun onMissionsDone() {
        val a = alarm ?: return
        missionsStarted = true
        missionsDone = true
        stopSound()
        if (!isTest) AlarmRepository.recordSuccess(a.id)
    }

    /** One snooze per wake-up. Enforced here, not just in the UI. */
    private fun snooze() {
        val a = alarm ?: return
        if (missionsStarted) return
        val fresh = if (isTest) a else (WakeStore.get(a.id) ?: a)
        if (fresh.snoozeUsed) return
        stopSound()
        if (isTest) {
            val json = WakeStore.json.encodeToString(fresh.copy(snoozeUsed = true))
            AlarmScheduler.scheduleSnooze(
                this, 0, System.currentTimeMillis() + Times.TEST_SNOOZE_MS, test = true, json = json
            )
        } else {
            AlarmRepository.markSnoozed(this, a.id)
        }
        endWakeUp()
    }

    private fun finishWakeUp() {
        val a = alarm
        if (a != null && !isTest) AlarmRepository.complete(this, a.id)
        endWakeUp()
    }

    private fun endWakeUp() {
        stopSound()
        handler.removeCallbacks(watchdog)
        alarm = null
        RingState.flow.value = RingUiState(finished = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        audio?.stop()
        audio = null
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        super.onDestroy()
    }
}
