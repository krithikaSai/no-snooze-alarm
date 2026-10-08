package com.wake.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.wake.alarm.data.Alarm
import com.wake.alarm.data.AlarmRepository
import com.wake.alarm.data.WakeStore
import com.wake.alarm.ring.RingActivity
import com.wake.alarm.ring.RingState
import com.wake.alarm.ui.AlarmEditorScreen
import com.wake.alarm.ui.AlarmListScreen
import com.wake.alarm.ui.HistoryScreen
import com.wake.alarm.ui.MissionFlow
import com.wake.alarm.ui.StatusScreen
import com.wake.alarm.ui.WelcomeScreen
import com.wake.alarm.ui.WakeTheme
import kotlinx.serialization.encodeToString

private sealed class Screen {
    object List : Screen()
    /** [draft] carries unsaved edits across a trip to "test missions". */
    data class Editor(val alarmId: Int?, val draft: Alarm? = null) : Screen()
    object History : Screen()
    object Status : Screen()
    data class MissionTest(val alarm: Alarm) : Screen()
}

class MainActivity : ComponentActivity() {

    // Notifications are what opens the wake-up screen, so ask for them up front (Android 13+).
    // If refused, the banner on the alarm list explains why it matters and links to settings.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onResume() {
        super.onResume()
        // Heal any alarm the system may have dropped (force-stop, update) and pick up the exact-alarm
        // permission as soon as the user comes back from the settings page. Never throws: with the
        // permission missing, AlarmScheduler simply schedules nothing.
        runCatching { AlarmRepository.rescheduleAll(this) }
        openRingScreenIfRinging()
    }

    /**
     * Guaranteed way back to the wake-up screen: if an alarm is ringing, opening the app (or tapping
     * the notification) goes straight to it, whatever the full-screen intent did or didn't do.
     */
    private fun openRingScreenIfRinging() {
        val state = RingState.flow.value
        val ringing = state.alarm ?: return
        if (state.finished) return
        val json = if (state.isTest) WakeStore.json.encodeToString(ringing) else null
        runCatching { startActivity(RingActivity.intent(this, ringing.id, state.isTest, json)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WakeStore.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            WakeTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.List) }
                // First launch only. Anyone who already has alarms or history (an update from an older build)
                // skips it, and START stores the flag so it never shows again.
                var welcome by remember {
                    val d = WakeStore.state.value
                    mutableStateOf(!d.welcomeDone && d.alarms.isEmpty() && d.history.isEmpty())
                }

                if (screen != Screen.List) BackHandler { screen = Screen.List }

                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (welcome) {
                        WelcomeScreen(onStart = {
                            WakeStore.mutate { it.copy(welcomeDone = true) }
                            welcome = false
                        })
                        return@Box
                    }
                    when (val s = screen) {
                        Screen.List -> AlarmListScreen(
                            onEdit = { screen = Screen.Editor(it) },
                            onHistory = { screen = Screen.History },
                            onStatus = { screen = Screen.Status }
                        )

                        is Screen.Editor -> AlarmEditorScreen(
                            alarmId = s.alarmId,
                            draft = s.draft,
                            onClose = { screen = Screen.List },
                            onTestAlarm = { draft ->
                                val json = WakeStore.json.encodeToString(draft.copy(id = 0))
                                startActivity(RingActivity.intent(this@MainActivity, 0, true, json))
                            },
                            onTestMissions = { draft -> screen = Screen.MissionTest(draft) }
                        )

                        Screen.History -> HistoryScreen(onBack = { screen = Screen.List })
                        Screen.Status -> StatusScreen(onBack = { screen = Screen.List })

                        is Screen.MissionTest -> {
                            val back = { screen = Screen.Editor(s.alarm.id.takeIf { it > 0 }, s.alarm) }
                            BackHandler { back() }
                            MissionFlow(
                                settings = s.alarm.missions,
                                onInteract = {},
                                onAwake = {},
                                onClose = { back() }
                            )
                        }
                    }
                }
            }
        }
    }
}
