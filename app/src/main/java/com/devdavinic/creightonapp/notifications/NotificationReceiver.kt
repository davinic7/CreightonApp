package com.devdavinic.creightonapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.devdavinic.creightonapp.MainActivity
import com.devdavinic.creightonapp.R

// =============================================================================
// NOTIFICATION RECEIVER
// Fired by AlarmManager — creates and shows the notification
// =============================================================================

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: return
        val type    = runCatching { NotificationType.valueOf(typeStr) }.getOrNull() ?: return

        val (title, body) = when (type) {
            NotificationType.DAILY_REGISTER ->
                "Registro del dia" to
                        "Es el momento de registrar las observaciones de hoy en tu ciclo Creighton."

            NotificationType.BREAST_EXAM_DAY7 ->
                "Autoexamen mamario — Dia 7" to
                        "El manual recomienda realizar el autoexamen mamario hoy, dia 7 de tu ciclo."

            NotificationType.PEAK_DAY_SUGGESTION ->
                "Posible Dia Pico detectado" to
                        "El sistema detecto que ayer podria ser tu Dia Pico. Abre la app para confirmar."

            NotificationType.FERTILE_PHASE_START ->
                "Inicio de fase fertil" to
                        "Tu ciclo de moco ha comenzado. Recuerda observar con atencion durante el dia."

            NotificationType.DOUBLE_PEAK_QUESTION ->
                "Preguntas del Doble Pico — P+3" to
                        "Hoy es el dia 3 post-Pico. Responde las preguntas del Doble Pico juntos."

            NotificationType.PARTNER_FERTILE_ALERT ->
                "Alerta de ciclo — Pareja" to
                        "El ciclo de tu pareja entro en fase fertil. Abre la app para ver el estado."
        }

        createChannelIfNeeded(context, type)
        showNotification(context, type, title, body)

        // Reschedule daily notification for the next day
        if (type == NotificationType.DAILY_REGISTER) {
            val prefs = context.getSharedPreferences("notif_settings", Context.MODE_PRIVATE)
            val hour   = prefs.getInt("daily_hour", 21)
            val minute = prefs.getInt("daily_minute", 0)
            NotificationScheduler.scheduleDailyRegister(context, hour, minute)
        }
    }

    private fun createChannelIfNeeded(context: Context, type: NotificationType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(type.channelId) == null) {
                val channel = NotificationChannel(
                    type.channelId,
                    type.channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones de CreightonApp — ${type.channelName}"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun showNotification(
        context: Context, type: NotificationType,
        title: String, body: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, type.requestCode, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(type.requestCode, notification)
    }

    companion object {
        const val EXTRA_TYPE = "notification_type"
    }
}