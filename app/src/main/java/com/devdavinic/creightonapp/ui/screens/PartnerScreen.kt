package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.window.Dialog
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.AuthViewModel
import com.devdavinic.creightonapp.viewmodel.MainViewModel

// =============================================================================
// PARTNER SCREEN v2
// Full partner experience — requires linked account
// Tabs: Ciclo | Calendario | S-P-I-C-E | Asistente
// =============================================================================

private enum class PartnerTab(val label: String, val icon: ImageVector) {
    CYCLE    ("Ciclo",      Icons.Outlined.Favorite),
    CALENDAR ("Calendario", Icons.Outlined.CalendarMonth),
    SPICE    ("S-P-I-C-E", Icons.Outlined.FavoriteBorder),
    AI       ("Asistente",  Icons.Outlined.SmartToy)
}

@Composable
fun PartnerScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onGoToAiChat: () -> Unit = {}
) {
    val profile        by authViewModel.currentProfile.collectAsState()
    val partnerProfile by authViewModel.partnerProfile.collectAsState()
    val analysis       by viewModel.cycleAnalysis.collectAsState()
    val cycleRecords   by viewModel.currentCycleRecords.collectAsState()

    val isLinked       = profile?.partnerUid != null
    var activeTab      by remember { mutableStateOf(PartnerTab.CYCLE) }
    var showIDialog    by remember { mutableStateOf(false) }
    var showDoublePeak by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }
    var iRegistered    by remember { mutableStateOf(false) }

    val cycleDay    = analysis?.cycleDay ?: (cycleRecords.size + 1)
    val isFertile   = analysis?.isCurrentlyFertile ?: false
    val phase       = analysis?.currentPhase ?: CyclePhase.UNKNOWN
    val postPeakDay = analysis?.postPeakDay ?: 0
    val isDay7      = cycleDay == 7
    val isPostPeak3 = postPeakDay == 3

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(
            Color(0xFFDBEAFE), Color(0xFFEDE9FE), Color(0xFFD1FAE5))))) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            PartnerHeader(
                partnerName    = partnerProfile?.displayName,
                partnerColor   = partnerProfile?.avatarColor ?: "#059669",
                isLinked       = isLinked,
                onBack         = onBack,
                onUnlink       = { showUnlinkDialog = true }
            )

            if (!isLinked) {
                // Not linked state
                NotLinkedState(
                    myCode  = profile?.partnerLinkCode ?: "------",
                    authViewModel = authViewModel
                )
            } else {
                // Tab bar
                TabBar(activeTab = activeTab, onTabSelect = { activeTab = it })

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        },
                        label = "partnerTab"
                    ) { tab ->
                        when (tab) {
                            PartnerTab.CYCLE ->
                                CycleTab(
                                    analysis    = analysis,
                                    cycleDay    = cycleDay,
                                    phase       = phase,
                                    isFertile   = isFertile,
                                    postPeakDay = postPeakDay,
                                    isDay7      = isDay7,
                                    isPostPeak3 = isPostPeak3,
                                    iRegistered = iRegistered,
                                    onRegisterI = { showIDialog = true },
                                    onDoublePeak = { showDoublePeak = true }
                                )
                            PartnerTab.CALENDAR ->
                                PartnerCalendarTab(viewModel = viewModel)
                            PartnerTab.SPICE ->
                                PartnerSpiceTab(analysis = analysis, cycleDay = cycleDay)
                            PartnerTab.AI ->
                                PartnerAiTab(onGoToAiChat = onGoToAiChat)
                        }
                    }
                }
            }
        }

        // Intercourse dialog
        if (showIDialog) {
            IntercourseDialog(
                alreadyRegistered = iRegistered,
                onConfirm = {
                    authViewModel.registerIntercourseForPartner { success ->
                        if (success) iRegistered = true
                    }
                    showIDialog = false
                },
                onDismiss = { showIDialog = false }
            )
        }

        // Double peak dialog
        if (showDoublePeak) {
            DoublePeakDialog(onDismiss = { showDoublePeak = false })
        }

        // Unlink dialog
        if (showUnlinkDialog) {
            AlertDialog(
                onDismissRequest = { showUnlinkDialog = false },
                title = { Text("Desvincular pareja") },
                text  = { Text("Esto eliminara la vinculacion con ${partnerProfile?.displayName ?: "tu pareja"}. Podran volver a vincularse con el codigo en cualquier momento.") },
                confirmButton = {
                    TextButton(onClick = {
                        authViewModel.unlinkPartner { }
                        showUnlinkDialog = false
                    }) { Text("Desvincular", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun PartnerHeader(
    partnerName: String?,
    partnerColor: String,
    isLinked: Boolean,
    onBack: () -> Unit,
    onUnlink: () -> Unit
) {
    val color = try { Color(android.graphics.Color.parseColor(partnerColor)) }
    catch (e: Exception) { Emerald600 }

    Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
        .statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, "Atras",
                tint = MaterialTheme.colorScheme.onSurface)
        }
        if (isLinked && partnerName != null) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center) {
                Text(partnerName.take(1).uppercase(), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(if (partnerName != null) "Perfil de $partnerName"
            else "Vista de la Pareja",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            if (isLinked) Text("Vinculado", fontSize = 10.sp, color = Emerald600)
            else Text("Sin vincular", fontSize = 10.sp, color = Color(0xFFD97706))
        }
        if (isLinked) {
            IconButton(onClick = onUnlink) {
                Icon(Icons.Outlined.LinkOff, "Desvincular",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

// =============================================================================
// TAB BAR
// =============================================================================

@Composable
private fun TabBar(activeTab: PartnerTab, onTabSelect: (PartnerTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(Color(0x55FFFFFF))
        .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PartnerTab.entries.forEach { tab ->
            val isActive = activeTab == tab
            Box(modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) Emerald600 else Color.Transparent)
                .clickable { onTabSelect(tab) }
                .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(tab.icon, null,
                        tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                    Text(tab.label, fontSize = 9.sp,
                        color = if (isActive) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}

// =============================================================================
// NOT LINKED STATE
// =============================================================================

@Composable
private fun NotLinkedState(myCode: String, authViewModel: AuthViewModel) {
    var partnerCode  by remember { mutableStateOf("") }
    var linkResult   by remember { mutableStateOf<String?>(null) }
    var isLinking    by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.size(80.dp).clip(CircleShape)
            .background(Color(0xFFFEF3C7)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Link, null, tint = Color(0xFFD97706),
                modifier = Modifier.size(40.dp))
        }

        Text("Conecta con tu pareja", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Text("Para ver el ciclo de tu pareja, necesitan vincularse usando el codigo de 6 letras.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 20.sp)

        // My code to share
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.75f)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tu codigo de vinculacion", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(myCode, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp, color = MaterialTheme.colorScheme.primary)
            Text("Compartilo con tu pareja para que ingrese tu codigo",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }

        Text("— o ingresa el codigo de tu pareja —", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Enter partner code
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.75f)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value         = partnerCode.uppercase(),
                onValueChange = { if (it.length <= 6) partnerCode = it },
                label         = { Text("Codigo de tu pareja (6 letras)") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true,
                isError       = linkResult?.startsWith("Error") == true || linkResult?.startsWith("Codigo") == true
            )
            linkResult?.let { msg ->
                val isError = !msg.startsWith("Vinculacion")
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (isError) Color(0xFFFEF2F2) else Color(0xFFECFDF5))
                    .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                        null, tint = if (isError) Color(0xFFDC2626) else Emerald600,
                        modifier = Modifier.size(16.dp))
                    Text(msg, fontSize = 12.sp,
                        color = if (isError) Color(0xFFDC2626) else Color(0xFF065F46))
                }
            }
            Button(
                onClick = {
                    isLinking = true
                    authViewModel.linkWithPartner(partnerCode) { success, msg ->
                        linkResult = msg
                        isLinking  = false
                    }
                },
                enabled  = partnerCode.length == 6 && !isLinking,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                if (isLinking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                        color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Vincular")
            }
        }
    }
}

// =============================================================================
// CYCLE TAB
// =============================================================================

@Composable
private fun CycleTab(
    analysis: com.devdavinic.creightonapp.model.CycleAnalysis?,
    cycleDay: Int, phase: CyclePhase, isFertile: Boolean,
    postPeakDay: Int, isDay7: Boolean, isPostPeak3: Boolean,
    iRegistered: Boolean,
    onRegisterI: () -> Unit, onDoublePeak: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // Status card
        PartnerStatusCard(cycleDay, phase, isFertile, postPeakDay)

        // Notifications
        if (isDay7) {
            NotifCard(Icons.Outlined.FavoriteBorder,
                "Recordatorio: Autoexamen mamario",
                "Hoy es el dia 7. El manual recomienda que ella realice el AM hoy.",
                Color(0xFFDB2777))
        }
        if (isPostPeak3) {
            NotifCard(Icons.Outlined.QuestionMark,
                "Preguntas del Doble Pico — P+3",
                "Hoy es P+3. Deben responder juntos las preguntas del Doble Pico.",
                Emerald600,
                actionLabel = "Ver preguntas",
                onAction    = onDoublePeak)
        }

        // Register I button
        RegisterICard(iRegistered, onRegisterI)

        // Cycle metrics if available
        analysis?.let { a ->
            if (a.totalCyclesRecorded > 0) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.65f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Metricas del ciclo", fontWeight = FontWeight.Medium,
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    a.avgCycleLength?.let { MetricRow("Duracion promedio", "$it dias") }
                    a.avgPostPeakLength?.let { MetricRow("Post-Pico promedio", "$it dias") }
                    MetricRow("Ciclos registrados", "${a.totalCyclesRecorded}")
                    a.estimatedNextPeriod?.let { MetricRow("Proxima menstruacion", "En ~$it dias") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PartnerStatusCard(
    cycleDay: Int, phase: CyclePhase,
    isFertile: Boolean, postPeakDay: Int
) {
    val (bg, accent, emoji, statusText, detail) = when {
        phase == CyclePhase.MENSTRUAL ->
            listOf(Color(0xFFFEE2E2), Color(0xFFDC2626), "❤️",
                "Periodo menstrual", "Tiempo de acompanamiento y cuidado.")
        phase == CyclePhase.PEAK_DAY ->
            listOf(Color(0xFFECFDF5), Emerald600, "⭐",
                "Dia Pico — Maxima fertilidad", "El momento mas significativo del ciclo.")
        phase == CyclePhase.POST_PEAK_123 ->
            listOf(Color(0xFFECFDF5), Emerald600, "🕐",
                "Cuenta post-Pico: $postPeakDay/3", "La infertilidad comienza al terminar el dia 3.")
        phase == CyclePhase.POST_PEAK ->
            listOf(Color(0xFFEFF6FF), Color(0xFF2563EB), "🌿",
                "Fase infertil", "Progesterona dominante. Tiempo de mayor libertad.")
        isFertile ->
            listOf(Color(0xFFFEF3C7), Color(0xFFD97706), "🌸",
                "Fase fertil", "Hay moco cervical activo. Tiempo de comunicacion.")
        else ->
            listOf(Color(0xFFEFF6FF), Color(0xFF2563EB), "🌿",
                "Fase infertil", "Tiempo de mayor libertad en la intimidad.")
    }

    @Suppress("UNCHECKED_CAST")
    val accentColor = accent as Color

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(bg as Color).border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(18.dp))
        .padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DIA $cycleDay DEL CICLO", fontSize = 10.sp,
                    fontWeight = FontWeight.Medium, color = accentColor)
                Text(statusText as String, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = accentColor)
                Text(detail as String, fontSize = 12.sp,
                    color = accentColor.copy(0.8f), lineHeight = 16.sp)
            }
            Text(emoji as String, fontSize = 42.sp)
        }

        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(0.1f)).padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isFertile) Icons.Outlined.FavoriteBorder else Icons.Outlined.Shield,
                null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isFertile) "TIEMPO FERTIL" else "TIEMPO INFERTIL",
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = accentColor)
        }
    }
}

@Composable
private fun NotifCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, message: String, color: Color,
    actionLabel: String? = null, onAction: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(color.copy(0.08f)).border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp))
        .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = color)
            Text(message, fontSize = 11.sp, color = color.copy(0.8f), lineHeight = 14.sp)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, fontSize = 11.sp, color = color)
            }
        }
    }
}

@Composable
private fun RegisterICard(registered: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (registered) Emerald200.copy(0.5f) else Color.White.copy(0.7f))
        .border(1.dp,
            if (registered) Emerald600 else MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(14.dp))
        .clickable(enabled = !registered, onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(if (registered) Emerald600.copy(0.15f)
            else MaterialTheme.colorScheme.primary.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Icon(if (registered) Icons.Outlined.CheckCircle else Icons.Outlined.AddCircleOutline,
                null,
                tint = if (registered) Emerald600 else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(if (registered) "Relacion intima registrada" else "Registrar relacion intima — I",
                fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = if (registered) Emerald600 else MaterialTheme.colorScheme.onSurface)
            Text(if (registered) "La I fue enviada para aparecer en la planilla de ella."
            else "Toca para registrar la I en la planilla del dia de hoy.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp)
        }
        if (!registered) Icon(Icons.Outlined.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

// =============================================================================
// CALENDAR TAB - reuses CalendarScreen content
// =============================================================================

@Composable
private fun PartnerCalendarTab(viewModel: MainViewModel) {
    val allRecords by viewModel.allRecords.collectAsState()
    val analysis   by viewModel.cycleAnalysis.collectAsState()

    // Simplified calendar view embedded in tab
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text("Calendario del ciclo", fontWeight = FontWeight.Medium,
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

        analysis?.let { a ->
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                a.estimatedPeakDay?.let {
                    PredChip("Proximo Pico", "~$it dias", Emerald600, Modifier.weight(1f))
                }
                a.estimatedNextPeriod?.let {
                    PredChip("Proxima mens.", "~$it dias", Color(0xFFDC2626), Modifier.weight(1f))
                }
            }
            if (!a.predictionConfidence.predictionsUnlocked) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF3C7)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Lock, null, tint = Color(0xFFD97706),
                        modifier = Modifier.size(14.dp))
                    Text("Se necesitan 3 ciclos completos para activar las predicciones. Ciclos actuales: ${a.totalCyclesRecorded - 1}/3",
                        fontSize = 11.sp, color = Color(0xFF92400E), lineHeight = 14.sp)
                }
            }
        }

        Text("Para ver el calendario completo con estampas, ve a la seccion Calendario desde la pantalla principal.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PredChip(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(color.copy(0.08f)).border(1.dp, color.copy(0.3f), RoundedCornerShape(10.dp))
        .padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = color.copy(0.8f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// =============================================================================
// SPICE TAB
// =============================================================================

@Composable
private fun PartnerSpiceTab(
    analysis: com.devdavinic.creightonapp.model.CycleAnalysis?,
    cycleDay: Int
) {
    val phase = analysis?.currentPhase ?: CyclePhase.UNKNOWN
    val tip   = remember(cycleDay, phase) { SpiceContent.getDailyTip(cycleDay, phase) }
    val dimColor = when (tip.dimension) {
        SpiceDimension.SPIRITUAL    -> Color(0xFF7C3AED)
        SpiceDimension.PHYSICAL     -> Color(0xFF059669)
        SpiceDimension.INTELLECTUAL -> Color(0xFF2563EB)
        SpiceDimension.CREATIVE     -> Color(0xFFD97706)
        SpiceDimension.EMOTIONAL    -> Color(0xFFDB2777)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Text("Consejo S-P-I-C-E del dia", fontWeight = FontWeight.Medium,
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.75f))
            .border(1.dp, dimColor.copy(0.3f), RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(dimColor.copy(0.12f)), contentAlignment = Alignment.Center) {
                    Text(tip.dimension.icon, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = dimColor)
                }
                Column {
                    Text(tip.dimension.label, fontWeight = FontWeight.Medium,
                        fontSize = 13.sp, color = dimColor)
                    Text("Para el esposo — Dia $cycleDay", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(tip.title, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text(tip.body, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)
            tip.actionSuggestion?.let { action ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(dimColor.copy(0.07f)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = dimColor,
                        modifier = Modifier.size(16.dp))
                    Text(action, fontSize = 12.sp, color = dimColor,
                        lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Phase context for husband
        val phaseNote = when (phase) {
            CyclePhase.MENSTRUAL     -> "Tu esposa esta en su periodo. Es un buen momento para el cuidado y la atencion especial."
            CyclePhase.PRE_PEAK      -> "El estrogeno esta subiendo. Tu esposa puede sentirse con mas energia y buen humor."
            CyclePhase.PEAK_DAY      -> "Hoy es el Dia Pico — el momento de mayor fertilidad y conexion en el ciclo."
            CyclePhase.POST_PEAK_123 -> "Dias post-Pico 1-3. La infertilidad esta llegando. Es un tiempo de paciencia y comunicacion."
            CyclePhase.POST_PEAK     -> "Fase post-Pico. La progesterona es dominante — pueden aparecer cambios de humor."
            CyclePhase.UNKNOWN       -> "Con mas registros, aqui veras consejos personalizados para cada fase del ciclo."
        }
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFF6FF)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = Color(0xFF2563EB),
                modifier = Modifier.size(16.dp))
            Text(phaseNote, fontSize = 12.sp, color = Color(0xFF1E40AF), lineHeight = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// =============================================================================
// AI TAB
// =============================================================================

@Composable
private fun PartnerAiTab(onGoToAiChat: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.size(72.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.SmartToy, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }

        Text("Asistente IA del Creighton", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)

        Text("El asistente puede responder tus preguntas sobre el Modelo Creighton, el ciclo de tu pareja y la planificacion familiar natural.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 20.sp)

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.7f)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Preguntas frecuentes del esposo:", fontSize = 12.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            listOf(
                "Que significa el Dia Pico para nosotros?",
                "Como funciona la fase post-Pico?",
                "Que es S-P-I-C-E en el Creighton?",
                "Cuanto dura normalmente la fase fertil?",
                "Como puedo acompanar mejor a mi esposa?"
            ).forEach { q ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top) {
                    Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text(q, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp)
                }
            }
        }

        Button(
            onClick  = onGoToAiChat,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Outlined.Chat, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Abrir asistente IA", fontWeight = FontWeight.SemiBold)
        }
    }
}

// =============================================================================
// DIALOGS (reused from previous version)
// =============================================================================

@Composable
private fun IntercourseDialog(
    alreadyRegistered: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Favorite, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Text(if (alreadyRegistered) "Ya registrada" else "Registrar relacion intima genital",
                    style = MaterialTheme.typography.titleMedium)
            }
            Text(if (alreadyRegistered)
                "La relacion intima ya fue registrada hoy. Aparecera en la planilla de tu pareja."
            else
                "Esto registrara la marca I para la planilla de tu pareja en el dia de hoy.",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(if (alreadyRegistered) "Cerrar" else "Cancelar")
                }
                if (!alreadyRegistered) {
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Registrar I")
                    }
                }
            }
        }
    }
}

@Composable
private fun DoublePeakDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Preguntas del Doble Pico — P+3", style = MaterialTheme.typography.titleMedium)
            Text("Hoy es el dia 3 post-Pico. Respondan juntos:", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)
            listOf(
                "1. Desde el ultimo Pico hasta hoy: hubo algun dia de sequedad completa?" to
                        "Si hubo al menos 1 dia completamente seco, el Pico fue verdadero.",
                "2. La fase post-Pico tiene mas de 16 dias sin menstruacion?" to
                        "Si es asi, puede anticiparse un Doble Pico. Consultar al Profesional."
            ).forEach { (q, note) ->
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Emerald200.copy(0.3f)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(q, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                    Text(note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)) {
                Text("Entendido")
            }
        }
    }
}