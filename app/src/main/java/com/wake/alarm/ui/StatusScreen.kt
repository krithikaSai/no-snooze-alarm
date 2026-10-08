package com.wake.alarm.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wake.alarm.ring.Readiness
import com.wake.alarm.schedule.ExactAlarms

/**
 * Plain-language list of the permissions this app uses and why, with a button for each
 * that isn't granted yet. Nothing is requested until you press a button here (or enable
 * the Steps / Jumps challenge in the editor).
 */
@Composable
fun StatusScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    var askedNotifications by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedNotifications = true
        refresh++
    }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    // Reading the counter makes the checks below re-run after returning from system settings.
    @Suppress("UNUSED_VARIABLE") val tick = refresh
    val exactAlarmsOk = ExactAlarms.canSchedule(context)
    val notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val fullScreenOk = Build.VERSION.SDK_INT < 34 ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    val activityOk = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    val hasStepSensor = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        WakeButton("← BACK", onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text("STATUS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Everything in this app works offline. There is no need to create an account, no internet permissions required, " +
                "and no data leaves your phone. The app is deliberately designed in a very simple, old-fashioned style: " +
                "straightforward, practical, and free of unnecessary clutter.",
            style = MaterialTheme.typography.bodyMedium
        )

        PermissionRow(
            title = "Alarms & reminders",
            ok = exactAlarmsOk,
            why = "Required by Android before any app can set alarms. Without it nothing will ring.",
            action = "OPEN SETTINGS"
        ) {
            ExactAlarms.openSettings(context)
        }

        PermissionRow(
            title = "Notifications",
            ok = notificationsOk,
            why = "Needed to show the alarm over your lock screen, and for bedtime reminders.",
            action = "ALLOW"
        ) {
            if (Build.VERSION.SDK_INT >= 33 && !askedNotifications) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Android won't show the prompt again after a denial: send them to the settings page.
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        if (Build.VERSION.SDK_INT >= 34) {
            PermissionRow(
                title = "Full-screen alarm",
                ok = fullScreenOk,
                why = "Lets the wake-up screen take over your display when the alarm goes off.",
                action = "OPEN SETTINGS"
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        PermissionRow(
            title = "Display over other apps (recommended)",
            ok = Readiness.overlayOk(context),
            why = "Lets the wake-up screen open on top of whatever you're doing while the phone is unlocked. " +
                "Without it you get a banner to tap instead. On a locked phone it isn't needed.",
            action = "OPEN SETTINGS"
        ) {
            Readiness.openOverlaySettings(context)
        }

        PermissionRow(
            title = "Step counting",
            ok = activityOk && hasStepSensor,
            why = if (hasStepSensor) {
                "Only for the Steps / Jumps challenge, to count your steps and jumps. If you don't allow it, that challenge is swapped for easy math."
            } else {
                "This phone has no step counter, so the Steps / Jumps challenge is swapped for easy math."
            },
            action = if (hasStepSensor) "ALLOW" else null
        ) {
            activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        SectionTitle("How the alarm stays reliable")
        Text(
            "Your alarms use Android's built-in alarm system, so they can ring even when the app is closed or your phone is idle. " +
                "Your alarms are also restored after restarting your phone.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "While an alarm is ringing, the sound continues even if you switch to another app. " +
                "The alarm volume is temporarily raised to maximum and restored afterwards.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Some phones have extra battery-saving settings that can interfere with alarms. " +
                "If an alarm ever fails to ring, check your phone's battery settings and make sure this app isn't being restricted. " +
                "You can also use TEST ALARM from the alarm editor to make sure everything is working.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PermissionRow(title: String, ok: Boolean, why: String, action: String?, onAction: () -> Unit) {
    SectionTitle(title)
    Text(
        if (ok) "✓ ALLOWED" else if (action != null) "⚠ NOT ALLOWED" else "⚠ NOT AVAILABLE",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = if (ok) goodColor() else badColor()
    )
    Spacer(Modifier.height(4.dp))
    Text(why, style = MaterialTheme.typography.bodyMedium)
    if (!ok && action != null) {
        Spacer(Modifier.height(8.dp))
        WakeButton(action, onClick = onAction, modifier = Modifier.fillMaxWidth())
    }
}
