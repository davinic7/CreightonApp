package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
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
import com.devdavinic.creightonapp.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// REGISTRATION SCREEN v4 — Empathetic conversational flow
//
// Stamp colors are ALWAYS fixed per NaProTRACKING standard:
//   RED        = #EF4444  (any bleeding)
//   GREEN      = #34D399  (dry/infertile)
//   WHITE+baby = White    (mucus/fertile)
//   GREEN+baby = #A7F3D0  (post-peak dry)
//
// Flow: BLEEDING? → SENSATION? → MUCUS? → [COLOR+CONSISTENCY] → MARKERS → CONFIRM
// Follows CCCS order: Color, Consistency, Change (sensation), Sensation
// =============================================================================

// Fixed stamp colors — NEVER change with skin
private val STAMP_RED         = Color(0xFFEF4444)
private val STAMP_RED_BORDER  = Color(0xFFDC2626)
private val STAMP_GREEN       = Color(0xFF34D399)
private val STAMP_GREEN_BORDER= Color(0xFF059669)
private val STAMP_WHITE       = Color.White
private val STAMP_WHITE_BORDER= Color(0xFF34D399)
private val STAMP_LGREEN      = Color(0xFFA7F3D0)
private val STAMP_DARK_TEXT   = Color(0xFF065F46)

private enum class RegStep {
    Q_BLEEDING,    // "¿Hubo sangrado hoy?"
    Q_SENSATION,   // "¿Cómo te sentiste?"
    Q_MUCUS,       // "¿Viste moco?"
    Q_MUCUS_DETAIL,// Color + Consistencia (CCCS)
    Q_MARKERS,     // Día Pico, I, AM
    CONFIRM
}

@Composable
fun RegistrationScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val dateText = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es"))
        .format(Date()).replaceFirstChar { it.uppercase() }

    val cycleRecords      by viewModel.currentCycleRecords.collectAsState()
    val todayPartial      by viewModel.todayPartial.collectAsState()
    val alreadyRegistered by viewModel.todayAlreadyRegistered.collectAsState()
    val peakSuggestion    by viewModel.peakDaySuggestion.collectAsState()

    // ── State ─────────────────────────────────────────────────────────────────
    var step        by remember { mutableStateOf(RegStep.Q_BLEEDING) }
    var forward     by remember { mutableStateOf(true) }
    var bleeding    by remember { mutableStateOf<BleedingLevel?>(null) }
    var sensation   by remember { mutableStateOf<Sensation?>(null) }
    var lubrication by remember { mutableStateOf<LubricationSensation?>(null) }
    var consistency by remember { mutableStateOf<MucusConsistency?>(null) }
    var mucusColor  by remember { mutableStateOf<MucusColor?>(null) }
    var hasMucus    by remember { mutableStateOf<Boolean?>(null) }
    var isPeakDay   by remember { mutableStateOf(false) }
    var hasI        by remember { mutableStateOf(todayPartial?.hasIntercourse ?: false) }
    var hasAM       by remember { mutableStateOf(todayPartial?.breastSelfExam ?: false) }
    val baseCount   = todayPartial?.observationCount ?: 0
    val cycleDay    = cycleRecords.size + 1
    
    val isPeakCandidate = lubrication != null ||
            consistency == MucusConsistency.STRETCHY ||
            mucusColor == MucusColor.CLEAR ||
            mucusColor == MucusColor.CLOUDY_CLEAR
    LaunchedEffect(isPeakCandidate) { if (!isPeakCandidate) isPeakDay = false }

    // Auto-suggest peak
    val autoSuggestPeak = peakSuggestion != null

    // Frequency
    val freq = when {
        sensation == Sensation.DRY && lubrication == null && hasMucus == false ->
            ObservationFrequency.AD
        baseCount + 1 >= 4 -> ObservationFrequency.AD
        baseCount + 1 == 3 -> ObservationFrequency.X3
        baseCount + 1 == 2 -> ObservationFrequency.X2
        else               -> ObservationFrequency.X1
    }

    // Preview
    val officialCode = CreightonLogic.buildOfficialCode(
        bleeding, sensation, consistency, mucusColor, lubrication, freq, isPeakDay)
    val stampType = CreightonLogic.computeStamp(
        bleeding, lubrication, consistency, mucusColor, sensation, 0)

    // ── Navigation ────────────────────────────────────────────────────────────
    fun goTo(s: RegStep, fwd: Boolean = true) { forward = fwd; step = s }

    fun next() = when (step) {
        RegStep.Q_BLEEDING -> goTo(
            if (bleeding == BleedingLevel.H || bleeding == BleedingLevel.M) RegStep.Q_MARKERS
            else RegStep.Q_SENSATION
        )
        RegStep.Q_SENSATION -> goTo(
            if (lubrication != null) RegStep.Q_MARKERS
            else RegStep.Q_MUCUS
        )
        RegStep.Q_MUCUS -> goTo(
            if (hasMucus == true) RegStep.Q_MUCUS_DETAIL
            else RegStep.Q_MARKERS
        )
        RegStep.Q_MUCUS_DETAIL -> goTo(RegStep.Q_MARKERS)
        RegStep.Q_MARKERS      -> goTo(RegStep.CONFIRM)
        RegStep.CONFIRM        -> {}
    }

    fun back() = when (step) {
        RegStep.Q_BLEEDING     -> {}
        RegStep.Q_SENSATION    -> goTo(RegStep.Q_BLEEDING, false)
        RegStep.Q_MUCUS        -> goTo(RegStep.Q_SENSATION, false)
        RegStep.Q_MUCUS_DETAIL -> goTo(RegStep.Q_MUCUS, false)
        RegStep.Q_MARKERS      -> when {
            bleeding == BleedingLevel.H || bleeding == BleedingLevel.M ->
                goTo(RegStep.Q_BLEEDING, false)
            hasMucus == true  -> goTo(RegStep.Q_MUCUS_DETAIL, false)
            lubrication != null -> goTo(RegStep.Q_SENSATION, false)
            else              -> goTo(RegStep.Q_MUCUS, false)
        }
        RegStep.CONFIRM        -> goTo(RegStep.Q_MARKERS, false)
    }

    // Step index for progress
    val stepIndex = when (step) {
        RegStep.Q_BLEEDING, RegStep.Q_SENSATION -> 0
        RegStep.Q_MUCUS, RegStep.Q_MUCUS_DETAIL -> 1
        RegStep.Q_MARKERS                        -> 2
        RegStep.CONFIRM                          -> 3
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(
            Color(0xFFFDE8F0), Color(0xFFEDE9FE), Color(0xFFD1FAE5))))) {

        Column(modifier = Modifier.fillMaxSize()) {

            RegHeader(dateText, stepIndex, todayPartial != null, baseCount, onClose)
            StampPreviewBar(stampType, officialCode, isPeakDay)

            // Partial banner
            if (todayPartial != null) {
                PartialBanner(baseCount, todayPartial!!.officialCode)
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState  = step,
                    transitionSpec = {
                        val d = if (forward) 1 else -1
                        (slideInHorizontally(tween(260)) { d * it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(200)) { -d * it } + fadeOut(tween(160)))
                    },
                    label = "regStep",
                    modifier = Modifier.fillMaxSize()
                ) { s ->
                    Column(modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        when (s) {
                            RegStep.Q_BLEEDING ->
                                QuestionBleeding(bleeding, cycleDay) { v ->
                                    bleeding = v
                                    forward  = true
                                    step = if (v == BleedingLevel.H || v == BleedingLevel.M)
                                        RegStep.Q_MARKERS else RegStep.Q_SENSATION
                                }
                            RegStep.Q_SENSATION ->
                                QuestionSensation(sensation, lubrication, bleeding) { s2, l ->
                                    sensation   = s2; lubrication = l
                                    if (l != null) { hasMucus = false; consistency = null; mucusColor = null }
                                }
                            RegStep.Q_MUCUS ->
                                QuestionMucus(hasMucus) { v ->
                                    hasMucus = v
                                    if (!v) { consistency = null; mucusColor = null }
                                    forward = true
                                    step = if (v) RegStep.Q_MUCUS_DETAIL else RegStep.Q_MARKERS
                                }
                            RegStep.Q_MUCUS_DETAIL ->
                                QuestionMucusDetail(consistency, mucusColor,
                                    { consistency = it }, { mucusColor = it })
                            RegStep.Q_MARKERS ->
                                QuestionMarkers(
                                    isPeakCandidate, isPeakDay, hasI, hasAM,
                                    cycleDay, autoSuggestPeak, peakSuggestion?.officialCode,
                                    { isPeakDay = it }, { hasI = it }, { hasAM = it }
                                )
                            RegStep.CONFIRM ->
                                QuestionConfirm(
                                    bleeding, sensation, consistency, mucusColor,
                                    lubrication, isPeakDay, hasI, hasAM,
                                    officialCode, stampType, freq, baseCount + 1
                                )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            RegFooter(step,
                onBack    = { back() },
                onNext    = { next() },
                onPartial = {
                    viewModel.savePartialObservation(
                        bleeding, sensation, consistency, mucusColor, lubrication, hasI, hasAM)
                    onClose()
                },
                onSave    = {
                    viewModel.saveFinalRecord(
                        bleeding, sensation, consistency, mucusColor, lubrication,
                        isPeakDay, hasI, hasAM, baseCount + 1)
                    if (autoSuggestPeak) viewModel.confirmPeakDayYesterday()
                    onClose()
                }
            )
        }

        if (alreadyRegistered) {
            Dialog(onDismissRequest = { viewModel.resetTodayAlreadyRegistered(); onClose() }) {
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Ya registraste hoy", style = MaterialTheme.typography.titleMedium)
                    Text("Solo se permite un registro final por dia en modo normal.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { viewModel.resetTodayAlreadyRegistered(); onClose() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)) {
                        Text("Entendido")
                    }
                }
            }
        }
    }
}

@Composable
private fun RegHeader(date: String, stepIndex: Int, isPartial: Boolean,
                      obsCount: Int, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()
        .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(date, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary)
                Text("Registro de hoy", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onClose, modifier = Modifier.background(
                MaterialTheme.colorScheme.surface.copy(0.6f), CircleShape)) {
                Icon(Icons.Outlined.Close, null)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                val active = i == stepIndex
                Box(modifier = Modifier.height(6.dp).weight(1f)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(0.12f)))
            }
        }
    }
}

@Composable
private fun StampPreviewBar(stampType: StampType, code: String, isPeak: Boolean) {
    val (bg, border, txt) = when (stampType) {
        StampType.RED         -> Triple(STAMP_RED,    STAMP_RED_BORDER,   Color.White)
        StampType.GREEN_SOLID -> Triple(STAMP_GREEN,  STAMP_GREEN_BORDER, Color.White)
        StampType.WHITE_BABY  -> Triple(STAMP_WHITE,  STAMP_WHITE_BORDER, STAMP_DARK_TEXT)
        StampType.GREEN_BABY  -> Triple(STAMP_LGREEN, STAMP_WHITE_BORDER, STAMP_DARK_TEXT)
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(bg.copy(0.1f)).border(1.dp, border.copy(0.2f), RoundedCornerShape(12.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            .background(bg).border(1.5.dp, border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center) {
            if (isPeak) Text("P", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = txt)
            if (stampType == StampType.WHITE_BABY || stampType == StampType.GREEN_BABY)
                Text("👶", fontSize = 12.sp)
        }
        Text("Vista previa: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(code, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PartialBanner(count: Int, lastCode: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFFFEF3C7)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.History, null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
        Text("Llevas $count observaciones hoy. La mas fertil fue: $lastCode",
            fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
    }
}

// =============================================================================
// Q1: BLEEDING
// =============================================================================

@Composable
private fun QuestionBleeding(current: BleedingLevel?, cycleDay: Int, onSelect: (BleedingLevel?) -> Unit) {
    QuestionContainer(
        question = "¿Hubo algún sangrado hoy?",
        hint     = "Día $cycleDay del ciclo",
        icon     = Icons.Outlined.WaterDrop,
        color    = STAMP_RED
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // NO bleeding
            OutlinedButton(
                onClick  = { onSelect(null) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(if (current == null) 2.dp else 1.dp,
                    if (current == null) STAMP_GREEN_BORDER else MaterialTheme.colorScheme.outlineVariant),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (current == null) STAMP_GREEN.copy(0.1f) else Color.White.copy(0.6f))
            ) {
                Text("No, nada de sangrado", color = if (current == null) STAMP_GREEN_BORDER else MaterialTheme.colorScheme.onSurface)
                if (current == null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp), tint = STAMP_GREEN_BORDER)
                }
            }

            Text("Sí, nivel de flujo:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BleedingChip(BleedingLevel.H, current == BleedingLevel.H, Modifier.weight(1f)) { onSelect(BleedingLevel.H) }
                BleedingChip(BleedingLevel.M, current == BleedingLevel.M, Modifier.weight(1f)) { onSelect(BleedingLevel.M) }
                BleedingChip(BleedingLevel.L, current == BleedingLevel.L, Modifier.weight(1f)) { onSelect(BleedingLevel.L) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BleedingChip(BleedingLevel.VL, current == BleedingLevel.VL, Modifier.weight(1f)) { onSelect(BleedingLevel.VL) }
                BleedingChip(BleedingLevel.B,  current == BleedingLevel.B,  Modifier.weight(1f)) { onSelect(BleedingLevel.B) }
            }
        }
    }
}

// =============================================================================
// Q2: SENSATION
// =============================================================================

@Composable
private fun QuestionSensation(
    current: Sensation?, lubrication: LubricationSensation?,
    bleeding: BleedingLevel?, onSelect: (Sensation?, LubricationSensation?) -> Unit
) {
    QuestionContainer(
        question = "¿Cómo fue la sensación vulvar?",
        hint     = "Al caminar o al limpiarte con el papel",
        icon     = Icons.Outlined.Waves,
        color    = Color(0xFF6366F1)
    ) {
        // High fertility first
        Text("Signos de Lubricación (Muy fértil):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))

        val lubOptions = listOf(
            LubricationSensation.VERY_WET_LUB to "10WL — Mojado con mucha lubricacion",
            LubricationSensation.SHINY_LUB    to "10SL — Brillo con lubricacion",
            LubricationSensation.WET_LUB      to "10DL — Humedo con lubricacion"
        )
        lubOptions.forEach { (l, desc) ->
            ConvOption(l.description, desc, lubrication == l, STAMP_GREEN_BORDER, Icons.Outlined.AutoAwesome) {
                onSelect(Sensation.WET, l)
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
        Spacer(Modifier.height(8.dp))

        Text("Otras Sensaciones:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))

        val sensOptions = listOf(
            Sensation.VERY_WET to "2W — Mojado (sin lubricacion)",
            Sensation.WET      to "2 — Humedo (sin lubricacion)",
            Sensation.SHINY    to "4 — Brillo (sin lubricacion)",
            Sensation.DRY      to "0 — Seco"
        )
        sensOptions.forEach { (s, desc) ->
            ConvOption(s.description, desc, current == s && lubrication == null, Color(0xFF6366F1),
                if (s == Sensation.DRY) Icons.Outlined.WbSunny else Icons.Outlined.Opacity) {
                onSelect(s, null)
            }
            Spacer(Modifier.height(4.dp))
        }
        InfoNote(Color(0xFF2563EB), Color(0xFFEFF6FF),
            "Si hay moco, la sensacion queda implícita en el codigo — no se registra por separado.")
    }
}

// =============================================================================
// Q3: MUCUS YES/NO
// =============================================================================

@Composable
private fun QuestionMucus(hasMucus: Boolean?, onSelect: (Boolean) -> Unit) {
    QuestionContainer(
        question = "¿Viste moco cervical?",
        hint     = "Moco visible en el papel al limpiarte",
        icon     = Icons.Outlined.Science,
        color    = Color(0xFF2563EB)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // YES — shown first (more fertile)
            Column(modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (hasMucus == true) Color(0xFF2563EB).copy(0.1f)
                else Color.White.copy(0.65f))
                .border(if (hasMucus == true) 2.dp else 1.dp,
                    if (hasMucus == true) Color(0xFF2563EB)
                    else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp))
                .clickable { onSelect(true) }
                .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💧", fontSize = 32.sp)
                Text("Sí, vi moco", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (hasMucus == true) Color(0xFF2563EB)
                    else MaterialTheme.colorScheme.onSurface)
                Text("Pegar o elástico en el papel",
                    fontSize = 10.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // NO
            Column(modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (hasMucus == false) STAMP_GREEN_BORDER.copy(0.1f)
                else Color.White.copy(0.65f))
                .border(if (hasMucus == false) 2.dp else 1.dp,
                    if (hasMucus == false) STAMP_GREEN_BORDER
                    else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp))
                .clickable { onSelect(false) }
                .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("☀️", fontSize = 32.sp)
                Text("No, sin moco", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (hasMucus == false) STAMP_GREEN_BORDER
                    else MaterialTheme.colorScheme.onSurface)
                Text("Papel seco o sin moco visible",
                    fontSize = 10.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// =============================================================================
// Q4: MUCUS DETAIL — CCCS order: Color, Consistency, Change, Sensation
// =============================================================================

@Composable
private fun QuestionMucusDetail(
    consistency: MucusConsistency?,
    mucusColor: MucusColor?,
    onConsistency: (MucusConsistency?) -> Unit,
    onColor: (MucusColor?) -> Unit
) {
    QuestionContainer(
        question = "¿Cómo era el moco?",
        hint     = "Primero el color, luego la elasticidad (orden CCCS del manual)",
        icon     = Icons.Outlined.Layers,
        color    = Color(0xFF2563EB)
    ) {
        // C — COLOR first (CCCS order)
        Text("1. Color / Apariencia:", fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))

        // Colors from LEAST to MOST fertile
        val mucusColorOptions: Map<MucusColor, String> = mapOf(
            MucusColor.YELLOW       to "Amarillo — Y",
            MucusColor.BROWN        to "Marron / Cafe — B",
            MucusColor.CLOUDY       to "Nublado / Blanco — C",
            MucusColor.CLOUDY_CLEAR to "Nublado-Transparente — C/K (Tipo Pico)",
            MucusColor.CLEAR        to "Transparente — K (Tipo Pico)"
        )
        mucusColorOptions.forEach { (mc, desc) ->
            val isPeak = mc == MucusColor.CLEAR || mc == MucusColor.CLOUDY_CLEAR
            val isSel  = mucusColor == mc
            ConvOption(
                label      = mc.description,
                sublabel   = desc,
                isSelected = isSel,
                color      = if (isPeak) STAMP_GREEN_BORDER else Color(0xFF2563EB),
                icon       = Icons.Outlined.Circle
            ) { onColor(if (isSel) null else mc) }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))

        // C — CONSISTENCY second (from least to most fertile)
        Text("2. Consistencia / Elasticidad:", fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))

        val consistencyOptions: Map<MucusConsistency, String> = mapOf(
            MucusConsistency.STICKY   to "6 — Pegajoso, se rompe en menos de 0.5 cm",
            MucusConsistency.TACKY    to "8 — Ligoso, se estira 1 a 2 cm",
            MucusConsistency.STRETCHY to "10 — Elastico mas de 2.5 cm (Tipo Pico)"
        )
        consistencyOptions.forEach { (cons, desc) ->
            val isPeak = cons == MucusConsistency.STRETCHY
            val isSel  = consistency == cons
            ConvOption(
                label      = cons.description,
                sublabel   = desc,
                isSelected = isSel,
                color      = if (isPeak) STAMP_GREEN_BORDER else Color(0xFF2563EB),
                icon       = Icons.Outlined.LinearScale
            ) { onConsistency(if (isSel) null else cons) }
            Spacer(Modifier.height(4.dp))
        }

        if (consistency == MucusConsistency.STRETCHY ||
            mucusColor == MucusColor.CLEAR ||
            mucusColor == MucusColor.CLOUDY_CLEAR) {
            Spacer(Modifier.height(4.dp))
            InfoNote(STAMP_GREEN_BORDER, Color(0xFFECFDF5),
                "Moco Tipo Pico detectado — posible Dia Pico.")
        }
    }
}

// =============================================================================
// Q5: MARKERS
// =============================================================================

@Composable
private fun QuestionMarkers(
    isPeakCandidate: Boolean, isPeakDay: Boolean,
    hasI: Boolean, hasAM: Boolean, cycleDay: Int,
    autoSuggestPeak: Boolean, yesterdayCode: String?,
    onPeak: (Boolean) -> Unit, onI: (Boolean) -> Unit, onAM: (Boolean) -> Unit
) {
    QuestionContainer(
        question = "¿Algo más para marcar?",
        hint     = "Marcadores opcionales del día",
        icon     = Icons.Outlined.Flag,
        color    = MaterialTheme.colorScheme.primary
    ) {
        // Auto-suggest Peak
        AnimatedVisibility(visible = autoSuggestPeak && yesterdayCode != null) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF7C3AED).copy(0.08f))
                    .border(2.dp, Color(0xFF7C3AED), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color(0xFF7C3AED).copy(0.15f)),
                        contentAlignment = Alignment.Center) {
                        Text("P", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sugerencia del sistema", fontWeight = FontWeight.Medium,
                            fontSize = 13.sp, color = Color(0xFF7C3AED))
                        Text("Ayer ($yesterdayCode) era Tipo Pico y hoy bajo. Ayer posiblemente fue el Dia Pico.",
                            fontSize = 11.sp, color = Color(0xFF7C3AED).copy(0.85f),
                            lineHeight = 14.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Peak Day
        AnimatedVisibility(visible = isPeakCandidate || autoSuggestPeak) {
            Column {
                MarkerRow(isPeakDay, onPeak,
                    "Dia Pico — P",
                    "Ultimo dia de moco tipo Pico. Activa P+1, P+2, P+3.",
                    Color(0xFF7C3AED),
                    highlighted = autoSuggestPeak && !isPeakDay)
                Spacer(Modifier.height(8.dp))
            }
        }
        if (!isPeakCandidate && !autoSuggestPeak) {
            InfoNote(MaterialTheme.colorScheme.onSurfaceVariant, Color(0xFFF8FAFC),
                "Dia Pico aplica solo con moco tipo Pico. El sistema lo sugiere automaticamente al dia siguiente.")
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
        Spacer(Modifier.height(8.dp))

        MarkerRow(hasI, onI,
            "Relacion intima genital — I",
            "Se anota con I debajo de la estampa en la planilla.")
        Spacer(Modifier.height(8.dp))
        MarkerRow(hasAM, onAM,
            "Autoexamen mamario — AM",
            if (cycleDay == 7) "Hoy es dia 7 — el manual recomienda hacerlo hoy."
            else "El manual recomienda realizarlo el dia 7 del ciclo.",
            accentColor = if (cycleDay == 7) Color(0xFFDB2777) else MaterialTheme.colorScheme.primary,
            highlighted = cycleDay == 7)
    }
}

// =============================================================================
// CONFIRM
// =============================================================================

@Composable
private fun QuestionConfirm(
    bleeding: BleedingLevel?, sensation: Sensation?,
    consistency: MucusConsistency?, mucusColor: MucusColor?,
    lubrication: LubricationSensation?, isPeakDay: Boolean,
    hasI: Boolean, hasAM: Boolean,
    code: String, stampType: StampType,
    freq: ObservationFrequency, obsCount: Int
) {
    val (stampBg, stampBorder, stampTxt) = when (stampType) {
        StampType.RED         -> Triple(STAMP_RED,    STAMP_RED_BORDER,   Color.White)
        StampType.GREEN_SOLID -> Triple(STAMP_GREEN,  STAMP_GREEN_BORDER, Color.White)
        StampType.WHITE_BABY  -> Triple(STAMP_WHITE,  STAMP_WHITE_BORDER, STAMP_DARK_TEXT)
        StampType.GREEN_BABY  -> Triple(STAMP_LGREEN, STAMP_WHITE_BORDER, STAMP_DARK_TEXT)
    }

    QuestionContainer(
        question = "Todo listo — revisá antes de guardar",
        hint     = "Este es el registro final del dia",
        icon     = Icons.Outlined.CheckCircle,
        color    = STAMP_GREEN_BORDER
    ) {
        // Big stamp + code
        Row(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(stampBg.copy(0.12f))
            .border(1.5.dp, stampBorder.copy(0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                .background(stampBg).border(3.dp, stampBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isPeakDay) Text("P", fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = stampTxt)
                    if (stampType == StampType.WHITE_BABY || stampType == StampType.GREEN_BABY)
                        Text("👶", fontSize = 20.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("CODIGO OFICIAL", fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(code, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${freq.code} — observado $obsCount vez hoy",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Summary
        Column(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.6f)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (bleeding != null)    SumRow("Sangrado",     "${bleeding.code} — ${bleeding.description}")
            if (lubrication != null) SumRow("Lubricacion",  "${lubrication.code} — Tipo Pico")
            if (mucusColor != null)  SumRow("Color",        "${mucusColor.code} — ${mucusColor.description}")
            if (consistency != null) SumRow("Consistencia", "${consistency.code} — ${consistency.description}")
            if (sensation != null)   SumRow("Sensacion",    "${sensation.code} — ${sensation.description}")
            if (isPeakDay) SumRow("Dia Pico", "Si — activa P+1, P+2, P+3", Color(0xFF7C3AED))
            if (hasI)      SumRow("Intercurso", "Si — I en la planilla")
            if (hasAM)     SumRow("Autoexamen", "Si — AM registrado")
        }
    }
}

// =============================================================================
// FOOTER
// =============================================================================

@Composable
private fun RegFooter(
    step: RegStep, onBack: () -> Unit, onNext: () -> Unit,
    onPartial: () -> Unit, onSave: () -> Unit
) {
    val isFinal = step == RegStep.CONFIRM
    Column(modifier = Modifier.fillMaxWidth()
        .background(Color.White.copy(0.5f)).navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {

        OutlinedButton(onClick = onPartial, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFEFCE8))) {
            Icon(Icons.Outlined.Schedule, null, tint = Color(0xFFD97706),
                modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Guardar observacion parcial", fontSize = 12.sp, color = Color(0xFF92400E))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step != RegStep.Q_BLEEDING) {
                OutlinedButton(onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Atras")
                }
            }
            Button(
                onClick  = if (isFinal) onSave else onNext,
                modifier = Modifier
                    .weight(if (step == RegStep.Q_BLEEDING) 2f else 1f)
                    .height(50.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFinal) STAMP_GREEN_BORDER
                    else MaterialTheme.colorScheme.primary)
            ) {
                if (isFinal) {
                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Continuar", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================

@Composable
private fun QuestionContainer(
    question: String, hint: String, icon: ImageVector, color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(46.dp).clip(CircleShape)
                .background(color.copy(0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(question, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, lineHeight = 23.sp)
                Text(hint, fontSize = 11.sp, lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun ConvOption(
    label: String, sublabel: String,
    isSelected: Boolean, color: Color,
    icon: ImageVector, onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.02f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "cScale")
    Row(modifier = Modifier.fillMaxWidth().scale(scale)
        .clip(RoundedCornerShape(14.dp))
        .background(if (isSelected) color.copy(0.09f) else Color.White.copy(0.65f))
        .border(if (isSelected) 2.dp else 1.dp,
            if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(14.dp))
        .clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(38.dp).clip(CircleShape)
            .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center) {
            Icon(icon, null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface)
            Text(sublabel, fontSize = 11.sp, lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = isSelected) {
            Icon(Icons.Outlined.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BleedingChip(
    level: BleedingLevel, isSelected: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.05f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "bScale")
    Box(contentAlignment = Alignment.Center,
        modifier = modifier.scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) STAMP_RED else Color.White.copy(0.65f))
            .border(2.dp, if (isSelected) STAMP_RED_BORDER
            else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(level.code, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
            Text(level.description, fontSize = 9.sp, textAlign = TextAlign.Center,
                color = if (isSelected) Color.White.copy(0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun MarkerRow(
    checked: Boolean, onChange: (Boolean) -> Unit,
    label: String, sublabel: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    highlighted: Boolean = false
) {
    val scale by animateFloatAsState(
        if (checked) 1.02f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "mScale")
    Row(modifier = Modifier.fillMaxWidth().scale(scale)
        .clip(RoundedCornerShape(14.dp))
        .background(if (checked) accentColor.copy(0.08f)
        else if (highlighted) Color(0xFFFEF3C7)
        else Color.White.copy(0.65f))
        .border(if (checked || highlighted) 2.dp else 1.dp,
            if (checked) accentColor
            else if (highlighted) Color(0xFFFBBF24)
            else MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(14.dp))
        .clickable { onChange(!checked) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = accentColor))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = if (checked) accentColor else MaterialTheme.colorScheme.onSurface)
            Text(sublabel, fontSize = 11.sp, lineHeight = 13.sp,
                color = if (highlighted && !checked) Color(0xFFD97706)
                else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoNote(iconColor: Color, bg: Color, text: String) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(bg).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Info, null, tint = iconColor, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 11.sp, color = iconColor, lineHeight = 15.sp)
    }
}

@Composable
private fun SumRow(label: String, value: String,
                   color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = color, textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f))
    }
}

// =============================================================================
// PEAK DAY SUGGESTION DIALOG — also used from PlanillaScreen
// =============================================================================

@Composable
fun PeakDaySuggestionDialog(
    yesterdayCode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0xFF7C3AED).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center) {
                    Text("P", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED))
                }
                Text("Sugerencia del sistema",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF7C3AED))
            }
            Text("Ayer ($yesterdayCode) tenia moco tipo Pico, pero hoy el signo bajo de calidad.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp)
            Text("Segun el manual, el Dia Pico es el ULTIMO dia de moco tipo Pico. Como hoy bajo, ayer posiblemente fue el Dia Pico.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp)
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFECFDF5)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Info, null, tint = STAMP_GREEN_BORDER,
                    modifier = Modifier.size(14.dp))
                Text("Si confirmas, el sistema marcara ayer como Dia Pico y activara P+1, P+2, P+3.",
                    fontSize = 11.sp, color = STAMP_DARK_TEXT, lineHeight = 15.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("No fue Pico") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = STAMP_GREEN_BORDER)) {
                    Text("Si, confirmar")
                }
            }
        }
    }
}
