package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// HOME SCREEN v2
// Skin-aware, dynamic cycle card, animated floating AI assistant
// =============================================================================

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    profile: UserProfile? = null,
    onRegistroParcial: () -> Unit,
    onVerPlanilla: () -> Unit = {},
    onPrediccion: () -> Unit = {},
    onSpice: () -> Unit = {},
    onAsistente: () -> Unit = {},
    onCasosEspeciales: () -> Unit = {},
    onPartner: () -> Unit = {},
    onProfile: () -> Unit = {},
    onCalendario: () -> Unit = {},
    onSimulator: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onSymptoms: () -> Unit = {},
    onExport: () -> Unit = {},
    onVideos: () -> Unit = {},
    onTrivia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val skin         = currentSkin
    val analysis     by viewModel.cycleAnalysis.collectAsState()
    val cycleRecords by viewModel.currentCycleRecords.collectAsState()
    val allRecords   by viewModel.allRecords.collectAsState()

    val cycleDay    = analysis?.cycleDay ?: (cycleRecords.size + 1)
    val phase       = analysis?.currentPhase ?: CyclePhase.UNKNOWN
    val isFertile   = analysis?.isCurrentlyFertile ?: false
    val postPeakDay = analysis?.postPeakDay ?: 0
    val confidence  = analysis?.predictionConfidence

    // Pulse animation for register button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(modifier = modifier.fillMaxSize()
        .background(Brush.linearGradient(skin.gradient()))) {

        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(profile, skin, onProfile, onNotifications)

            Column(modifier = Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Spacer(Modifier.height(4.dp))

                // ── Main cycle status card ────────────────────────────────────
                CycleStatusCard(
                    cycleDay    = cycleDay,
                    phase       = phase,
                    isFertile   = isFertile,
                    postPeakDay = postPeakDay,
                    totalCycles = analysis?.totalCyclesRecorded ?: 0,
                    skin        = skin
                )

                // ── Register button (pulsing) ─────────────────────────────────
                Button(
                    onClick   = onRegistroParcial,
                    modifier  = Modifier.fillMaxWidth().height(56.dp)
                        .scale(pulseScale)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = skin.accent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Registrar observacion del dia",
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                // ── Prediction confidence bar ─────────────────────────────────
                confidence?.let {
                    if (!it.predictionsUnlocked) {
                        ConfidenceBar(it, skin)
                    }
                }

                // ── Quick stats row ───────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickStat("Registros", "${allRecords.size}", Icons.Outlined.CalendarMonth,
                        skin, Modifier.weight(1f))
                    QuickStat("Ciclos", "${analysis?.totalCyclesRecorded ?: 0}",
                        Icons.Outlined.Loop, skin, Modifier.weight(1f))
                    QuickStat("Fase", phase.label.take(8), Icons.Outlined.Favorite,
                        skin, Modifier.weight(1f))
                }

                // ── Daily SPICE tip (tappable → SpiceScreen) ─────────────────
                val dailyTip = remember(cycleDay, phase) {
                    SpiceContent.getDailyTip(cycleDay, phase)
                }
                DailySpiceTipCard(dailyTip, skin, onClick = onSpice)

                // ── Primary 4 quick access ────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryNavCard(NavOption("Planilla",   Icons.Outlined.Description,   onVerPlanilla),   skin, Modifier.weight(1f))
                    PrimaryNavCard(NavOption("Calendario", Icons.Outlined.CalendarMonth, onCalendario),    skin, Modifier.weight(1f))
                    PrimaryNavCard(NavOption("Prediccion", Icons.Outlined.AutoGraph,     onPrediccion),    skin, Modifier.weight(1f))
                    PrimaryNavCard(NavOption("Pareja",     Icons.Outlined.PeopleAlt,     onPartner),       skin, Modifier.weight(1f))
                }

                // ── Collapsible extra modules ──────────────────────────────────
                var expanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.5f))
                    .border(1.dp, skin.accent.copy(0.15f), RoundedCornerShape(16.dp))) {

                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Apps, null,
                                tint = skin.accent, modifier = Modifier.size(18.dp))
                            Text("Mas modulos", fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = skin.accentDark)
                        }
                        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null, tint = skin.accent, modifier = Modifier.size(20.dp))
                    }

                    AnimatedVisibility(visible = expanded,
                        enter = expandVertically(tween(260)) + fadeIn(tween(220)),
                        exit  = shrinkVertically(tween(200)) + fadeOut(tween(160))) {
                        Column(modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = skin.accent.copy(0.1f))
                            Spacer(Modifier.height(4.dp))
                            val extras = buildList {
                                add(NavOption("S-P-I-C-E",       Icons.Outlined.FavoriteBorder,    onSpice))
                                add(NavOption("Sintomas",         Icons.Outlined.MonitorHeart,      onSymptoms))
                                add(NavOption("Casos Esp.",       Icons.Outlined.MedicalServices,   onCasosEspeciales))
                                add(NavOption("Videos",           Icons.Outlined.PlayCircleOutline, onVideos))
                                add(NavOption("Trivia",           Icons.Outlined.Quiz,              onTrivia))
                                add(NavOption("Exportar",         Icons.Outlined.PictureAsPdf,      onExport))
                                add(NavOption("Notificaciones",   Icons.Outlined.NotificationsNone, onNotifications))
                                if (viewModel.isTestMode)
                                    add(NavOption("Simulador",    Icons.Outlined.BugReport,         onSimulator))
                            }
                            extras.chunked(4).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { opt ->
                                        SecondaryNavCard(opt, skin, Modifier.weight(1f))
                                    }
                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }

        // ── Floating AI Assistant ─────────────────────────────────────────────
        FloatingAssistantButton(
            skin     = skin,
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            onClick  = onAsistente
        )
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun HomeHeader(
    profile: UserProfile?, skin: AppSkin,
    onProfile: () -> Unit, onNotifications: () -> Unit
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when { hour < 12 -> "Buenos dias"; hour < 18 -> "Buenas tardes"; else -> "Buenas noches" }
    }
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(profile?.avatarColor ?: "#059669"))
    } catch (e: Exception) { skin.accent }

    Row(modifier = Modifier.fillMaxWidth()
        .background(Color.White.copy(0.4f)).statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {

        Column(modifier = Modifier.weight(1f)) {
            Text(greeting, fontSize = 12.sp, color = skin.accentDark.copy(0.6f))
            Text(profile?.displayName ?: "CreightonApp",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = skin.accentDark)
        }

        IconButton(onClick = onNotifications) {
            Icon(Icons.Outlined.NotificationsNone, null,
                tint = skin.accentDark.copy(0.7f))
        }

        // Avatar — photo or color
        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(avatarColor).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center) {
            if (profile?.avatarPhotoPath != null) {
                // Photo avatar via Coil — if added as dependency
                Text(profile.displayName.take(1).uppercase(),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text(profile?.displayName?.take(1)?.uppercase() ?: "?",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// =============================================================================
// CYCLE STATUS CARD
// =============================================================================

@Composable
private fun CycleStatusCard(
    cycleDay: Int, phase: CyclePhase, isFertile: Boolean,
    postPeakDay: Int, totalCycles: Int, skin: AppSkin
) {
    val (emoji, statusLabel, detail, cardBg) = when {
        phase == CyclePhase.MENSTRUAL ->
            listOf("❤️", "Periodo menstrual", "Tiempo de cuidado y descanso",
                Color(0xFFFEE2E2))
        phase == CyclePhase.PEAK_DAY ->
            listOf("⭐", "Dia Pico", "Momento de maxima fertilidad",
                Color(0xFFECFDF5))
        phase == CyclePhase.POST_PEAK_123 ->
            listOf("🕐", "Post-Pico dia $postPeakDay/3", "Completando cuenta obligatoria",
                Color(0xFFECFDF5))
        isFertile ->
            listOf("🌸", "Fase fertil activa", "Hay moco cervical presente",
                Color(0xFFFEF9C3))
        phase == CyclePhase.POST_PEAK ->
            listOf("🌿", "Fase infertil", "Progesterona dominante",
                Color(0xFFEFF6FF))
        else ->
            listOf("📅", "Dia $cycleDay", "Registra para ver tu analisis",
                Color.White.copy(0.6f))
    }

    @Suppress("UNCHECKED_CAST")
    Column(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(cardBg as Color)
        .border(1.dp, skin.accent.copy(0.2f), RoundedCornerShape(20.dp))
        .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DIA $cycleDay DEL CICLO", fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = skin.accent.copy(0.7f),
                    letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(statusLabel as String, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = skin.accentDark)
                Text(detail as String, fontSize = 13.sp,
                    color = skin.accentDark.copy(0.7f))
            }
            Text(emoji as String, fontSize = 52.sp)
        }

        HorizontalDivider(color = skin.accent.copy(0.15f))

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround) {
            StatusPill(if (isFertile) "FERTIL" else "INFERTIL",
                if (isFertile) skin.accent else Color(0xFF2563EB), skin)
            StatusPill(phase.label, skin.accentDark, skin)
            if (totalCycles > 0) StatusPill("$totalCycles ciclos", skin.accent, skin)
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color, skin: AppSkin) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(color.copy(0.1f))
        .border(1.dp, color.copy(0.3f), RoundedCornerShape(20.dp))
        .padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

// =============================================================================
// CONFIDENCE BAR
// =============================================================================

@Composable
private fun ConfidenceBar(confidence: PredictionConfidence, skin: AppSkin) {
    val completed = 0 // will be computed from totalCyclesRecorded - 1
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(skin.accentLight.copy(0.5f))
        .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Lock, null, tint = skin.accent,
            modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Predicciones bloqueadas", fontSize = 12.sp,
                fontWeight = FontWeight.Medium, color = skin.accentDark)
            Text("Necesitas 3 ciclos completos", fontSize = 10.sp,
                color = skin.accentDark.copy(0.6f))
        }
    }
}

// =============================================================================
// QUICK STATS
// =============================================================================

@Composable
private fun QuickStat(
    label: String, value: String, icon: ImageVector,
    skin: AppSkin, modifier: Modifier
) {
    Column(modifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(0.55f))
        .border(1.dp, skin.accent.copy(0.15f), RoundedCornerShape(14.dp))
        .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = skin.accent, modifier = Modifier.size(18.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = skin.accentDark)
        Text(label, fontSize = 9.sp, color = skin.accentDark.copy(0.6f))
    }
}

// =============================================================================
// DAILY SPICE TIP
// =============================================================================

@Composable
fun DailySpiceTipCard(tip: SpiceTip, skin: AppSkin, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(0.55f))
        .border(1.dp, skin.accent.copy(0.2f), RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
            .background(skin.accentLight),
            contentAlignment = Alignment.Center) {
            Text(tip.dimension.icon, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Consejo del dia — ${tip.dimension.label}",
                fontSize = 10.sp, color = skin.accent, fontWeight = FontWeight.Medium)
            Text(tip.title, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = skin.accentDark, maxLines = 1)
            Text(tip.body, fontSize = 11.sp, color = skin.accentDark.copy(0.7f),
                maxLines = 2, lineHeight = 14.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null,
            tint = skin.accent, modifier = Modifier.size(18.dp))
    }
}

// =============================================================================
// NAV GRID
// =============================================================================

data class NavOption(val label: String, val icon: ImageVector, val onClick: () -> Unit)


@Composable
private fun PrimaryNavCard(option: NavOption, skin: AppSkin, modifier: Modifier) {
    val scale by animateFloatAsState(1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "ps")
    Column(modifier = modifier
        .clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(0.72f))
        .border(1.5.dp, skin.accent.copy(0.25f), RoundedCornerShape(18.dp))
        .clickable { option.onClick() }
        .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
            .background(skin.accentLight),
            contentAlignment = Alignment.Center) {
            Icon(option.icon, null, tint = skin.accent,
                modifier = Modifier.size(22.dp))
        }
        Text(option.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = skin.accentDark, textAlign = TextAlign.Center,
            maxLines = 2, lineHeight = 13.sp)
    }
}

@Composable
private fun SecondaryNavCard(option: NavOption, skin: AppSkin, modifier: Modifier) {
    Column(modifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(0.55f))
        .clickable { option.onClick() }
        .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(skin.accentLight.copy(0.7f)),
            contentAlignment = Alignment.Center) {
            Icon(option.icon, null, tint = skin.accentDark,
                modifier = Modifier.size(18.dp))
        }
        Text(option.label, fontSize = 9.5.sp, fontWeight = FontWeight.Medium,
            color = skin.accentDark, textAlign = TextAlign.Center,
            maxLines = 2, lineHeight = 11.sp)
    }
}



// =============================================================================
// FLOATING ASSISTANT BUTTON
// =============================================================================

@Composable
fun FloatingAssistantButton(skin: AppSkin, modifier: Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )

    Box(modifier = modifier.offset(y = floatY.dp)) {
        // Glow ring
        Box(modifier = Modifier.size(68.dp).align(Alignment.Center)
            .clip(CircleShape)
            .background(skin.accent.copy(glowAlpha)))

        // Main FAB
        FloatingActionButton(
            onClick            = onClick,
            modifier           = Modifier.size(56.dp),
            shape              = CircleShape,
            containerColor     = skin.accent,
            contentColor       = Color.White,
            elevation          = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp, pressedElevation = 4.dp)
        ) {
            Icon(Icons.Outlined.SmartToy, "Asistente IA",
                modifier = Modifier.size(26.dp))
        }
    }
}