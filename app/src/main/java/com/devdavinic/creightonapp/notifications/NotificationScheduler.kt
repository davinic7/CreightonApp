package com.devdavinic.creightonapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

// =============================================================================
// NOTIFICATION SCHEDULER
// Uses AlarmManager to schedule exact-time notifications
// =============================================================================

object NotificationScheduler {

    // -------------------------------------------------------------------------
    // DAILY REGISTER REMINDER
    // -------------------------------------------------------------------------

    fun scheduleDailyRegister(context: Context, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Save settings for rescheduling after boot/fire
        context.getSharedPreferences("notif_settings", Context.MODE_PRIVATE).edit()
            .putInt("daily_hour", hour)
            .putInt("daily_minute", minute)
            .putBoolean("daily_enabled", true)
            .apply()

        scheduleAlarm(context, NotificationType.DAILY_REGISTER, calendar.timeInMillis)
    }

    fun cancelDailyRegister(context: Context) {
        context.getSharedPreferences("notif_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("daily_enabled", false).apply()
        cancelAlarm(context, NotificationType.DAILY_REGISTER)
    }

    // -------------------------------------------------------------------------
    // BREAST EXAM DAY 7 — fired by app logic when cycle day == 7
    // -------------------------------------------------------------------------

    fun scheduleBreastExamToday(context: Context) {
        // Fire in 1 hour from now as a reminder
        val triggerMs = System.currentTimeMillis() + 60 * 60 * 1000L
        scheduleAlarm(context, NotificationType.BREAST_EXAM_DAY7, triggerMs, oneShot = true)
    }

    // -------------------------------------------------------------------------
    // PEAK DAY SUGGESTION — fired immediately when system detects it
    // -------------------------------------------------------------------------

    fun notifyPeakDaySuggestion(context: Context) {
        scheduleAlarm(context, NotificationType.PEAK_DAY_SUGGESTION,
            System.currentTimeMillis() + 5_000L, oneShot = true)
    }

    // -------------------------------------------------------------------------
    // FERTILE PHASE START — fired immediately when mucus first appears
    // -------------------------------------------------------------------------

    fun notifyFertilePhaseStart(context: Context) {
        scheduleAlarm(context, NotificationType.FERTILE_PHASE_START,
            System.currentTimeMillis() + 5_000L, oneShot = true)
    }

    // -------------------------------------------------------------------------
    // DOUBLE PEAK QUESTION — fired on P+3 day at the daily reminder time
    // -------------------------------------------------------------------------

    fun scheduleDoublePeakQuestion(context: Context) {
        val prefs  = context.getSharedPreferences("notif_settings", Context.MODE_PRIVATE)
        val hour   = prefs.getInt("daily_hour", 21)
        val minute = prefs.getInt("daily_minute", 0)
        val cal    = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        scheduleAlarm(context, NotificationType.DOUBLE_PEAK_QUESTION,
            cal.timeInMillis, oneShot = true)
    }

    // -------------------------------------------------------------------------
    // PARTNER ALERT — fired when fertile phase starts
    // -------------------------------------------------------------------------

    fun notifyPartner(context: Context) {
        scheduleAlarm(context, NotificationType.PARTNER_FERTILE_ALERT,
            System.currentTimeMillis() + 5_000L, oneShot = true)
    }

    // -------------------------------------------------------------------------
    // RESTORE ON BOOT
    // -------------------------------------------------------------------------

    fun restoreOnBoot(context: Context) {
        val prefs = context.getSharedPreferences("notif_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("daily_enabled", false)) {
            val hour   = prefs.getInt("daily_hour", 21)
            val minute = prefs.getInt("daily_minute", 0)
            scheduleDailyRegister(context, hour, minute)
        }
    }

    // -------------------------------------------------------------------------
    // CORE ALARM HELPERS
    // -------------------------------------------------------------------------

    private fun scheduleAlarm(
        context: Context,
        type: NotificationType,
        triggerAtMs: Long,
        oneShot: Boolean = false
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TYPE, type.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, type.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact if permission not granted
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent
        )
    }

    private fun cancelAlarm(context: Context, type: NotificationType) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, type.requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
        }
    }
}