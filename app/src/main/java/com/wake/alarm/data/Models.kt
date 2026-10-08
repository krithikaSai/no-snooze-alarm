package com.wake.alarm.data

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty { EASY, MEDIUM, HARD }

@Serializable
data class MissionSettings(
    val mathEnabled: Boolean = true,
    val mathDifficulty: Difficulty = Difficulty.EASY,
    val mathCount: Int = 3,
    val memoryEnabled: Boolean = false,
    val memoryDifficulty: Difficulty = Difficulty.EASY,
    val memoryCount: Int = 1,
    val logicEnabled: Boolean = false,
    val logicDifficulty: Difficulty = Difficulty.EASY,
    val logicCount: Int = 2,
    val walkEnabled: Boolean = false,
    val walkSteps: Int = 30,
    val shakeEnabled: Boolean = false,
    val shakeCount: Int = 30,
    val photoEnabled: Boolean = false,
    /** Blank means "pick a random encouraging nudge". */
    val photoInstruction: String = ""
) {
    val anyEnabled: Boolean
        get() = mathEnabled || memoryEnabled || logicEnabled || walkEnabled || shakeEnabled || photoEnabled
}

@Serializable
data class BedtimeSettings(
    val enabled: Boolean = false,
    val hour: Int = 22,
    val minute: Int = 0,
    val msg30: String = DEFAULT_30,
    val msg15: String = DEFAULT_15,
    val msg0: String = DEFAULT_0
) {
    companion object {
        const val DEFAULT_30 =
            "Time to start winding down. Try to put the phone away when you can, and maybe pick up a book."
        const val DEFAULT_15 =
            "Getting closer now. Time to brush your teeth, wash your face, and start getting ready for bed."
        const val DEFAULT_0 =
            "Good night. Try not to pick up the phone again. Get some good sleep."
    }
}

@Serializable
data class Alarm(
    val id: Int = 0,
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    /** java.time.DayOfWeek values: 1 = Monday ... 7 = Sunday. Empty = one-time alarm. */
    val repeatDays: Set<Int> = emptySet(),
    /** Asset path like "audio/classic/bell.mp3". Blank = system default alarm tone. */
    val audio: String = "",
    /** Asset path like "backgrounds/cat.jpg". Blank = plain black. */
    val background: String = "",
    /** Seconds to ramp volume from quiet to full. 0 = full volume immediately. */
    val rampSeconds: Int = 30,
    val vibrate: Boolean = true,
    val missions: MissionSettings = MissionSettings(),
    val bedtime: BedtimeSettings = BedtimeSettings(),

    // ---- runtime state (managed by the app, not by the editor) ----
    /** Epoch millis of the next scheduled ring (0 = not scheduled). Used to detect missed alarms after reboot. */
    val scheduledAt: Long = 0L,
    /** Epoch millis when the current wake-up started (0 = no wake-up in progress). */
    val activeSince: Long = 0L,
    /** The single snooze of the current wake-up has been used. */
    val snoozeUsed: Boolean = false,
    /** Epoch millis when a pending snooze will ring (0 = none). */
    val snoozeAt: Long = 0L
)

@Serializable
data class HistoryEntry(
    /** ISO date (yyyy-MM-dd) of the day the wake-up started. */
    val date: String,
    val alarmId: Int,
    /** "HH:mm" of the alarm at that time. */
    val time: String
)

@Serializable
data class AppData(
    val alarms: List<Alarm> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val nextId: Int = 1,
    /** Set once the first-launch welcome screen has been dismissed. */
    val welcomeDone: Boolean = false
)
