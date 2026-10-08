package com.wake.alarm.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.wake.alarm.data.Alarm

object RingNotifications {
    /** Silent: the service plays the sound itself. */
    const val CH_ALARM = "alarm_ring"
    /** Used only if the service could not start (e.g. blocked after reboot): this one makes its own noise. */
    const val CH_FALLBACK = "alarm_fallback"
    const val CH_BEDTIME = "bedtime"
    const val ID_FALLBACK = 4343

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val ring = NotificationChannel(CH_ALARM, "Alarm ringing", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while an alarm is ringing."
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val fallback = NotificationChannel(CH_FALLBACK, "Alarm (backup)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Backup alarm notification if the ringing service is blocked."
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val bedtime = NotificationChannel(CH_BEDTIME, "Bedtime reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Gentle reminders to get to bed."
        }
        nm.createNotificationChannels(listOf(ring, fallback, bedtime))
    }

    private fun openRing(context: Context, alarm: Alarm, test: Boolean, json: String?): PendingIntent =
        PendingIntent.getActivity(
            context, 7001, RingActivity.intent(context, alarm.id, test, json),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun ring(context: Context, alarm: Alarm, test: Boolean, json: String?, channel: String = CH_ALARM): Notification {
        val pi = openRing(context, alarm, test, json)
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (test) "TEST ALARM" else "WAKE UP!!")
            .setContentText("Tap to start your wake-up challenge")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            // Android 12+ may delay a foreground-service notification by up to 10 seconds: not for an alarm.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
    }

    /** Shown only to satisfy startForeground() when the service has nothing to ring. */
    fun placeholder(context: Context): Notification =
        NotificationCompat.Builder(context, CH_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm")
            .build()
}
