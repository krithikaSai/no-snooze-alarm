package com.wake.alarm.ring

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Everything that decides whether the wake-up screen can actually appear.
 * The sound comes from a foreground service and always works; the SCREEN depends on these.
 *
 *  - Notifications (Android 13+ runtime permission): the screen is launched by a full-screen-intent
 *    notification. If notifications are off, nothing is shown and only the sound plays.
 *  - Full-screen intents (Android 14+): a separate switch Android can turn off for apps.
 *  - Display over other apps (optional): the only way to open the screen while the phone is in use
 *    and unlocked. Without it, an unlocked phone shows a heads-up banner to tap instead.
 */
object Readiness {

    fun notificationsOk(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = nm.getNotificationChannel(RingNotifications.CH_ALARM)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun fullScreenOk(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.canUseFullScreenIntent()
    }

    fun overlayOk(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openNotificationSettings(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            start(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg(context)))
        } else {
            openNotificationSettings(context)
        }
    }

    fun openOverlaySettings(context: Context) {
        start(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg(context)))
    }

    private fun pkg(context: Context): Uri = Uri.parse("package:${context.packageName}")

    private fun start(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Some phones don't have the dedicated page: fall back to this app's details screen.
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg(context)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
