package com.devdavinic.creightonapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// =============================================================================
// SKIN-AWARE THEME
// =============================================================================

val LocalAppSkin = staticCompositionLocalOf<AppSkin> { AppSkin.EMERALD }

@Composable
fun CreightonAppTheme(
    skin: AppSkin = AppSkin.EMERALD,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppSkin provides skin) {
        MaterialTheme(
            colorScheme = skin.colorScheme(),
            typography  = Typography,
            content     = content
        )
    }
}

// Convenience accessor
val currentSkin @Composable get() = LocalAppSkin.current