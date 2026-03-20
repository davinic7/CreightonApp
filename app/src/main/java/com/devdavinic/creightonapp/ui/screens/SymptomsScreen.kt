package com.devdavinic.creightonapp.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// SYMPTOMS SCREEN
// Daily symptom logging + biomarker analysis based on Creighton manual
// =============================================================================

@Composable
fun SymptomsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val allSymptoms by viewModel.allSymptoms.collectAsState()
    val analysis    by viewModel.cycleAnalysis.collectAsState()

    var showLogDialog      by remember { mutableStateOf(false) }
    var biomarkerAlerts    by remember { mutableStateOf<List<BiomarkerAlert>>(emptyList()) }
    var selectedSymptom    by remember { mutableStateOf<SymptomType?>(null) }

    LaunchedEffect(allSymptoms) {
        biomarkerAlerts = viewModel.getBiomarkerAlerts()
    }

    val todayStr = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es"))
        .format(Date()).replaceFirstChar { it.uppercase() }
    val todayRecord = allSymptoms.firstOrNull {
        it.date / 86_400_000L == System.currentTimeMillis() / 86_400_000L
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(Emerald200, Purple100, Pink200)))) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Atras",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sintomas y Biomarcadores",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(todayStr, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showLogDialog = true }) {
                    Icon(Icons.Outlined.Add, "Registrar sintomas",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Today's log card
                TodaySymptomCard(
                    todayRecord = todayRecord,
                    cycleDay    = viewModel.cycleAnalysis.collectAsState().value?.cycleDay ?: 0,
                    onLog       = { showLogDialog = true }
                )

                // Biomarker alerts
                if (biomarkerAlerts.isNotEmpty()) {
                    BiomarkerAlertsSection(
                        alerts          = biomarkerAlerts,
                        onAlertClick    = { alert -> selectedSymptom = alert.type }
                    )
                }

                // Phase correlation hint
                analysis?.let { PhaseCorrelationCard(it) }

                // Recent history
                if (allSymptoms.isNotEmpty()) {
                    SymptomHistorySection(allSymptoms.take(7))
                }

                // Unusual bleeding warning
                val hasUnusual = allSymptoms.take(7).any { it.isUnusualBleeding }
                if (hasUnusual) {
                    UnusualBleedingWarning()
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Log symptoms dialog
        if (showLogDialog) {
            LogSymptomsDialog(
                existingRecord = todayRecord,
                onSave = { symptoms, notes ->
                    viewModel.saveSymptoms(symptoms, notes)
                    showLogDialog = false
                },
                onDismiss = { showLogDialog = false }
            )
        }

        // Biomarker detail dialog
        selectedSymptom?.let { type ->
            BiomarkerDetailDialog(type = type, onDismiss = { selectedSymptom = null })
        }
    }
}

// =============================================================================
// TODAY SYMPTOM CARD
// =============================================================================

@Composable
private fun TodaySymptomCard(
    todayRecord: DailySymptom?,
    cycleDay: Int,
    onLog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(alpha = 0.75f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Sintomas de hoy", fontWeight = FontWeight.Medium,
                    fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                if (cycleDay > 0) Text("Dia $cycleDay del ciclo", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onLog, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (todayRecord != null) "Editar" else "Registrar", fontSize = 13.sp)
            }
        }

        if (todayRecord == null) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp))
                Text("Sin registro de sintomas para hoy.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val symptoms = todayRecord.symptoms
            if (symptoms.isEmpty()) {
                Text("Sin sintomas registrados hoy.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SymptomChipsRow(symptoms)
            }
            if (todayRecord.notes.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Notes, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp))
                    Text(todayRecord.notes, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                }
            }
            if (todayRecord.isUnusualBleeding) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEF2F2)).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, null,
                        tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    Text("Sangrado inusual registrado — no reinicia el ciclo",
                        fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// =============================================================================
// BIOMARKER ALERTS
// =============================================================================

@Composable
private fun BiomarkerAlertsSection(
    alerts: List<BiomarkerAlert>,
    onAlertClick: (BiomarkerAlert) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Biomarcadores activos", fontWeight = FontWeight.Medium,
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        alerts.forEach { alert ->
            val (bg, border, color) = when (alert.severity) {
                AlertSeverity.WARNING, AlertSeverity.CRITICAL ->
                    Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFDC2626))
                else ->
                    Triple(Color(0xFFEFF6FF), Color(0xFF93C5FD), Color(0xFF2563EB))
            }
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(bg).border(1.dp, border, RoundedCornerShape(12.dp))
                .clickable { onAlertClick(alert) }.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Biotech, null, tint = color,
                    modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.title, fontWeight = FontWeight.Medium,
                        fontSize = 13.sp, color = color)
                    Text(alert.message, fontSize = 11.sp,
                        color = color.copy(alpha = 0.8f), lineHeight = 14.sp, maxLines = 2)
                }
                Icon(Icons.Outlined.ChevronRight, null,
                    tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =============================================================================
// PHASE CORRELATION CARD
// =============================================================================

@Composable
private fun PhaseCorrelationCard(analysis: CycleAnalysis) {
    val (title, body, color) = when (analysis.currentPhase) {
        CyclePhase.MENSTRUAL -> Triple(
            "Fase menstrual",
            "Es comun sentir fatiga, dolor pelvico y menor energia. El manual sugiere descanso y autocuidado en esta fase.",
            Color(0xFFDC2626)
        )
        CyclePhase.PRE_PEAK -> Triple(
            "Fase pre-Pico (estrogenica)",
            "El estrogeno en aumento puede mejorar el estado de animo y la energia. Es una fase de mayor vitalidad para muchas mujeres.",
            Color(0xFF2563EB)
        )
        CyclePhase.PEAK_DAY -> Triple(
            "Dia Pico",
            "Momento de maxima fertilidad. Algunas mujeres sienten el mittelschmerz (dolor a mitad del ciclo) asociado a la ovulacion.",
            Emerald600
        )
        CyclePhase.POST_PEAK_123, CyclePhase.POST_PEAK -> Triple(
            "Fase post-Pico (progesterogenica)",
            "La progesterona es dominante. Pueden aparecer tension mamaria y cambios de humor. El manchado premenstrual en esta fase es un biomarcador importante.",
            Color(0xFF7C3AED)
        )
        CyclePhase.UNKNOWN -> Triple(
            "Fase desconocida",
            "Registra mas dias para que el sistema pueda correlacionar tus sintomas con tu ciclo.",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(color.copy(alpha = 0.07f))
        .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Insights, null, tint = color, modifier = Modifier.size(18.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = color)
            Text(body, fontSize = 11.sp, color = color.copy(alpha = 0.85f), lineHeight = 15.sp)
        }
    }
}

// =============================================================================
// SYMPTOM HISTORY
// =============================================================================

@Composable
private fun SymptomHistorySection(recent: List<DailySymptom>) {
    val fmt = SimpleDateFormat("d MMM", Locale("es"))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ultimos 7 dias", fontWeight = FontWeight.Medium,
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        recent.forEach { record ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.6f)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmt.format(Date(record.date)), fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Dia ${record.cycleDay}", fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (record.symptoms.isEmpty()) {
                    Text("Sin sintomas", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    SymptomChipsRow(record.symptoms, compact = true)
                }
            }
        }
    }
}

// =============================================================================
// UNUSUAL BLEEDING WARNING
// =============================================================================

@Composable
private fun UnusualBleedingWarning() {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFFEF2F2))
        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.MedicalInformation, null,
            tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
        Column {
            Text("Sangrado inusual reciente", fontWeight = FontWeight.Medium,
                fontSize = 13.sp, color = Color(0xFFDC2626))
            Text("Registraste sangrado inusual recientemente. El manual indica consultar al medico. Este tipo de sangrado no reinicia el ciclo menstrual.",
                fontSize = 11.sp, color = Color(0xFFEF4444), lineHeight = 15.sp)
        }
    }
}

// =============================================================================
// SYMPTOM CHIPS
// =============================================================================

@Composable
private fun SymptomChipsRow(
    symptoms: Map<SymptomType, SymptomIntensity>,
    compact: Boolean = false
) {
    val grouped = symptoms.entries.groupBy { it.key.category }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        grouped.forEach { (cat, entries) ->
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("${cat.icon}", fontSize = if (compact) 10.sp else 12.sp)
                entries.forEach { (type, intensity) ->
                    val color = when (intensity) {
                        SymptomIntensity.MILD     -> Color(0xFF6B7280)
                        SymptomIntensity.MODERATE -> Color(0xFFD97706)
                        SymptomIntensity.SEVERE   -> Color(0xFFDC2626)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = if (compact) 6.dp else 8.dp,
                            vertical   = if (compact) 2.dp else 4.dp)) {
                        Text(
                            if (compact) type.label.take(10) else type.label,
                            fontSize = if (compact) 9.sp else 11.sp, color = color
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// LOG SYMPTOMS DIALOG
// =============================================================================

@Composable
private fun LogSymptomsDialog(
    existingRecord: DailySymptom?,
    onSave: (Map<SymptomType, SymptomIntensity>, String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSymptoms = existingRecord?.symptoms ?: emptyMap()
    val selected = remember { mutableStateMapOf<SymptomType, SymptomIntensity>().apply { putAll(initialSymptoms) } }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }
    var expandedCategory by remember { mutableStateOf<SymptomCategory?>(SymptomCategory.PHYSICAL) }

    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)) {

            // Header
            Column(modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(16.dp)) {
                Text("Registrar sintomas de hoy",
                    style = MaterialTheme.typography.titleMedium)
                Text("Selecciona los sintomas y su intensidad",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Category sections
                SymptomCategory.entries.forEach { category ->
                    val isExpanded = expandedCategory == category
                    val categorySymptoms = SymptomType.entries.filter { it.category == category }
                    val selectedCount = categorySymptoms.count { selected.containsKey(it) }

                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))) {

                        // Category header
                        Row(modifier = Modifier.fillMaxWidth()
                            .clickable { expandedCategory = if (isExpanded) null else category }
                            .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon, fontSize = 18.sp)
                            Text(category.label, fontWeight = FontWeight.Medium,
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f))
                            if (selectedCount > 0) {
                                Box(modifier = Modifier.clip(CircleShape)
                                    .background(Emerald600)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text("$selectedCount", fontSize = 11.sp,
                                        color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(
                                if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp))
                        }

                        // Symptoms list
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {

                                // Unusual bleeding warning
                                if (category == SymptomCategory.UNUSUAL_BLEEDING) {
                                    Row(modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7)).padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Outlined.Info, null,
                                            tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                        Text("El sangrado inusual NO reinicia el ciclo menstrual. Se registra como informacion clinica separada.",
                                            fontSize = 10.sp, color = Color(0xFF92400E), lineHeight = 14.sp)
                                    }
                                }

                                categorySymptoms.forEach { symptom ->
                                    val currentIntensity = selected[symptom]
                                    SymptomRow(
                                        symptom   = symptom,
                                        intensity = currentIntensity,
                                        onSelect  = { intensity ->
                                            if (intensity == null) selected.remove(symptom)
                                            else selected[symptom] = intensity
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes field
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notas adicionales (opcional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    minLines      = 2,
                    maxLines      = 4
                )
            }

            // Footer
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onSave(selected.toMap(), notes) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun SymptomRow(
    symptom: SymptomType,
    intensity: SymptomIntensity?,
    onSelect: (SymptomIntensity?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(
            if (intensity != null) Emerald200.copy(alpha = 0.3f) else Color.Transparent
        )
        .border(1.dp,
            if (intensity != null) Emerald600.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            RoundedCornerShape(8.dp))
        .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(symptom.label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (symptom.isBiomarker) {
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Biomarcador", fontSize = 8.sp, color = Color(0xFFD97706),
                        fontWeight = FontWeight.Medium)
                }
            }
            // Clear button
            if (intensity != null) {
                IconButton(onClick = { onSelect(null) },
                    modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Outlined.Close, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp))
                }
            }
        }

        // Intensity selector
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SymptomIntensity.entries.forEach { lvl ->
                val isSelected = intensity == lvl
                val color = when (lvl) {
                    SymptomIntensity.MILD     -> Color(0xFF059669)
                    SymptomIntensity.MODERATE -> Color(0xFFD97706)
                    SymptomIntensity.SEVERE   -> Color(0xFFDC2626)
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) color else Color(0xFFE2E8F0))
                    .clickable { onSelect(lvl) }.padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center) {
                    Text(lvl.label, fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// =============================================================================
// BIOMARKER DETAIL DIALOG
// =============================================================================

@Composable
private fun BiomarkerDetailDialog(type: SymptomType, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Biotech, null,
                        tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(type.label, style = MaterialTheme.typography.titleMedium)
                    Text("Biomarcador Creighton", fontSize = 10.sp,
                        color = Color(0xFFD97706))
                }
            }
            type.biomarkerNote?.let { note ->
                Text(note, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)
            }
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFEF3C7)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Warning, null,
                    tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                Text("Esta informacion es educativa. Consulta con tu Profesional de FertilityCare para interpretacion clinica.",
                    fontSize = 11.sp, color = Color(0xFF92400E), lineHeight = 15.sp)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cerrar")
            }
        }
    }
}