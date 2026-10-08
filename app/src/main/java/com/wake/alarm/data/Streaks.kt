package com.wake.alarm.data

import java.time.LocalDate

object Streaks {
    /**
     * Consecutive scheduled days with a completed wake-up, ending today (or yesterday,
     * if today's wake-up has not happened yet). Days the alarm is not scheduled to
     * ring on are skipped rather than breaking the streak.
     */
    fun forAlarm(alarm: Alarm, history: List<HistoryEntry>, today: LocalDate = LocalDate.now()): Int {
        val done = history.filter { it.alarmId == alarm.id }
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSet()
        if (done.isEmpty()) return 0

        fun scheduled(d: LocalDate) = alarm.repeatDays.isEmpty() || d.dayOfWeek.value in alarm.repeatDays

        var day = today
        if (day !in done) day = day.minusDays(1)
        var count = 0
        var guard = 0
        while (guard++ < 3650) {
            if (!scheduled(day)) {
                day = day.minusDays(1)
                continue
            }
            if (day in done) {
                count++
                day = day.minusDays(1)
            } else break
        }
        return count
    }

    fun successDates(history: List<HistoryEntry>): Set<LocalDate> =
        history.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
}
