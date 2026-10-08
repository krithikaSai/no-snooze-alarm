package com.wake.alarm.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wake.alarm.MainActivity
import com.wake.alarm.data.Alarm
import com.wake.alarm.data.WakeStore
import com.wake.alarm.util.Times

/**
 * Uses AlarmManager.setAlarmClock(): the most reliable alarm API on Android.
 * It is exempt from Doze and battery restrictions, shows the system alarm icon,
 * and needs no special permission.
 *
 * Request codes per alarm id N:  N*10+0 main ring, N*10+1 snooze, N*10+5 "show app" intent.
 * (Bedtime reminders use N*10+2..4, see BedtimeScheduler.) Id 0 is reserved for test alarms.
 */
object AlarmScheduler {
    const val ACTION_FIRE = "com.wake.alarm.FIRE"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_RESUME = "resume"
    const val EXTRA_TEST = "test"
    const val EXTRA_JSON = "alarm_json"

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun manager(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun fireIntent(
        context: Context, id: Int, resume: Boolean, test: Boolean, json: String?
    ): Intent = Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_FIRE
        putExtra(EXTRA_ALARM_ID, id)
        putExtra(EXTRA_RESUME, resume)
        putExtra(EXTRA_TEST, test)
        if (json != null) putExtra(EXTRA_JSON, json)
    }

    private fun firePending(
        context: Context, id: Int, resume: Boolean, test: Boolean, json: String?
    ): PendingIntent = PendingIntent.getBroadcast(
        context, id * 10 + (if (resume) 1 else 0), fireIntent(context, id, resume, test, json), FLAGS
    )

    private fun showPending(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        context, id * 10 + 5, Intent(context, MainActivity::class.java), FLAGS
    )

    /**
     * (Re)schedule the next ring and bedtime reminders for one alarm.
     * Returns true if the alarm is set with the system (or doesn't need to be because it's off).
     * Returns false, without throwing, when the exact-alarm permission is missing: nothing is
     * scheduled until it is granted, and the UI tells the user (see ExactAlarms).
     */
    fun schedule(context: Context, alarm: Alarm): Boolean {
        cancelMain(context, alarm.id)
        if (!alarm.enabled) {
            setScheduledAt(alarm.id, 0L)
            BedtimeScheduler.schedule(context, alarm)
            return true
        }
        if (!ExactAlarms.canSchedule(context)) return notScheduled(context, alarm)

        val at = Times.nextTrigger(alarm)
        val info = AlarmManager.AlarmClockInfo(at, showPending(context, alarm.id))
        try {
            manager(context).setAlarmClock(info, firePending(context, alarm.id, false, false, null))
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call.
            return notScheduled(context, alarm)
        }
        setScheduledAt(alarm.id, at)
        BedtimeScheduler.schedule(context, alarm.copy(scheduledAt = at))
        return true
    }

    private fun notScheduled(context: Context, alarm: Alarm): Boolean {
        setScheduledAt(alarm.id, 0L)
        // Bedtime reminders are inexact alarms and need no special permission.
        BedtimeScheduler.schedule(context, alarm)
        return false
    }

    fun scheduleSnooze(context: Context, id: Int, at: Long, test: Boolean, json: String?) {
        val pending = firePending(context, id, true, test, json)
        if (ExactAlarms.canSchedule(context)) {
            try {
                val info = AlarmManager.AlarmClockInfo(at, showPending(context, id))
                manager(context).setAlarmClock(info, pending)
                return
            } catch (e: SecurityException) {
                // fall through to the inexact alarm
            }
        }
        // Without the permission an inexact (but Doze-piercing) alarm still beats no snooze at all.
        manager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun cancelMain(context: Context, id: Int) {
        val pi = PendingIntent.getBroadcast(
            context, id * 10, fireIntent(context, id, false, false, null),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            manager(context).cancel(pi)
            pi.cancel()
        }
    }

    fun cancelSnooze(context: Context, id: Int) {
        val pi = PendingIntent.getBroadcast(
            context, id * 10 + 1, fireIntent(context, id, true, false, null),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            manager(context).cancel(pi)
            pi.cancel()
        }
    }

    fun cancelAll(context: Context, id: Int) {
        cancelMain(context, id)
        cancelSnooze(context, id)
        BedtimeScheduler.cancel(context, id)
    }

    private fun setScheduledAt(id: Int, at: Long) {
        WakeStore.mutate { d ->
            d.copy(alarms = d.alarms.map { if (it.id == id) it.copy(scheduledAt = at) else it })
        }
    }
}
