package com.wake.alarm.ui

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.wake.alarm.data.Alarm
import com.wake.alarm.data.AlarmRepository
import com.wake.alarm.data.Streaks
import com.wake.alarm.data.WakeStore
import com.wake.alarm.mission.MissionPlan
import com.wake.alarm.util.Times
import kotlinx.coroutines.delay

@Composable
fun AlarmListScreen(
    onEdit: (Int?) -> Unit,
    onHistory: () -> Unit,
    onStatus: () -> Unit
) {
    val context = LocalContext.current
    val data by WakeStore.state.collectAsState()
    // Re-evaluate lock states and countdowns while the screen is open.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }
    val exactAlarmsOk = rememberExactAlarmAllowed()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (!exactAlarmsOk) {
            ExactAlarmBanner()
            Spacer(Modifier.height(16.dp))
        }

        // Shows itself only while notifications or full-screen alarms are blocked; otherwise draws nothing.
        ReadinessBanner(Modifier.padding(bottom = 16.dp))

        if (data.alarms.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("No alarms yet.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Set your first alarm and choose how you want to be dragged out of bed :)",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        data.alarms.sortedWith(compareBy({ it.hour }, { it.minute })).forEach { alarm ->
            AlarmRow(
                alarm = alarm,
                now = now,
                streak = Streaks.forAlarm(alarm, data.history),
                onOpen = { onEdit(alarm.id) },
                onToggle = {
                    val ok = AlarmRepository.setEnabled(context, alarm.id, !alarm.enabled)
                    if (!ok) {
                        Toast.makeText(
                            context, "Locked. This alarm rings within the hour, so it can't be changed.", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }

        Spacer(Modifier.height(24.dp))
        WakeButton("+ NEW ALARM", onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth(), filled = true)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WakeButton("HISTORY", onClick = onHistory, modifier = Modifier.weight(1f))
            WakeButton("STATUS", onClick = onStatus, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, now: Long, streak: Int, onOpen: () -> Unit, onToggle: () -> Unit) {
    val context = LocalContext.current
    val locked = Times.isLocked(alarm, now)
    val ink = MaterialTheme.colorScheme.onBackground

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                Times.formatClock(context, alarm.hour, alarm.minute),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = if (alarm.enabled) ink else MaterialTheme.colorScheme.outline
            )
            Text(Times.daysLabel(alarm.repeatDays), style = MaterialTheme.typography.bodyMedium)
            Text(
                MissionPlan.summary(alarm.missions).joinToString("  +  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (locked) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "LOCKED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.border(1.5.dp, ink).padding(horizontal = 6.dp, vertical = 2.dp)
                )
                val msg = if (Times.isWakeUpInProgress(alarm, now)) {
                    "Wake-up in progress."
                } else {
                    "Rings in ${Times.countdown(Times.nextTrigger(alarm, now) - now)}. No edits or delete."
                }
                Text(msg, style = MaterialTheme.typography.bodySmall)
            } else if (alarm.enabled) {
                Text(
                    "Rings in ${Times.countdown(Times.nextTrigger(alarm, now) - now)}. Locks 1h before.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (streak > 0) {
                Text(
                    "$streak DAY STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        // Same toggle action as before; the switch just reflects alarm.enabled (a locked alarm stays put and shows the toast).
        Switch(
            checked = alarm.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
