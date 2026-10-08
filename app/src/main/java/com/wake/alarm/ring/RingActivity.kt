package com.wake.alarm.ring

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wake.alarm.data.Alarm
import com.wake.alarm.data.WakeStore
import com.wake.alarm.schedule.AlarmScheduler
import com.wake.alarm.ui.MissionFlow
import com.wake.alarm.ui.RingScreen
import com.wake.alarm.ui.WakeTheme
import com.wake.alarm.util.Times
import kotlinx.serialization.decodeFromString

/**
 * Shows over the lock screen, keeps the screen on, and goes immersive.
 * It does NOT own the sound; RingService does, so leaving this screen never silences the alarm.
 */
class RingActivity : ComponentActivity() {

    companion object {
        fun intent(context: Context, alarmId: Int, test: Boolean, json: String?): Intent =
            Intent(context, RingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_TEST, test)
                if (json != null) putExtra(AlarmScheduler.EXTRA_JSON, json)
            }
    }

    private var alarm: Alarm? = null
    private var isTest = false
    private var json: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WakeStore.init(this)
        showOverLockScreen()

        if (!resolve(intent)) {
            finish()
            return
        }
        val a = alarm ?: return

        // A leftover "finished" flag from the previous wake-up must not close this one.
        if (RingState.flow.value.finished) RingState.flow.value = RingUiState()
        // Normally the service is already ringing. If not (e.g. opened from a backup notification), start it.
        if (RingState.flow.value.alarm == null) {
            runCatching { RingService.start(this, a.id, isTest, json) }
        }

        setContent {
            WakeTheme {
                val state by RingState.flow.collectAsState()
                var started by rememberSaveable { mutableStateOf(false) }
                val current = state.alarm ?: a

                LaunchedEffect(state.finished) {
                    if (state.finished) finishAndRemoveTask()
                }
                // No back button: the only way out is through the challenge (or the one snooze).
                BackHandler(enabled = true) { }

                if (!started) {
                    RingScreen(
                        alarm = current,
                        isTest = isTest,
                        snoozeAvailable = !current.snoozeUsed,
                        onSnooze = { RingService.send(this@RingActivity, RingService.ACTION_SNOOZE) },
                        onStartChallenge = { started = true }
                    )
                } else {
                    MissionFlow(
                        settings = current.missions,
                        onInteract = { grace -> RingService.send(this@RingActivity, RingService.ACTION_INTERACT, grace) },
                        onAwake = { RingService.send(this@RingActivity, RingService.ACTION_FINISH) },
                        onClose = { finishAndRemoveTask() },
                        onMissionsComplete = { RingService.send(this@RingActivity, RingService.ACTION_MISSIONS_DONE) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolve(intent)
    }

    override fun onStart() {
        super.onStart()
        if (RingState.flow.value.alarm != null) RingService.send(this, RingService.ACTION_BACK)
    }

    override fun onStop() {
        if (RingState.flow.value.alarm != null) RingService.send(this, RingService.ACTION_GONE)
        super.onStop()
    }

    private fun resolve(i: Intent): Boolean {
        isTest = i.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)
        json = i.getStringExtra(AlarmScheduler.EXTRA_JSON)
        alarm = if (isTest) {
            json?.let { runCatching { WakeStore.json.decodeFromString<Alarm>(it) }.getOrNull() }
        } else {
            WakeStore.get(i.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
                ?.takeIf { Times.isWakeUpInProgress(it) }
        }
        return alarm != null
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
