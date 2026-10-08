package com.wake.alarm.ring

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.wake.alarm.data.Alarm

object RingLauncher {
    /**
     * Try to put the wake-up screen up directly, in addition to the full-screen-intent notification.
     * Android only allows this from the background when "Display over other apps" is granted (or on
     * Android 9 and below); otherwise it is silently refused and the notification does the job.
     */
    fun openScreen(context: Context, alarm: Alarm, test: Boolean, json: String?) {
        try {
            context.startActivity(RingActivity.intent(context, alarm.id, test, json))
        } catch (_: Exception) {
        }
    }

    /**
     * Start the ringing foreground service. If Android refuses (for example, some
     * versions block media foreground services started from BOOT_COMPLETED), fall back
     * to a loud full-screen-intent notification; opening it starts the service from a
     * visible activity, which is always allowed.
     */
    @SuppressLint("MissingPermission")
    fun launch(context: Context, alarm: Alarm, test: Boolean, json: String?) {
        RingNotifications.ensureChannels(context)
        try {
            RingService.start(context, alarm.id, test, json)
        } catch (e: Exception) {
            try {
                NotificationManagerCompat.from(context).notify(
                    RingNotifications.ID_FALLBACK,
                    RingNotifications.ring(context, alarm, test, json, RingNotifications.CH_FALLBACK)
                )
            } catch (_: SecurityException) {
            }
        }
    }
}
