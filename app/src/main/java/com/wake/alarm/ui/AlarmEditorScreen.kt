package com.wake.alarm.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wake.alarm.data.Alarm
import com.wake.alarm.data.AlarmRepository
import com.wake.alarm.data.AssetCatalog
import com.wake.alarm.data.BedtimeSettings
import com.wake.alarm.data.MissionSettings
import com.wake.alarm.data.SaveResult
import com.wake.alarm.data.WakeStore
import com.wake.alarm.mission.MissionPlan
import com.wake.alarm.schedule.ExactAlarms
import com.wake.alarm.util.Times
import kotlinx.coroutines.delay

private enum class Sub { NONE, SOUND, BACKGROUND }

/** A little extra air between the editor's major sections, and between the challenge groups inside one. */
private val SECTION_GAP = 56.dp
private val GROUP_GAP = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    alarmId: Int?,
    draft: Alarm?,
    onClose: () -> Unit,
    onTestAlarm: (Alarm) -> Unit,
    onTestMissions: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val data by WakeStore.state.collectAsState()
    val stored = alarmId?.let { id -> data.alarms.firstOrNull { it.id == id } }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }

    var a by remember {
        mutableStateOf(
            draft ?: stored ?: Alarm(
                hour = 7, minute = 0,
                audio = AssetCatalog.defaultSound(context),
                background = AssetCatalog.defaultBackground(context)
            )
        )
    }
    val timeState = rememberTimePickerState(
        initialHour = a.hour,
        initialMinute = a.minute,
        is24Hour = false   // always AM/PM, to match the bedtime picker (hour() still returns 0-23)
    )
    var sub by remember { mutableStateOf(Sub.NONE) }
    var error by remember { mutableStateOf("") }
    var showCreated by remember { mutableStateOf(false) }
    var showPermission by remember { mutableStateOf(false) }
    var createdPending by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showBedtime by remember { mutableStateOf(false) }
    val exactAlarmsOk = rememberExactAlarmAllowed()

    fun current(): Alarm = a.copy(hour = timeState.hour, minute = timeState.minute)

    val activityPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // ---- locked: read-only summary -------------------------------------------------------------
    if (stored != null && Times.isLocked(stored, now)) {
        LockedView(stored, now, onClose, onTestAlarm, onTestMissions)
        return
    }

    when (sub) {
        Sub.SOUND -> {
            BackHandler { sub = Sub.NONE }
            SoundPicker(a.audio, onPick = { a = a.copy(audio = it); sub = Sub.NONE }, onBack = { sub = Sub.NONE })
            return
        }
        Sub.BACKGROUND -> {
            BackHandler { sub = Sub.NONE }
            BackgroundPicker(a.background, onPick = { a = a.copy(background = it); sub = Sub.NONE }, onBack = { sub = Sub.NONE })
            return
        }
        Sub.NONE -> {}
    }

    val m = a.missions
    fun setMissions(change: (MissionSettings) -> MissionSettings) {
        a = a.copy(missions = change(m))
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        BackButton(onClick = onClose)
        Spacer(Modifier.height(12.dp))
        Text(
            if (stored == null) "NEW ALARM" else "EDIT ALARM",
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall
        )

        if (!exactAlarmsOk) {
            Spacer(Modifier.height(12.dp))
            ExactAlarmBanner()
        }

        // ---- time ----
        SectionTitle("Time", topSpace = SECTION_GAP)
        TimeInput(state = timeState)
        val ringsIn = Times.nextTrigger(current(), now) - now
        if (a.enabled && ringsIn <= Times.LOCK_MS) {
            Text(
                "This rings in under an hour, so it locks the moment you save it.",
                style = MaterialTheme.typography.bodySmall
            )
        } else if (a.enabled) {
            Text(
                "Rings in ${Times.countdown(ringsIn)}. You can edit/delete this alarm until " +
                    "${lockTimeText(Times.lockStartsAt(current(), now))}. After that, it's locked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CheckRow("Alarm on", a.enabled, { a = a.copy(enabled = it) })

        // ---- repeat ----
        SectionTitle("Repeat", topSpace = SECTION_GAP)
        DayRow(a.repeatDays) { a = a.copy(repeatDays = it) }
        Text(Times.daysLabel(a.repeatDays), style = MaterialTheme.typography.bodySmall)

        // ---- sound ----
        SectionTitle("Sound and screen", topSpace = SECTION_GAP)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sound", style = MaterialTheme.typography.labelMedium)
                Text(
                    if (a.audio.isBlank()) "System default" else AssetCatalog.prettyName(a.audio),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            WakeButton("CHANGE", onClick = { sub = Sub.SOUND })
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(
                a.background,
                Modifier.size(width = 84.dp, height = 56.dp).background(androidx.compose.ui.graphics.Color.Black)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground),
                maxPx = 240
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Background", style = MaterialTheme.typography.labelMedium)
                Text(
                    if (a.background.isBlank()) "Plain black" else AssetCatalog.prettyName(a.background),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            WakeButton("CHANGE", onClick = { sub = Sub.BACKGROUND })
        }
        Spacer(Modifier.height(28.dp))
        Text("Volume", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Segmented(
            listOf(30 to "GRADUAL", 90 to "SLOW", 0 to "FULL"),
            selected = a.rampSeconds,
            onSelect = { a = a.copy(rampSeconds = it) }
        )
        Text(
            when (a.rampSeconds) {
                0 -> "Full volume straight away."
                30 -> "Quiet at first, full volume after 30 seconds."
                else -> "Quiet at first, full volume after 90 seconds."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CheckRow("Vibrate", a.vibrate, { a = a.copy(vibrate = it) })

        // ---- missions ----
        SectionTitle("Wake-up challenges", topSpace = SECTION_GAP)
        Text(
            "When this alarm rings, you won't be able to turn it off until you've completed some challenges. " +
                "Below, you can choose the type of challenges you want to do, along with the amount and difficulty.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))

        CheckRow("Math", m.mathEnabled, { v -> setMissions { it.copy(mathEnabled = v) } })
        if (m.mathEnabled) {
            DifficultyPicker(m.mathDifficulty) { d -> setMissions { it.copy(mathDifficulty = d) } }
            CountRow("Problems", m.mathCount, 1, 15) { n -> setMissions { it.copy(mathCount = n) } }
        }
        Spacer(Modifier.height(GROUP_GAP))
        CheckRow("Memory", m.memoryEnabled, { v -> setMissions { it.copy(memoryEnabled = v) } })
        if (m.memoryEnabled) {
            DifficultyPicker(m.memoryDifficulty) { d -> setMissions { it.copy(memoryDifficulty = d) } }
            CountRow("Challenges", m.memoryCount, 1, 8) { n -> setMissions { it.copy(memoryCount = n) } }
        }
        Spacer(Modifier.height(GROUP_GAP))
        CheckRow("Logic", m.logicEnabled, { v -> setMissions { it.copy(logicEnabled = v) } })
        if (m.logicEnabled) {
            DifficultyPicker(m.logicDifficulty) { d -> setMissions { it.copy(logicDifficulty = d) } }
            CountRow("Questions", m.logicCount, 1, 10) { n -> setMissions { it.copy(logicCount = n) } }
        }
        Spacer(Modifier.height(GROUP_GAP))
        CheckRow("Shake the phone", m.shakeEnabled, { v -> setMissions { it.copy(shakeEnabled = v) } })
        if (m.shakeEnabled) {
            CountRow("Shakes", m.shakeCount, 10, 150, step = 5) { n -> setMissions { it.copy(shakeCount = n) } }
        }
        Spacer(Modifier.height(GROUP_GAP))
        CheckRow(
            "Steps / Jumps", m.walkEnabled,
            { v ->
                setMissions { it.copy(walkEnabled = v) }
                // Ask now, in daylight, rather than at 4 AM.
                if (v && Build.VERSION.SDK_INT >= 29) activityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            },
            sub = "Get moving. Take steps or do jumps until you've reached the target."
        )
        if (m.walkEnabled) {
            CountRow("Steps or jumps", m.walkSteps, 10, 500, step = 10) { n -> setMissions { it.copy(walkSteps = n) } }
        }
        Spacer(Modifier.height(GROUP_GAP))
        CheckRow(
            "Photo", m.photoEnabled, { v -> setMissions { it.copy(photoEnabled = v) } },
            sub = "Get out of bed and take a picture somewhere else. The photo isn't saved or checked."
        )
        if (m.photoEnabled) {
            WakeTextField(
                m.photoInstruction,
                { v -> setMissions { it.copy(photoInstruction = v) } },
                label = "Your own instruction (optional)",
                minLines = 2
            )
            Text(
                "Leave blank for a random nudge like \"Go find something in another room.\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(1.5.dp, MaterialTheme.colorScheme.primary)
                .padding(12.dp)
        ) {
            Text("CHALLENGES SELECTED FOR THIS ALARM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (m.anyEnabled) {
                MissionPlan.summary(m).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            } else {
                Text("Nothing yet. Pick at least one challenge.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ---- bedtime ----
        SectionTitle("Bedtime reminders (optional)", topSpace = SECTION_GAP)
        val b = a.bedtime
        fun setBedtime(change: (BedtimeSettings) -> BedtimeSettings) {
            a = a.copy(bedtime = change(b))
        }
        CheckRow("Remind me to go to bed", b.enabled, { v -> setBedtime { it.copy(enabled = v) } })
        if (b.enabled) {
            Spacer(Modifier.height(24.dp))
            Text("Ideal bedtime", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            WakeButton(
                format12(b.hour, b.minute),
                onClick = { showBedtime = true },
                modifier = Modifier.width(140.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "If you wish, you can set some reminders for yourself before bedtime, to help you wind down after the day and get ready for bed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Text("Reminder messages", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(10.dp))
            WakeTextField(b.msg30, { v -> setBedtime { it.copy(msg30 = v) } }, label = "30 minutes before", minLines = 2)
            Spacer(Modifier.height(18.dp))
            WakeTextField(b.msg15, { v -> setBedtime { it.copy(msg15 = v) } }, label = "15 minutes before", minLines = 2)
            Spacer(Modifier.height(18.dp))
            WakeTextField(b.msg0, { v -> setBedtime { it.copy(msg0 = v) } }, label = "At bedtime", minLines = 2)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = {
                setBedtime { it.copy(msg30 = BedtimeSettings.DEFAULT_30, msg15 = BedtimeSettings.DEFAULT_15, msg0 = BedtimeSettings.DEFAULT_0) }
            }) { Text("RESET MESSAGES") }
        }

        // ---- test ----
        SectionTitle("Try it first", topSpace = SECTION_GAP)
        Text(
            "Test alarm plays your sound and screen, with a 15 second snooze. Test missions runs just the challenges. Neither affects your streak.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        WakeButton("TEST ALARM", onClick = { onTestAlarm(current()) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        WakeButton("TEST MISSIONS", onClick = { onTestMissions(current()) }, modifier = Modifier.fillMaxWidth())

        // ---- save / delete ----
        Spacer(Modifier.height(72.dp))
        if (error.isNotEmpty()) {
            Text(error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        WakeButton(
            "SAVE ALARM",
            onClick = {
                val result = current()
                if (!result.missions.anyEnabled) {
                    error = "Pick at least one wake-up challenge."
                } else {
                    // The alarm is always saved. If Android won't let us set it yet, say so before leaving.
                    when (AlarmRepository.save(context, result)) {
                        SaveResult.CREATED -> {
                            createdPending = true
                            if (!ExactAlarms.canSchedule(context)) showPermission = true else showCreated = true
                        }
                        SaveResult.UPDATED -> {
                            createdPending = false
                            if (!ExactAlarms.canSchedule(context)) showPermission = true else onClose()
                        }
                        SaveResult.LOCKED -> error = "This alarm is locked and can't be changed."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            filled = true,
            accent = goodColor()
        )
        if (stored != null) {
            Spacer(Modifier.height(16.dp))
            WakeButton("DELETE ALARM", onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(), accent = badColor())
        }
        Spacer(Modifier.height(48.dp))
    }

    if (showBedtime) {
        // Holds the wheel's latest value until OK is pressed.
        var pickedHour by remember { mutableStateOf(a.bedtime.hour) }
        var pickedMinute by remember { mutableStateOf(a.bedtime.minute) }
        AlertDialog(
            onDismissRequest = { showBedtime = false },
            title = { Text("IDEAL BEDTIME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                WheelTimePicker(a.bedtime.hour, a.bedtime.minute) { h, m -> pickedHour = h; pickedMinute = m }
            },
            confirmButton = {
                TextButton(onClick = {
                    a = a.copy(bedtime = a.bedtime.copy(hour = pickedHour, minute = pickedMinute))
                    showBedtime = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showBedtime = false }) { Text("CANCEL") } }
        )
    }
    if (showPermission) {
        val proceed: () -> Unit = {
            showPermission = false
            if (createdPending) {
                showCreated = true
            } else {
                onClose()
            }
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("ONE MORE STEP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your alarm is saved, but it won't ring yet. Android needs your OK for \"Alarms & reminders\" " +
                        "before an app can set alarms. Tap OPEN SETTINGS, switch it on for No Snooze Alarm, then come back.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { ExactAlarms.openSettings(context); proceed() }) {
                    Text("OPEN SETTINGS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { proceed() }) { Text("LATER") } }
        )
    }
    if (showCreated) {
        WakeDialog(
            "Well done taking the first step. Make sure you head to bed early enough to get sufficient sleep.",
            onOk = { showCreated = false; onClose() }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text("Delete this alarm?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val id = stored?.id
                    if (id != null && !AlarmRepository.delete(context, id)) {
                        error = "This alarm is locked and can't be deleted."
                    } else {
                        onClose()
                    }
                }) { Text("DELETE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("KEEP IT") } }
        )
    }
}

/** "3:00 AM" for the moment an alarm locks. Display only. */
private fun lockTimeText(epochMs: Long): String {
    val t = java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault())
    return format12(t.hour, t.minute)
}

@Composable
private fun CountRow(label: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Stepper(value, onChange, min, max, step)
    }
}

@Composable
private fun DayRow(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        letters.forEachIndexed { i, letter ->
            val day = i + 1
            val on = day in days
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .border(1.5.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable { onChange(if (on) days - day else days + day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letter,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Shown instead of the editor once an alarm is locked. Nothing here can change the alarm. */
@Composable
private fun LockedView(
    alarm: Alarm,
    now: Long,
    onClose: () -> Unit,
    onTestAlarm: (Alarm) -> Unit,
    onTestMissions: (Alarm) -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        BackButton(onClick = onClose)
        Spacer(Modifier.height(16.dp))
        Text(
            Times.formatClock(context, alarm.hour, alarm.minute),
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium
        )
        Text(Times.daysLabel(alarm.repeatDays), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().border(1.5.dp, MaterialTheme.colorScheme.onBackground).padding(12.dp)) {
            Text("LOCKED", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (Times.isWakeUpInProgress(alarm, now)) {
                    "Your wake-up is in progress. This alarm can't be edited or deleted until you're done."
                } else {
                    "This alarm rings in ${Times.countdown(Times.nextTrigger(alarm, now) - now)}. " +
                        "Alarms lock one hour before they ring, so there's no changing your mind at the last minute."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SectionTitle("What you'll face")
        MissionPlan.summary(alarm.missions).forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
        SectionTitle("Sound and screen")
        Text(
            "Sound: " + if (alarm.audio.isBlank()) "System default" else AssetCatalog.prettyName(alarm.audio),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Background: " + if (alarm.background.isBlank()) "Plain black" else AssetCatalog.prettyName(alarm.background),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        WakeButton("TEST ALARM", onClick = { onTestAlarm(alarm) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        WakeButton("TEST MISSIONS", onClick = { onTestMissions(alarm) }, modifier = Modifier.fillMaxWidth())
    }
}
