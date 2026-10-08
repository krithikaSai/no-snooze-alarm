package com.wake.alarm.data

import android.content.Context
import com.wake.alarm.ring.RingLauncher
import com.wake.alarm.schedule.AlarmScheduler
import com.wake.alarm.util.Times
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class SaveResult { CREATED, UPDATED, LOCKED }

/**
 * Every change to alarms goes through here. The one-hour lock is enforced in this
 * class (against the STORED alarm), so no UI path can bypass it.
 */
object AlarmRepository {

    fun save(context: Context, draft: Alarm): SaveResult {
        var result = SaveResult.UPDATED
        var savedId = draft.id
        WakeStore.mutate { data ->
            val existing = data.alarms.firstOrNull { it.id == draft.id }
            when {
                existing != null && Times.isLocked(existing) -> {
                    result = SaveResult.LOCKED
                    data
                }
                existing == null -> {
                    result = SaveResult.CREATED
                    savedId = data.nextId
                    val created = draft.copy(
                        id = savedId, scheduledAt = 0L, activeSince = 0L, snoozeUsed = false, snoozeAt = 0L
                    )
                    data.copy(alarms = data.alarms + created, nextId = savedId + 1)
                }
                else -> {
                    val updated = draft.copy(
                        id = existing.id,
                        activeSince = existing.activeSince,
                        snoozeUsed = existing.snoozeUsed,
                        snoozeAt = existing.snoozeAt
                    )
                    data.copy(alarms = data.alarms.map { if (it.id == existing.id) updated else it })
                }
            }
        }
        if (result != SaveResult.LOCKED) WakeStore.get(savedId)?.let { AlarmScheduler.schedule(context, it) }
        return result
    }

    /** Returns false (and changes nothing) if the alarm is locked. */
    fun delete(context: Context, id: Int): Boolean {
        val existing = WakeStore.get(id) ?: return true
        if (Times.isLocked(existing)) return false
        WakeStore.mutate { it.copy(alarms = it.alarms.filterNot { a -> a.id == id }) }
        AlarmScheduler.cancelAll(context, id)
        return true
    }

    /** Returns false (and changes nothing) if the alarm is locked. */
    fun setEnabled(context: Context, id: Int, enabled: Boolean): Boolean {
        val existing = WakeStore.get(id) ?: return false
        if (Times.isLocked(existing)) return false
        WakeStore.mutate { d -> d.copy(alarms = d.alarms.map { if (it.id == id) it.copy(enabled = enabled) else it }) }
        WakeStore.get(id)?.let { AlarmScheduler.schedule(context, it) }
        return true
    }

    /**
     * Called when an alarm rings. [resume] = true for a snooze ringing again or a
     * wake-up resumed after reboot (keeps the "snooze used" state).
     */
    fun onFired(context: Context, id: Int, resume: Boolean): Alarm? {
        val now = System.currentTimeMillis()
        var result: Alarm? = null
        WakeStore.mutate { d ->
            val a = d.alarms.firstOrNull { it.id == id } ?: return@mutate d
            val n = if (resume) {
                a.copy(activeSince = if (a.activeSince > 0) a.activeSince else now, snoozeAt = 0L)
            } else {
                a.copy(
                    activeSince = now,
                    snoozeUsed = false,
                    snoozeAt = 0L,
                    // One-time alarms switch themselves off after ringing.
                    enabled = if (a.repeatDays.isEmpty()) false else a.enabled
                )
            }
            result = n
            d.copy(alarms = d.alarms.map { if (it.id == id) n else it })
        }
        if (!resume) result?.let { AlarmScheduler.schedule(context, it) }
        return result
    }

    fun markSnoozed(context: Context, id: Int) {
        val at = System.currentTimeMillis() + Times.SNOOZE_MS
        WakeStore.mutate { d ->
            d.copy(alarms = d.alarms.map { if (it.id == id) it.copy(snoozeUsed = true, snoozeAt = at) else it })
        }
        AlarmScheduler.scheduleSnooze(context, id, at, test = false, json = null)
    }

    /**
     * The configured missions are done: add today to the history (once) without ending the wake-up,
     * so the user can still answer "wide awake?" and do extra missions on the same screen.
     */
    fun recordSuccess(id: Int) {
        val a = WakeStore.get(id) ?: return
        val started = if (a.activeSince > 0) a.activeSince else System.currentTimeMillis()
        val date = Instant.ofEpochMilli(started).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val entry = HistoryEntry(date, id, "%02d:%02d".format(a.hour, a.minute))
        WakeStore.mutate { d ->
            if (d.history.any { it.date == date && it.alarmId == id }) d else d.copy(history = d.history + entry)
        }
    }

    /** The user finished the wake-up (pressed "I'm wide awake"). */
    fun complete(context: Context, id: Int) {
        val a = WakeStore.get(id) ?: return
        val started = if (a.activeSince > 0) a.activeSince else System.currentTimeMillis()
        val date = Instant.ofEpochMilli(started).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val entry = HistoryEntry(date, id, "%02d:%02d".format(a.hour, a.minute))
        WakeStore.mutate { d ->
            val history = if (d.history.any { it.date == date && it.alarmId == id }) d.history else d.history + entry
            d.copy(
                history = history,
                alarms = d.alarms.map { if (it.id == id) it.copy(activeSince = 0L, snoozeUsed = false, snoozeAt = 0L) else it }
            )
        }
        AlarmScheduler.cancelSnooze(context, id)
        WakeStore.get(id)?.let { AlarmScheduler.schedule(context, it) }
    }

    fun rescheduleAll(context: Context) {
        WakeStore.alarms().forEach { AlarmScheduler.schedule(context, it) }
    }

    /**
     * After reboot / app update / clock change. On reboot also rings anything that
     * was missed while the phone was off (within the missed window) or was ringing
     * when the phone went down.
     */
    fun restore(context: Context, boot: Boolean) {
        val now = System.currentTimeMillis()
        val toRing = mutableListOf<Pair<Int, Boolean>>() // id to resume?
        if (boot) {
            for (a in WakeStore.alarms()) {
                when {
                    a.snoozeAt > 0 -> {
                        if (a.snoozeAt > now) {
                            AlarmScheduler.scheduleSnooze(context, a.id, a.snoozeAt, test = false, json = null)
                        } else if (now - a.snoozeAt < Times.MISSED_WINDOW_MS) {
                            toRing += a.id to true
                        }
                    }
                    Times.isWakeUpInProgress(a, now) -> toRing += a.id to true
                    a.enabled && a.scheduledAt in 1..now &&
                        now - a.scheduledAt < Times.MISSED_WINDOW_MS && a.activeSince < a.scheduledAt ->
                        toRing += a.id to false
                }
            }
        }
        rescheduleAll(context)
        for ((id, resume) in toRing) {
            onFired(context, id, resume)?.let { RingLauncher.launch(context, it, test = false, json = null) }
        }
    }

    fun today(): LocalDate = LocalDate.now()
}
