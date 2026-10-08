package com.wake.alarm.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wake.alarm.schedule.ExactAlarms

/** Live "can this app set alarms?" flag. Re-checked every time the user returns from system settings. */
@Composable
fun rememberExactAlarmAllowed(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(ExactAlarms.canSchedule(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = ExactAlarms.canSchedule(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return allowed
}

/** Shown at the top of the alarm list while Android is blocking alarms. Alarms stay saved, they just can't ring. */
@Composable
fun ExactAlarmBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.onBackground)
            .padding(12.dp)
    ) {
        Text("ALARMS CAN'T RING YET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Android needs your OK for \"Alarms & reminders\" before this app can set alarms. " +
                "Your alarms are saved, but none will ring until you switch it on for No Snooze Alarm.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        WakeButton(
            "OPEN ALARMS & REMINDERS",
            onClick = { ExactAlarms.openSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            filled = true
        )
    }
}
