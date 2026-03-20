package com.devdavinic.creightonapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// APP SKIN SYSTEM
// 4 interchangeable visual themes
// =============================================================================

enum class AppSkin(
    val id: String,
    val displayName: String,
    val description: String,
    val emoji: String,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color
) {
    EMERALD(
        id            = "emerald",
        displayName   = "Esmeralda",
        description   = "Moderno y minimalista — verde y lavanda suave",
        emoji         = "🌿",
        gradientStart = Color(0xFFA7F3D0),
        gradientMid   = Color(0xFFEDE9FE),
        gradientEnd   = Color(0xFFFBCFE8),
        accent        = Color(0xFF059669),
        accentLight   = Color(0xFFA7F3D0),
        accentDark    = Color(0xFF065F46)
    ),
    WARM(
        id            = "warm",
        displayName   = "Caloroso",
        description   = "Caloroso y organico — rosas, terra y naranja",
        emoji         = "🌸",
        gradientStart = Color(0xFFFECDD3),
        gradientMid   = Color(0xFFFED7AA),
        gradientEnd   = Color(0xFFFEF9C3),
        accent        = Color(0xFFE11D48),
        accentLight   = Color(0xFFFECDD3),
        accentDark    = Color(0xFF9F1239)
    ),
    CLINICAL(
        id            = "clinical",
        displayName   = "Profesional",
        description   = "Limpio y confiable — azul medico y blanco",
        emoji         = "🏥",
        gradientStart = Color(0xFFDBEAFE),
        gradientMid   = Color(0xFFEFF6FF),
        gradientEnd   = Color(0xFFDCFCE7),
        accent        = Color(0xFF2563EB),
        accentLight   = Color(0xFFDBEAFE),
        accentDark    = Color(0xFF1E3A8A)
    ),
    ELEGANT(
        id            = "elegant",
        displayName   = "Elegante",
        description   = "Femenino y elegante — violeta, rosa y oro suave",
        emoji         = "✨",
        gradientStart = Color(0xFFEDE9FE),
        gradientMid   = Color(0xFFFCE7F3),
        gradientEnd   = Color(0xFFFEF3C7),
        accent        = Color(0xFF7C3AED),
        accentLight   = Color(0xFFEDE9FE),
        accentDark    = Color(0xFF4C1D95)
    );

    fun colorScheme(): ColorScheme = lightColorScheme(
        primary            = accent,
        onPrimary          = Color.White,
        primaryContainer   = accentLight,
        onPrimaryContainer = accentDark,
        secondary          = accentDark,
        onSecondary        = Color.White,
        background         = Color(0xFFFAFAFA),
        surface            = Color.White,
        onSurface          = Color(0xFF111827),
        onSurfaceVariant   = Color(0xFF6B7280),
        outline            = Color(0xFFD1D5DB),
        outlineVariant     = Color(0xFFE5E7EB)
    )

    fun gradient() = listOf(gradientStart, gradientMid, gradientEnd)
    fun whiteAlpha(alpha: Float) = Color.White.copy(alpha = alpha)

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: EMERALD
    }
}

// =============================================================================
// SKIN PREFERENCE MANAGER
// Persisted in SharedPreferences — survives app restarts
// =============================================================================

object SkinManager {
    const val PREF_KEY = "selected_skin"

    fun saveSkin(context: android.content.Context, skin: AppSkin) {
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, skin.id).apply()
    }

    fun loadSkin(context: android.content.Context): AppSkin {
        val id = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString(PREF_KEY, null)
        return if (id == null) AppSkin.EMERALD // first launch — will show onboarding
        else AppSkin.fromId(id)
    }

    fun isFirstLaunch(context: android.content.Context): Boolean {
        return !context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .contains(PREF_KEY)
    }
}