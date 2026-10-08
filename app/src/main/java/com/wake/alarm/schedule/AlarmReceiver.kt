package com.wake.alarm.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wake.alarm.data.AlarmRepository
import com.wake.alarm.data.WakeStore
import com.wake.alarm.ring.RingLauncher
import kotlinx.serialization.decodeFromString

/** Receives the system alarm broadcast (also when the app is closed) and starts the ringing service. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WakeStore.init(context)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val resume = intent.getBooleanExtra(AlarmScheduler.EXTRA_RESUME, false)
        val test = intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)
        val json = intent.getStringExtra(AlarmScheduler.EXTRA_JSON)

        if (test) {
            val alarm = json?.let { runCatching { WakeStore.json.decodeFromString<com.wake.alarm.data.Alarm>(it) }.getOrNull() }
                ?: return
            RingLauncher.launch(context, alarm, test = true, json = json)
            return
        }
        val alarm = AlarmRepository.onFired(context, id, resume) ?: return
        RingLauncher.launch(context, alarm, test = false, json = null)
    }
}
