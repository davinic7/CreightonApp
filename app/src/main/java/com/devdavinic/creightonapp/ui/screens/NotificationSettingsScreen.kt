package com.devdavinic.creightonapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.devdavinic.creightonapp.notifications.NotificationScheduler
import com.devdavinic.creightonapp.notifications.NotificationSettings
import com.devdavinic.creightonapp.ui.theme.*

// =============================================================================
// NOTIFICATION SETTINGS SCREEN
// =============================================================================

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences("notif_settings", android.content.Context.MODE_PRIVATE)

    // Load saved settings
    var dailyEnabled   by remember { mutableStateOf(prefs.getBoolean("daily_enabled", true)) }
    var dailyHour      by remember { mutableIntStateOf(prefs.getInt("daily_hour", 21)) }
    var dailyMinute    by remember { mutableIntStateOf(prefs.getInt("daily_minute", 0)) }
    var breastEnabled  by remember { mutableStateOf(prefs.getBoolean("breast_enabled", true)) }
    var peakEnabled    by remember { mutableStateOf(prefs.getBoolean("peak_enabled", true)) }
    var fertileEnabled by remember { mutableStateOf(prefs.getBoolean("fertile_enabled", true)) }
    var doublePeakEnabled by remember { mutableStateOf(prefs.getBoolean("double_peak_enabled", true)) }
    var partnerEnabled by remember { mutableStateOf(prefs.getBoolean("partner_enabled", true)) }

    var hasPermission  by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    fun saveAll() {
        prefs.edit()
            .putBoolean("daily_enabled", dailyEnabled)
            .putInt("daily_hour", dailyHour)
            .putInt("daily_minute", dailyMinute)
            .putBoolean("breast_enabled", breastEnabled)
            .putBoolean("peak_enabled", peakEnabled)
            .putBoolean("fertile_enabled", fertileEnabled)
            .putBoolean("double_peak_enabled", doublePeakEnabled)
            .putBoolean("partner_enabled", partnerEnabled)
            .apply()

        if (dailyEnabled && hasPermission) {
            NotificationScheduler.scheduleDailyRegister(context, dailyHour, dailyMinute)
        } else {
            NotificationScheduler.cancelDailyRegister(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(Emerald200, Purple100, Pink200)))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { saveAll(); onBack() }) {
                    Icon(Icons.Outlined.ArrowBack, "Atras",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Notificaciones", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Recordatorios y avisos", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(48.dp))
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Permission banner
                if (!hasPermission) {
                    PermissionBanner {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Daily register
                NotificationCard(
                    icon        = Icons.Outlined.EditNote,
                    title       = "Recordatorio de registro diario",
                    description = "Te recuerda registrar las observaciones del dia al final de la jornada.",
                    enabled     = dailyEnabled && hasPermission,
                    onToggle    = { dailyEnabled = it; saveAll() }
                ) {
                    AnimatedVisibility(visible = dailyEnabled && hasPermission) {
                        TimePicker(
                            hour     = dailyHour,
                            minute   = dailyMinute,
                            onHour   = { dailyHour = it; saveAll() },
                            onMinute = { dailyMinute = it; saveAll() }
                        )
                    }
                }

                // Breast exam
                NotificationCard(
                    icon        = Icons.Outlined.FavoriteBorder,
                    title       = "Autoexamen mamario — Dia 7",
                    description = "Aviso en el dia 7 del ciclo para realizar el autoexamen mamario.",
                    enabled     = breastEnabled && hasPermission,
                    onToggle    = { breastEnabled = it; saveAll() }
                )

                // Peak day suggestion
                NotificationCard(
                    icon        = Icons.Outlined.Star,
                    title       = "Posible Dia Pico detectado",
                    description = "Avisa cuando el sistema detecta que ayer podria haber sido el Dia Pico.",
                    enabled     = peakEnabled && hasPermission,
                    onToggle    = { peakEnabled = it; saveAll() }
                )

                // Fertile phase
                NotificationCard(
                    icon        = Icons.Outlined.Opacity,
                    title       = "Inicio de fase fertil",
                    description = "Avisa cuando aparece el primer signo de moco cervical del ciclo.",
                    enabled     = fertileEnabled && hasPermission,
                    onToggle    = { fertileEnabled = it; saveAll() }
                )

                // Double peak
                NotificationCard(
                    icon        = Icons.Outlined.QuestionMark,
                    title       = "Preguntas del Doble Pico — P+3",
                    description = "Recordatorio en el dia P+3 para responder las preguntas del Doble Pico.",
                    enabled     = doublePeakEnabled && hasPermission,
                    onToggle    = { doublePeakEnabled = it; saveAll() }
                )

                // Partner alert
                NotificationCard(
                    icon        = Icons.Outlined.PeopleAlt,
                    title       = "Avisos al perfil del esposo",
                    description = "Notifica al esposo cuando la fase fertil comienza o cuando hay eventos importantes.",
                    enabled     = partnerEnabled && hasPermission,
                    onToggle    = { partnerEnabled = it; saveAll() }
                )

                // Info note
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(12.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, null, tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp))
                    Text("Las notificaciones se reprograman automaticamente cada dia. Si desinstala y reinstala la app, vuelve a configurarlas aqui.",
                        fontSize = 11.sp, color = Color(0xFF1E40AF), lineHeight = 15.sp)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// =============================================================================
// COMPONENTS
// =============================================================================

@Composable
private fun PermissionBanner(onRequest: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(Color(0xFFFEF2F2))
        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.NotificationsOff, null, tint = Color(0xFFDC2626),
            modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Permiso de notificaciones requerido",
                fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFFDC2626))
            Text("La app necesita permiso para enviar notificaciones.",
                fontSize = 11.sp, color = Color(0xFFEF4444))
        }
        TextButton(onClick = onRequest,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))) {
            Text("Permitir", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun NotificationCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(alpha = 0.7f))
        .border(1.dp,
            if (enabled) Emerald600.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(
                    if (enabled) Emerald600.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                Icon(icon, null,
                    tint = if (enabled) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                    checkedTrackColor = Emerald600))
        }

        extra?.invoke()
    }
}

@Composable
private fun TimePicker(
    hour: Int, minute: Int,
    onHour: (Int) -> Unit, onMinute: (Int) -> Unit
) {
    val formattedTime = String.format("%02d:%02d", hour, minute)
    val ampm = if (hour < 12) "AM" else "PM"
    val hour12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(Color(0xFFF8FAFC)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Hora del recordatorio", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$hour12:${String.format("%02d", minute)} $ampm",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary)
        }

        // Hour slider
        Text("Hora: $hour12 $ampm", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = hour.toFloat(), onValueChange = { onHour(it.toInt()) },
            valueRange = 0f..23f, steps = 22,
            colors = SliderDefaults.colors(thumbColor = Emerald600,
                activeTrackColor = Emerald600))

        // Minute selector — 0, 15, 30, 45
        Text("Minutos:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 15, 30, 45).forEach { m ->
                val sel = minute == m
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Emerald600 else Color(0xFFE2E8F0))
                    .clickable { onMinute(m) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(":${String.format("%02d", m)}", fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}