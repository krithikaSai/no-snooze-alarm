package com.wake.alarm.schedule

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Android 12+ requires the exact-alarm permission even for setAlarmClock().
 *
 *  - Android 13+ : USE_EXACT_ALARM (declared in the manifest) is granted automatically to alarm-clock apps.
 *  - Android 12  : SCHEDULE_EXACT_ALARM is granted by default, but the user can switch it off under
 *                  "Alarms & reminders".
 *
 * Everything that schedules an alarm asks [canSchedule] first and never throws when it's missing.
 */
object ExactAlarms {

    fun canSchedule(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    /** Opens the "Alarms & reminders" page for this app, or app details if that page isn't available. */
    fun openSettings(context: Context): Boolean {
        val pkg = Uri.parse("package:${context.packageName}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (e: Exception) {
                // fall through to app details
            }
        }
        return try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
