package com.wake.alarm.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wake.alarm.ring.Readiness

/** Increases every time the screen comes back to the foreground (e.g. from a system settings page). */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick += 1
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}

/**
 * Shown while Android would stop the wake-up screen from appearing. The sound would still play,
 * but there would be no screen to do the challenge on, so this is treated as a blocking problem.
 */
@Composable
fun ReadinessBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val notificationsOk = remember(tick) { Readiness.notificationsOk(context) }
    val fullScreenOk = remember(tick) { Readiness.fullScreenOk(context) }

    // The system prompt only appears once or twice; after that the only way is the settings page.
    var asked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        asked = true
    }

    if (notificationsOk && fullScreenOk) return

    Column(
        modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.onBackground)
            .padding(12.dp)
    ) {
        Text(
            "THE WAKE-UP SCREEN WON'T APPEAR",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "The alarm would ring, but with nowhere to do the challenge and switch it off. Fix the items below.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!notificationsOk) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Notifications are off. The alarm screen is opened by a notification, so without them you only hear the sound.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            WakeButton(
                "ALLOW NOTIFICATIONS",
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !asked) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Readiness.openNotificationSettings(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                filled = true
            )
        }

        if (!fullScreenOk) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Full-screen alarms are off. Android needs this switched on to show the alarm over your lock screen.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            WakeButton(
                "ALLOW FULL-SCREEN ALARM",
                onClick = { Readiness.openFullScreenSettings(context) },
                modifier = Modifier.fillMaxWidth(),
                filled = true
            )
        }
    }
}
