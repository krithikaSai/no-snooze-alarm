package com.wake.alarm

import android.app.Application
import com.wake.alarm.data.WakeStore
import com.wake.alarm.ring.RingNotifications

class WakeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WakeStore.init(this)
        RingNotifications.ensureChannels(this)
        // Deliberately no rescheduling here: the missed-alarm check after reboot must see the
        // original schedule. Rescheduling happens in BootReceiver and when the app is opened.
    }
}
