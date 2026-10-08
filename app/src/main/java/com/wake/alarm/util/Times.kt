package com.wake.alarm.util

import android.content.Context
import android.text.format.DateFormat
import com.wake.alarm.data.Alarm
import java.time.Instant
import java.time.ZoneId

object Times {
    const val LOCK_MS = 60 * 60 * 1000L
    const val SNOOZE_MS = 15 * 60 * 1000L
    const val TEST_SNOOZE_MS = 15 * 1000L

    /** A wake-up that started more than this long ago is considered abandoned. */
    const val ACTIVE_WINDOW_MS = 6 * 60 * 60 * 1000L

    /** After a reboot, an alarm missed by less than this still rings immediately. */
    const val MISSED_WINDOW_MS = 30 * 60 * 1000L

    /** The next time (strictly after [after]) this alarm should ring. */
    fun nextTrigger(alarm: Alarm, after: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val from = Instant.ofEpochMilli(after).atZone(zone)
        for (i in 0..8) {
            val date = from.toLocalDate().plusDays(i.toLong())
            val candidate = date.atTime(alarm.hour, alarm.minute).atZone(zone)
            if (!candidate.toInstant().isAfter(from.toInstant())) continue
            if (alarm.repeatDays.isNotEmpty() && date.dayOfWeek.value !in alarm.repeatDays) continue
            return candidate.toInstant().toEpochMilli()
        }
        return after + 24 * 60 * 60 * 1000L
    }

    fun isWakeUpInProgress(alarm: Alarm, now: Long = System.currentTimeMillis()): Boolean =
        alarm.activeSince > 0 && now - alarm.activeSince < ACTIVE_WINDOW_MS

    /**
     * Locked = exactly one hour (or less) before it rings, or a wake-up is in progress.
     * Disabled alarms are never locked: they will not ring, so there is nothing to protect.
     */
    fun isLocked(alarm: Alarm, now: Long = System.currentTimeMillis()): Boolean {
        if (isWakeUpInProgress(alarm, now)) return true
        if (!alarm.enabled) return false
        return nextTrigger(alarm, now) - now <= LOCK_MS
    }

    /** When the lock starts for the next ring. */
    fun lockStartsAt(alarm: Alarm, now: Long = System.currentTimeMillis()): Long =
        nextTrigger(alarm, now) - LOCK_MS

    fun formatClock(context: Context, hour: Int, minute: Int): String {
        return if (DateFormat.is24HourFormat(context)) {
            "%02d:%02d".format(hour, minute)
        } else {
            val h = if (hour % 12 == 0) 12 else hour % 12
            "%d:%02d %s".format(h, minute, if (hour < 12) "AM" else "PM")
        }
    }

    fun formatMillisClock(context: Context, millis: Long): String {
        val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return formatClock(context, t.hour, t.minute)
    }

    fun daysLabel(days: Set<Int>): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days == setOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (days == setOf(6, 7)) return "Weekends"
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return days.sorted().joinToString(" ") { names[it - 1] }
    }

    fun countdown(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
        val d = totalMinutes / (60 * 24)
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }
}
