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
// TEST SIMULATOR SCREEN
// Only available in test mode profiles
// Allows: 1) Auto-generate realistic cycle data  2) Register on any past date
// =============================================================================

@Composable
fun TestSimulatorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val allRecords by viewModel.allRecords.collectAsState()
    val analysis   by viewModel.cycleAnalysis.collectAsState()

    var showGenerateDialog  by remember { mutableStateOf(false) }
    var showClearDialog     by remember { mutableStateOf(false) }
    var generateSuccess     by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFFFEF3C7), Color(0xFFD1FAE5))))) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Atras", tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Simulador de Testeo", style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFD97706))
                    Text("Solo disponible en perfil de testeo",
                        fontSize = 11.sp, color = Color(0xFFD97706).copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(48.dp))
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Warning banner
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(14.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.BugReport, null, tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp))
                    Column {
                        Text("Modo testeo activo", fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, color = Color(0xFF92400E))
                        Text("Los registros generados aqui estan marcados como test y no afectan un uso real de la app.",
                            fontSize = 12.sp, color = Color(0xFFD97706), lineHeight = 16.sp)
                    }
                }

                // Status card
                StatusCard(allRecords, analysis)

                // Prediction confidence card
                analysis?.let { PredictionConfidenceCard(it) }

                // Action: Generate cycles
                ActionCard(
                    icon        = Icons.Outlined.AutoAwesome,
                    title       = "Generar ciclos de ejemplo",
                    description = "Genera 3, 6 o 12 ciclos completos con patron realista del Modelo Creighton. Incluye sangrado, fase seca, ciclo de moco y Dia Pico.",
                    accentColor = Emerald600,
                    onClick     = { showGenerateDialog = true }
                )

                // Action: Regenerate with different cycle count
                ActionCard(
                    icon        = Icons.Outlined.Refresh,
                    title       = "Regenerar datos de ejemplo",
                    description = "Vuelve a generar los ciclos con el patron base. Util para resetear y probar desde cero.",
                    accentColor = Color(0xFF2563EB),
                    onClick     = { showGenerateDialog = true }
                )

                // Action: Clear all
                ActionCard(
                    icon        = Icons.Outlined.DeleteSweep,
                    title       = "Limpiar todos los registros",
                    description = "Elimina todos los registros de prueba de este perfil para empezar desde cero.",
                    accentColor = Color(0xFFDC2626),
                    isDestructive = true,
                    onClick     = { showClearDialog = true }
                )

                if (generateSuccess) {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Emerald600, RoundedCornerShape(12.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Emerald600,
                            modifier = Modifier.size(18.dp))
                        Text("Ciclos generados. Ve al Calendario o Prediccion para ver los resultados.",
                            fontSize = 13.sp, color = Color(0xFF065F46), lineHeight = 17.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Generate dialog
        if (showGenerateDialog) {
            GenerateCyclesDialog(
                onGenerate = { n ->
                    viewModel.generateTestCycles(n)
                    generateSuccess = true
                    showGenerateDialog = false
                },
                onDismiss = { showGenerateDialog = false }
            )
        }


        // Clear dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Limpiar registros") },
                text  = { Text("Esto elimina todos los registros de este perfil. No se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAllRecords()
                        generateSuccess = false
                        showClearDialog = false
                    }) { Text("Limpiar", color = Color(0xFFDC2626)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// =============================================================================
// STATUS CARD
// =============================================================================

@Composable
private fun StatusCard(records: List<DailyRecord>, analysis: CycleAnalysis?) {
    val fmt = SimpleDateFormat("d MMM yyyy", Locale("es"))
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(alpha = 0.7f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text("Estado del historial", fontWeight = FontWeight.Medium, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("Registros", "${records.size}", Emerald600, Modifier.weight(1f))
            StatChip("Ciclos", "${analysis?.totalCyclesRecorded ?: 0}", Color(0xFF2563EB), Modifier.weight(1f))
            StatChip("Para predic.", "3 min.", Color(0xFFD97706), Modifier.weight(1f))
        }

        if (records.isNotEmpty()) {
            val oldest = records.minByOrNull { it.date }
            val newest = records.maxByOrNull { it.date }
            Text("Desde ${oldest?.date?.let { fmt.format(Date(it)) } ?: "-"} hasta ${newest?.date?.let { fmt.format(Date(it)) } ?: "-"}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(color.copy(alpha = 0.08f))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
    }
}

// =============================================================================
// PREDICTION CONFIDENCE CARD
// =============================================================================

@Composable
private fun PredictionConfidenceCard(analysis: CycleAnalysis) {
    val confidence = analysis.predictionConfidence
    val (bg, border, color) = when (confidence) {
        PredictionConfidence.INSUFFICIENT_DATA ->
            Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFDC2626))
        PredictionConfidence.LOW ->
            Triple(Color(0xFFFEF3C7), Color(0xFFFBBF24), Color(0xFFD97706))
        PredictionConfidence.MODERATE ->
            Triple(Color(0xFFEFF6FF), Color(0xFF93C5FD), Color(0xFF2563EB))
        PredictionConfidence.HIGH ->
            Triple(Color(0xFFECFDF5), Color(0xFF86EFAC), Emerald600)
    }
    val completedCycles = analysis.totalCyclesRecorded
        .let { if (it > 0) it - 1 else 0 }

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(bg).border(1.dp, border, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(when (confidence) {
                PredictionConfidence.INSUFFICIENT_DATA -> Icons.Outlined.Lock
                PredictionConfidence.LOW               -> Icons.Outlined.LockOpen
                PredictionConfidence.MODERATE          -> Icons.Outlined.TrendingUp
                PredictionConfidence.HIGH              -> Icons.Outlined.Verified
            }, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(confidence.label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = color)
            Text(confidence.description, fontSize = 11.sp, color = color.copy(alpha = 0.85f),
                lineHeight = 15.sp)
            if (!confidence.predictionsUnlocked) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress   = { completedCycles / 3f },
                    modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color      = color, trackColor = color.copy(alpha = 0.2f)
                )
                Text("$completedCycles / 3 ciclos completos", fontSize = 10.sp,
                    color = color, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// =============================================================================
// ACTION CARD
// =============================================================================

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, description: String,
    accentColor: Color, isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(alpha = 0.7f))
        .border(1.dp, if (isDestructive) accentColor.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape)
            .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = if (isDestructive) accentColor else MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = accentColor,
            modifier = Modifier.size(20.dp))
    }
}

// =============================================================================
// GENERATE CYCLES DIALOG
// =============================================================================

@Composable
private fun GenerateCyclesDialog(onGenerate: (Int) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(3) }

    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text("Generar ciclos de ejemplo", style = MaterialTheme.typography.titleMedium)
            Text("Se generaran ciclos completos con patron realista: 5 dias menstruales, fase seca, ciclo de moco, Dia Pico en dia 15 y fase post-Pico de 13 dias.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    3  to "3 ciclos — Desbloquea predicciones (nivel bajo)",
                    6  to "6 ciclos — Confianza moderada",
                    12 to "12 ciclos — Confianza alta"
                ).forEach { (n, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (selected == n) Emerald200.copy(0.4f) else Color.Transparent)
                        .border(1.5.dp,
                            if (selected == n) Emerald600 else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(10.dp))
                        .clickable { selected = n }.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == n, onClick = { selected = n },
                            colors = RadioButtonDefaults.colors(selectedColor = Emerald600))
                        Text(label, fontSize = 13.sp,
                            color = if (selected == n) Emerald600 else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFEF3C7)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Warning, null, tint = Color(0xFFD97706),
                    modifier = Modifier.size(14.dp))
                Text("Esto reemplaza los registros existentes.", fontSize = 11.sp,
                    color = Color(0xFF92400E), lineHeight = 15.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = { onGenerate(selected) }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)) {
                    Text("Generar $selected ciclos")
                }
            }
        }
    }
}

// =============================================================================
// MANUAL DATE REGISTER DIALOG
// =============================================================================