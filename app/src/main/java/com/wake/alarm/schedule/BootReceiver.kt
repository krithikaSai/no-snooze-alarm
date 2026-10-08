package com.wake.alarm.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wake.alarm.data.AlarmRepository
import com.wake.alarm.data.WakeStore

/** AlarmManager forgets everything on reboot; this puts all alarms back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WakeStore.init(context)
        AlarmRepository.restore(context, boot = intent.action == Intent.ACTION_BOOT_COMPLETED)
    }
}
