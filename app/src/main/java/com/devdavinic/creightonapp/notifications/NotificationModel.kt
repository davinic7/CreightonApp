package com.devdavinic.creightonapp.notifications

// =============================================================================
// NOTIFICATION MODEL
// All notification types and user settings
// =============================================================================

enum class NotificationType(
    val channelId: String,
    val channelName: String,
    val requestCode: Int
) {
    DAILY_REGISTER(
        channelId   = "daily_register",
        channelName = "Recordatorio de registro diario",
        requestCode = 1001
    ),
    BREAST_EXAM_DAY7(
        channelId   = "breast_exam",
        channelName = "Autoexamen mamario",
        requestCode = 1002
    ),
    PEAK_DAY_SUGGESTION(
        channelId   = "peak_day",
        channelName = "Sugerencia de Dia Pico",
        requestCode = 1003
    ),
    FERTILE_PHASE_START(
        channelId   = "fertile_phase",
        channelName = "Inicio de fase fertil",
        requestCode = 1004
    ),
    DOUBLE_PEAK_QUESTION(
        channelId   = "double_peak",
        channelName = "Preguntas del Doble Pico",
        requestCode = 1005
    ),
    PARTNER_FERTILE_ALERT(
        channelId   = "partner_alert",
        channelName = "Alerta para la pareja",
        requestCode = 1006
    )
}

data class NotificationSettings(
    val dailyRegisterEnabled: Boolean = true,
    val dailyRegisterHour: Int = 21,        // 9pm default
    val dailyRegisterMinute: Int = 0,

    val breastExamEnabled: Boolean = true,

    val peakDaySuggestionEnabled: Boolean = true,

    val fertilePhaseEnabled: Boolean = true,

    val doublePeakEnabled: Boolean = true,

    val partnerAlertsEnabled: Boolean = true
)