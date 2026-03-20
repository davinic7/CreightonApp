package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.MainViewModel

// =============================================================================
// MODULE 3 - PREDICTION & INTERPRETATION SCREEN
// =============================================================================

@Composable
fun PredictionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val analysis by viewModel.cycleAnalysis.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(Emerald200, Purple100, Pink200)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            PredictionHeader(onBack = onBack)

            if (analysis == null) {
                EmptyAnalysisState()
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    analysis?.let { a ->
                        PhaseStatusCard(analysis = a)
                        HormonalPhaseCard(hormonalPhase = a.hormonalPhase)
                        CycleMetricsRow(analysis = a)
                        if (a.estimatedPeakDay != null || a.estimatedNextPeriod != null) {
                            PredictionsCard(analysis = a)
                        }
                        if (a.totalCyclesRecorded >= 2) {
                            HistoricalAveragesCard(analysis = a)
                        }
                        val warningAlerts = a.alerts.filter {
                            it.severity == AlertSeverity.WARNING || it.severity == AlertSeverity.CRITICAL
                        }
                        val infoAlerts = a.alerts.filter { it.severity == AlertSeverity.INFO }
                        if (warningAlerts.isNotEmpty()) {
                            AlertsSection(title = "Senales a tener en cuenta", alerts = warningAlerts)
                        }
                        if (infoAlerts.isNotEmpty()) {
                            AlertsSection(title = "Estado del ciclo", alerts = infoAlerts, isInfo = true)
                        }
                        ManualNoteCard(phase = a.currentPhase, postPeakDay = a.postPeakDay)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun PredictionHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteAlpha40)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Atras",
                tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "Prediccion e Interpretacion",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text     = "Analisis del ciclo actual",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

// =============================================================================
// PHASE STATUS CARD
// =============================================================================

@Composable
private fun PhaseStatusCard(analysis: CycleAnalysis) {
    val accentColor = when (analysis.currentPhase) {
        CyclePhase.MENSTRUAL      -> Color(0xFFDC2626)
        CyclePhase.PRE_PEAK       -> Emerald600
        CyclePhase.PEAK_DAY       -> Emerald600
        CyclePhase.POST_PEAK_123  -> Emerald600
        CyclePhase.POST_PEAK      -> Color(0xFF2563EB)
        CyclePhase.UNKNOWN        -> MaterialTheme.colorScheme.outline
    }
    val phaseIcon = when (analysis.currentPhase) {
        CyclePhase.MENSTRUAL      -> Icons.Outlined.WaterDrop
        CyclePhase.PRE_PEAK       -> Icons.Outlined.TrendingUp
        CyclePhase.PEAK_DAY       -> Icons.Outlined.Star
        CyclePhase.POST_PEAK_123  -> Icons.Outlined.Timer
        CyclePhase.POST_PEAK      -> Icons.Outlined.Shield
        CyclePhase.UNKNOWN        -> Icons.Outlined.HelpOutline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WhiteAlpha60)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FASE ACTUAL", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = analysis.currentPhase.label,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = accentColor
                    )
                    Text(
                        text       = analysis.currentPhase.description,
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(2.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "${analysis.cycleDay}",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                        Text("dia", fontSize = 10.sp, color = accentColor.copy(alpha = 0.7f))
                    }
                }
            }

            // Fertile/infertile indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (analysis.isCurrentlyFertile) Color(0xFFFEF3C7) else Color(0xFFEFF6FF)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector      = if (analysis.isCurrentlyFertile) Icons.Outlined.FavoriteBorder
                    else Icons.Outlined.Shield,
                    contentDescription = null,
                    tint             = if (analysis.isCurrentlyFertile) Color(0xFFD97706)
                    else Color(0xFF2563EB),
                    modifier         = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text       = if (analysis.isCurrentlyFertile) "Fase fertil activa"
                        else "Fase infertil confirmada",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = if (analysis.isCurrentlyFertile) Color(0xFF92400E)
                        else Color(0xFF1E40AF)
                    )
                    if (analysis.postPeakDay in 1..3) {
                        Text(
                            text       = "Dia post-Pico: ${analysis.postPeakDay} / 3. Infertilidad al final del dia 4.",
                            fontSize   = 11.sp,
                            color      = Color(0xFF92400E),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// HORMONAL PHASE CARD
// =============================================================================

@Composable
private fun HormonalPhaseCard(hormonalPhase: HormonalPhase) {
    if (hormonalPhase == HormonalPhase.UNKNOWN) return

    val color = when (hormonalPhase) {
        HormonalPhase.FOLLICULAR -> Color(0xFF7C3AED)
        HormonalPhase.OVULATORY  -> Emerald600
        HormonalPhase.LUTEAL     -> Color(0xFF2563EB)
        HormonalPhase.MENSTRUAL  -> Color(0xFFDC2626)
        HormonalPhase.UNKNOWN    -> MaterialTheme.colorScheme.outline
    }
    val icon = when (hormonalPhase) {
        HormonalPhase.FOLLICULAR -> Icons.Outlined.TrendingUp
        HormonalPhase.OVULATORY  -> Icons.Outlined.Star
        HormonalPhase.LUTEAL     -> Icons.Outlined.TrendingDown
        HormonalPhase.MENSTRUAL  -> Icons.Outlined.Refresh
        HormonalPhase.UNKNOWN    -> Icons.Outlined.HelpOutline
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WhiteAlpha60)
                .border(1.dp, WhiteAlpha40, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(hormonalPhase.label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = color)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(hormonalPhase.dominantHormone, fontSize = 10.sp, color = color,
                            fontWeight = FontWeight.Medium)
                    }
                }
                Text(hormonalPhase.description, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
            }
        }
        Text(
            text       = "Esta es una estimacion educativa basada en tus registros. No reemplaza la evaluacion del Profesional de FertilityCare.",
            fontSize   = 10.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp,
            modifier   = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// =============================================================================
// CYCLE METRICS
// =============================================================================

@Composable
private fun CycleMetricsRow(analysis: CycleAnalysis) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard("Dias con moco",  "${analysis.mucusCycleDays}",   "dias",    Emerald600,           Modifier.weight(1f))
        MetricCard("Fase pre-Pico",  "${analysis.prePicoPhaseDays}", "dias",    Color(0xFF7C3AED),    Modifier.weight(1f))
        if (analysis.postPicoPhaseDays > 0) {
            MetricCard("Fase post-Pico", "${analysis.postPicoPhaseDays}", "dias", Color(0xFF2563EB), Modifier.weight(1f))
        } else {
            MetricCard("Dia del ciclo", "${analysis.cycleDay}", "de ciclo", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(
    label: String, value: String, unit: String,
    color: Color, modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WhiteAlpha60)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        Text(unit, fontSize = 10.sp, color = color.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

// =============================================================================
// PREDICTIONS CARD
// =============================================================================

@Composable
private fun PredictionsCard(analysis: CycleAnalysis) {
    SectionCard("Predicciones", Icons.Outlined.AutoGraph) {
        analysis.estimatedPeakDay?.let { peakDay ->
            val daysUntilPeak = peakDay - analysis.cycleDay
            PredictionRow(
                icon    = Icons.Outlined.Star,
                color   = Emerald600,
                label   = "Dia Pico estimado",
                value   = "Dia $peakDay del ciclo",
                subtext = when {
                    daysUntilPeak > 0  -> "En aprox. $daysUntilPeak dias"
                    daysUntilPeak == 0 -> "Hoy podria ser el Dia Pico"
                    else               -> "Ya paso (dia ${analysis.cycleDay})"
                }
            )
        }
        analysis.estimatedNextPeriod?.let { days ->
            PredictionRow(
                icon    = Icons.Outlined.CalendarToday,
                color   = Color(0xFFDC2626),
                label   = "Proxima menstruacion",
                value   = if (days == 0) "Pronto" else "En ~$days dias",
                subtext = "Basado en tu promedio historico"
            )
        }
        Text(
            text       = "Las predicciones son estimaciones. El Modelo Creighton es prospectivo: observa cada dia.",
            fontSize   = 10.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun PredictionRow(
    icon: ImageVector, color: Color,
    label: String, value: String, subtext: String
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtext, fontSize = 11.sp, color = color)
        }
    }
}

// =============================================================================
// HISTORICAL AVERAGES
// =============================================================================

@Composable
private fun HistoricalAveragesCard(analysis: CycleAnalysis) {
    SectionCard("Tus promedios (${analysis.totalCyclesRecorded} ciclos)", Icons.Outlined.Analytics) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            analysis.avgCycleLength?.let {
                AvgChip("Ciclo", "$it dias", Modifier.weight(1f))
            }
            analysis.avgPostPeakLength?.let { avg ->
                val status = when {
                    avg <= 8  -> "Corto"
                    avg >= 17 -> "Largo"
                    else      -> "Normal"
                }
                AvgChip("Post-Pico", "$avg dias / $status", Modifier.weight(1f),
                    accent = avg <= 8 || avg >= 17)
            }
            analysis.avgMucusCycleDays?.let {
                AvgChip("Dias moco", "$it dias", Modifier.weight(1f))
            }
        }
        analysis.avgPostPeakLength?.let { avg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (avg in 9..16) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector      = if (avg in 9..16) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint             = if (avg in 9..16) Emerald600 else Color(0xFFD97706),
                    modifier         = Modifier.size(16.dp)
                )
                Text(
                    text       = if (avg in 9..16)
                        "Tu fase post-Pico promedio ($avg dias) esta dentro del rango normal del manual (9-17 dias)."
                    else
                        "Tu fase post-Pico promedio ($avg dias) esta fuera del rango normal del manual (9-17 dias). Consulta con tu Profesional.",
                    fontSize   = 11.sp,
                    color      = if (avg in 9..16) Color(0xFF065F46) else Color(0xFF92400E),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AvgChip(label: String, value: String, modifier: Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) Color(0xFFFEF3C7) else WhiteAlpha60)
            .border(1.dp, if (accent) Color(0xFFFBBF24) else WhiteAlpha40, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = if (accent) Color(0xFF92400E) else MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

// =============================================================================
// ALERTS SECTION
// =============================================================================

@Composable
private fun AlertsSection(
    title: String,
    alerts: List<CycleAlert>,
    isInfo: Boolean = false
) {
    SectionCard(
        title = title,
        icon  = if (isInfo) Icons.Outlined.Info else Icons.Outlined.NotificationImportant
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            alerts.forEach { alert -> AlertCard(alert = alert) }
        }
    }
}

@Composable
private fun AlertCard(alert: CycleAlert) {
    val (bg, borderColor, titleColor, icon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertColors(
            Color(0xFFFEE2E2), Color(0xFFEF4444), Color(0xFFDC2626), Icons.Outlined.ErrorOutline
        )
        AlertSeverity.WARNING  -> AlertColors(
            Color(0xFFFEF3C7), Color(0xFFFBBF24), Color(0xFFD97706), Icons.Outlined.Warning
        )
        AlertSeverity.INFO     -> AlertColors(
            Color(0xFFEFF6FF), Color(0xFF93C5FD), Color(0xFF2563EB), Icons.Outlined.Info
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = titleColor, modifier = Modifier.size(18.dp))
            Text(alert.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = titleColor)
        }
        Text(alert.message, fontSize = 12.sp, color = titleColor.copy(alpha = 0.85f), lineHeight = 16.sp)
        alert.actionHint?.let { hint ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ArrowForward, null, tint = titleColor, modifier = Modifier.size(12.dp))
                Text(hint, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = titleColor)
            }
        }
    }
}

private data class AlertColors(
    val bg: Color,
    val border: Color,
    val title: Color,
    val icon: ImageVector
)
private operator fun AlertColors.component1() = bg
private operator fun AlertColors.component2() = border
private operator fun AlertColors.component3() = title
private operator fun AlertColors.component4() = icon

// =============================================================================
// MANUAL NOTE CARD
// =============================================================================

@Composable
private fun ManualNoteCard(phase: CyclePhase, postPeakDay: Int) {
    val note = when (phase) {
        CyclePhase.MENSTRUAL ->
            "El periodo menstrual se considera fertil porque toda mujer podria tener un ciclo corto en algun momento. Observa el moco durante los ultimos dias del flujo. (Cap. 7)"
        CyclePhase.PRE_PEAK ->
            "Estas en fase pre-Pico. El moco cervical es el principal indicador de fertilidad. Registra el signo mas fertil al final de cada dia. (Cap. 6)"
        CyclePhase.PEAK_DAY ->
            "El Dia Pico es el ultimo dia de moco transparente, elastico o lubricante. La ovulacion ocurre en los 2 dias anteriores o posteriores en el 95% de los ciclos. (Cap. 7)"
        CyclePhase.POST_PEAK_123 ->
            "Dias 1-3 post-Pico: la infertilidad comienza al FINAL del dia 4. La progesterona suprime la produccion de moco. Cuenta con cuidado. (Cap. 7)"
        CyclePhase.POST_PEAK ->
            "Estas en la fase post-Pico. Es el periodo infertil mas confiable del ciclo. La progesterona es la hormona dominante hasta la proxima menstruacion. (Cap. 3 y 7)"
        CyclePhase.UNKNOWN ->
            "Comienza a registrar tus observaciones diarias. El Modelo Creighton es prospectivo: cada registro te da informacion valiosa sobre tu salud y fertilidad."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WhiteAlpha50)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Icon(Icons.Outlined.MenuBook, null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column {
            Text("Del manual Creighton", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(note, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
        }
    }
}

// =============================================================================
// EMPTY STATE
// =============================================================================

@Composable
private fun EmptyAnalysisState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📊", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Sin analisis disponible", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Registra al menos un dia para ver el analisis de tu ciclo.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteAlpha60)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        content()
    }
}