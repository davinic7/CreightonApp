package com.devdavinic.creightonapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Reschedules all alarms after device reboot
// (AlarmManager alarms are lost on reboot)
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.restoreOnBoot(context)
        }
    }
}