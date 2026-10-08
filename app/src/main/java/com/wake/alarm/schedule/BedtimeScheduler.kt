package com.wake.alarm.schedule

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wake.alarm.MainActivity
import com.wake.alarm.data.WakeStore
import com.wake.alarm.data.Alarm
import com.wake.alarm.ring.RingNotifications
import com.wake.alarm.util.Times
import java.time.Instant
import java.time.ZoneId

/**
 * Optional bedtime reminders: 30 min before, 15 min before, and at bedtime.
 * Reminder times are derived from the alarm's next ring, so they always point at
 * the evening before the alarm.
 *
 * Each reminder is set as an exact, Doze-piercing alarm whenever the exact-alarm permission is available
 * (it is for this app: USE_EXACT_ALARM). Inexact alarms set hours ahead may be held back by Android for hours
 * and delivered in one batch, which made all three reminders arrive late and together. Without the
 * permission it falls back to the old inexact alarm, so nothing here can throw.
 */
object BedtimeScheduler {
    private const val EXTRA_ID = "alarm_id"
    private const val EXTRA_STEP = "step"
    private val offsetsMinutes = listOf(30L, 15L, 0L)
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun intent(context: Context, id: Int, step: Int) =
        Intent(context, BedtimeReceiver::class.java).apply {
            action = "com.wake.alarm.BEDTIME.$step"
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_STEP, step)
        }

    fun cancel(context: Context, id: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (step in offsetsMinutes.indices) {
            val pi = PendingIntent.getBroadcast(
                context, id * 10 + 2 + step, intent(context, id, step),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun schedule(context: Context, alarm: Alarm) {
        cancel(context, alarm.id)
        if (!alarm.enabled || !alarm.bedtime.enabled) return
        val am = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val exact = ExactAlarms.canSchedule(context)
        val zone = ZoneId.systemDefault()
        val ring = Instant.ofEpochMilli(Times.nextTrigger(alarm, now)).atZone(zone)
        var bed = ring.toLocalDate().atTime(alarm.bedtime.hour, alarm.bedtime.minute).atZone(zone)
        if (!bed.isBefore(ring)) bed = bed.minusDays(1)
        offsetsMinutes.forEachIndexed { step, off ->
            val at = bed.minusMinutes(off).toInstant().toEpochMilli()
            if (at > now) {
                val pi = PendingIntent.getBroadcast(context, alarm.id * 10 + 2 + step, intent(context, alarm.id, step), FLAGS)
                try {
                    if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } catch (e: SecurityException) {
                    // Permission revoked between the check and the call.
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            }
        }
    }

    fun extraId(i: Intent) = i.getIntExtra(EXTRA_ID, -1)
    fun extraStep(i: Intent) = i.getIntExtra(EXTRA_STEP, 0)
}

class BedtimeReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        WakeStore.init(context)
        val id = BedtimeScheduler.extraId(intent)
        val step = BedtimeScheduler.extraStep(intent)
        val alarm = WakeStore.get(id) ?: return
        if (!alarm.enabled || !alarm.bedtime.enabled) return
        val (title, text) = when (step) {
            0 -> "Wind down" to alarm.bedtime.msg30
            1 -> "Almost bedtime" to alarm.bedtime.msg15
            else -> "Good night" to alarm.bedtime.msg0
        }
        RingNotifications.ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 6000, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, RingNotifications.CH_BEDTIME)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(7000 + id * 10 + step, n)
        } catch (_: SecurityException) {
        }
    }
}
