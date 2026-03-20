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
// MODULE 5 - SPICE EDUCATION & PHASE TIPS SCREEN
// Based on Chapters 2 and 8 of the Creighton manual
// =============================================================================

@Composable
fun SpiceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val analysis      by viewModel.cycleAnalysis.collectAsState()
    val cycleRecords  by viewModel.currentCycleRecords.collectAsState()

    val currentPhase = analysis?.currentPhase ?: CyclePhase.UNKNOWN
    val cycleDay     = analysis?.cycleDay ?: 1

    var selectedTab by remember { mutableStateOf(0) }
    // 0 = Tip del dia, 1 = S, 2 = P, 3 = I, 4 = C, 5 = E

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(Emerald200, Purple100, Pink200)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            SpiceHeader(onBack = onBack)

            // Phase context banner
            PhaseBanner(phase = currentPhase, cycleDay = cycleDay)

            // Tab selector
            SpiceTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        0    -> DailyTipContent(phase = currentPhase, cycleDay = cycleDay)
                        else -> DimensionContent(
                            dimension = SpiceDimension.entries[tab - 1],
                            phase     = currentPhase
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun SpiceHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteAlpha40)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, "Atras", tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = "S-P-I-C-E",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = "Educacion y Consejos por Fase",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

// =============================================================================
// PHASE CONTEXT BANNER
// =============================================================================

@Composable
private fun PhaseBanner(phase: CyclePhase, cycleDay: Int) {
    val (bg, accent, text) = when (phase) {
        CyclePhase.MENSTRUAL      -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "Fase Menstrual - Dia $cycleDay")
        CyclePhase.PRE_PEAK       -> Triple(Color(0xFFECFDF5), Emerald600,         "Fase Pre-Pico - Dia $cycleDay")
        CyclePhase.PEAK_DAY       -> Triple(Color(0xFFECFDF5), Emerald600,         "Dia Pico")
        CyclePhase.POST_PEAK_123  -> Triple(Color(0xFFECFDF5), Emerald600,         "Post-Pico dia ${phase.ordinal} de 3")
        CyclePhase.POST_PEAK      -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB),  "Fase Post-Pico - Dia $cycleDay")
        CyclePhase.UNKNOWN        -> Triple(WhiteAlpha60,     MaterialTheme.colorScheme.outline, "Sin datos de ciclo")
    }
    val phaseContext = when (phase) {
        CyclePhase.MENSTRUAL     -> "Los consejos de hoy se centran en acompanamiento y comprension mutua."
        CyclePhase.PRE_PEAK      -> "Fase fertil activa. Los consejos de hoy te ayudan a vivir este tiempo con profundidad."
        CyclePhase.PEAK_DAY      -> "Dia de maxima fertilidad. Moment unico para conectar con tu pareja."
        CyclePhase.POST_PEAK_123 -> "Cuenta post-Pico. Tres dias para vivir con consciencia e intimidad."
        CyclePhase.POST_PEAK     -> "Fase infertil. Tiempo ideal para profundizar la relacion sin presiones."
        CyclePhase.UNKNOWN       -> "Registra tu primer dia para recibir consejos personalizados."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("S-P-I-C-E", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent,
                textAlign = TextAlign.Center)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = accent)
            Text(phaseContext, fontSize = 11.sp, color = accent.copy(alpha = 0.8f), lineHeight = 15.sp)
        }
    }
}

// =============================================================================
// TAB ROW
// =============================================================================

@Composable
private fun SpiceTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Hoy") + SpiceDimension.entries.map { it.icon }
    val tabColors = listOf(
        MaterialTheme.colorScheme.primary,  // Hoy
        Color(0xFF7C3AED),   // S - purple
        Color(0xFF059669),   // P - green
        Color(0xFF2563EB),   // I - blue
        Color(0xFFD97706),   // C - amber
        Color(0xFFDB2777)    // E - pink
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            val color = tabColors[index]
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) color else WhiteAlpha60)
                    .border(1.5.dp,
                        if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp))
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    fontSize   = if (index == 0) 11.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isSelected) Color.White else color
                )
            }
        }
    }
}

// =============================================================================
// DAILY TIP CONTENT
// =============================================================================

@Composable
private fun DailyTipContent(phase: CyclePhase, cycleDay: Int) {
    val tip = remember(cycleDay, phase) {
        SpiceContent.getDailyTip(cycleDay, phase)
    }

    val phaseSpecificTips = remember(phase) {
        SpiceContent.getTipsForPhase(phase)
            .filter { it.applicablePhases.isNotEmpty() }
            .take(3)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Main daily tip card
        DailyTipCard(tip = tip)

        // What is SPICE explainer
        SpiceExplainerCard()

        // Phase-specific tips
        if (phaseSpecificTips.isNotEmpty()) {
            Text(
                "Para tu fase actual",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            phaseSpecificTips.forEach { phaseTip ->
                CompactTipCard(tip = phaseTip)
            }
        }
    }
}

@Composable
private fun DailyTipCard(tip: SpiceTip) {
    val dimColor = dimensionColor(tip.dimension)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WhiteAlpha60)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(dimColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tip.dimension.icon,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = dimColor
                )
            }
            Column {
                Text(
                    "Consejo del dia",
                    fontSize = 10.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    tip.dimension.label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = dimColor
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(dimColor.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("S-P-I-C-E", fontSize = 10.sp, color = dimColor, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            tip.title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )

        Text(
            tip.body,
            fontSize   = 14.sp,
            color      = MaterialTheme.colorScheme.onSurface,
            lineHeight = 21.sp
        )

        // Action suggestion
        tip.actionSuggestion?.let { action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(dimColor.copy(alpha = 0.08f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Lightbulb, null,
                    tint     = dimColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        "Accion de hoy",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color      = dimColor
                    )
                    Text(
                        action,
                        fontSize   = 13.sp,
                        color      = dimColor.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// SPICE EXPLAINER CARD
// =============================================================================

@Composable
private fun SpiceExplainerCard() {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteAlpha50)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.MenuBook, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    "Que es S-P-I-C-E",
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "El CREIGHTON MODEL System destaca que la sexualidad humana es cerebro-centrica, no genito-centrica. La verdadera intimidad se construye en cinco dimensiones.",
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 19.sp
                )
                SpiceDimension.entries.forEach { dim ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(dimensionColor(dim).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dim.icon, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = dimensionColor(dim))
                        }
                        Column {
                            Text(dim.label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = dimensionColor(dim))
                            Text(dimensionDescription(dim), fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp)
                        }
                    }
                }
                Text(
                    "Cap. 2 y 8 del manual Creighton MODEL FertilityCare System",
                    fontSize = 10.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// DIMENSION CONTENT
// =============================================================================

@Composable
private fun DimensionContent(dimension: SpiceDimension, phase: CyclePhase) {
    val tips = remember(dimension) {
        SpiceContent.getTipsByDimension(dimension)
    }
    val color = dimensionColor(dimension)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Dimension header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dimension.icon,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
            Column {
                Text(
                    dimension.label,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
                Text(
                    dimensionDescription(dimension),
                    fontSize   = 12.sp,
                    color      = color.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }

        // All tips for this dimension
        tips.forEach { tip ->
            FullTipCard(tip = tip, phase = phase)
        }
    }
}

@Composable
private fun FullTipCard(tip: SpiceTip, phase: CyclePhase) {
    val color         = dimensionColor(tip.dimension)
    val isApplicable  = tip.applicablePhases.isEmpty() || phase in tip.applicablePhases

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isApplicable) WhiteAlpha60
                else WhiteAlpha40.copy(alpha = 0.3f)
            )
            .border(
                width = if (isApplicable) 1.dp else 0.5.dp,
                color = if (isApplicable) WhiteAlpha40 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                tip.title,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                color      = if (isApplicable) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier   = Modifier.weight(1f)
            )
            if (isApplicable && tip.applicablePhases.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Para tu fase", fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
                }
            }
        }

        Text(
            tip.body,
            fontSize   = 13.sp,
            color      = if (isApplicable) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 19.sp
        )

        tip.actionSuggestion?.let { action ->
            if (isApplicable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.07f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Lightbulb, null,
                        tint = color, modifier = Modifier.size(16.dp))
                    Text(action, fontSize = 12.sp, color = color.copy(alpha = 0.9f),
                        lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactTipCard(tip: SpiceTip) {
    val color = dimensionColor(tip.dimension)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WhiteAlpha60)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(tip.dimension.icon, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(tip.title, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text(tip.body, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp,
                maxLines = 2)
        }
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Composable
private fun dimensionColor(dimension: SpiceDimension): Color = when (dimension) {
    SpiceDimension.SPIRITUAL    -> Color(0xFF7C3AED)
    SpiceDimension.PHYSICAL     -> Color(0xFF059669)
    SpiceDimension.INTELLECTUAL -> Color(0xFF2563EB)
    SpiceDimension.CREATIVE     -> Color(0xFFD97706)
    SpiceDimension.EMOTIONAL    -> Color(0xFFDB2777)
}

private fun dimensionDescription(dimension: SpiceDimension): String = when (dimension) {
    SpiceDimension.SPIRITUAL    -> "La oracion, los valores y la apertura a la vida como fundamento de la relacion."
    SpiceDimension.PHYSICAL     -> "La cercania, los abrazos que afirman y la intimidad sin presion genital."
    SpiceDimension.INTELLECTUAL -> "Comprender juntos el ciclo, las hormonas y las intenciones del metodo."
    SpiceDimension.CREATIVE     -> "Gestos, proyectos y formas creativas de expresar el amor."
    SpiceDimension.EMOTIONAL    -> "La comunicacion profunda, la apertura y el humor que construye el vinculo."
}