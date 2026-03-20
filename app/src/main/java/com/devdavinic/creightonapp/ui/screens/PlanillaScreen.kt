package com.devdavinic.creightonapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devdavinic.creightonapp.model.DailyRecord
import com.devdavinic.creightonapp.model.StampType
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════════
//  MÓDULO 2 — PLANILLA VISUAL NaProTRACKING™
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PlanillaScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onRegistrar: () -> Unit
) {
    val allRecords     by viewModel.allRecords.collectAsState()
    val cycleRecords   by viewModel.currentCycleRecords.collectAsState()
    val cycles = remember(allRecords) { groupByCycles(allRecords) }
    var selectedRecord    by remember { mutableStateOf<DailyRecord?>(null) }
    var editingRecord     by remember { mutableStateOf<DailyRecord?>(null) }
    var deleteTarget      by remember { mutableStateOf<DailyRecord?>(null) }
    var deleteCycleTarget by remember { mutableStateOf<List<DailyRecord>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(Emerald200, Purple100, Pink200)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ══ HEADER con statusBarsPadding ══
            PlanillaHeader(
                cycleDay   = cycleRecords.size + 1,
                cycleCount = cycles.size,
                onBack     = onBack,
                onRegistrar = onRegistrar
            )

            StampLegend()

            // ══ CONTENIDO con navigationBarsPadding al final ══
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
                    // navigationBarsPadding() al final del scroll para que el último ciclo
                    // no quede tapado por la barra del sistema
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (cycles.isEmpty()) {
                    EmptyPlanillaState(onRegistrar)
                } else {
                    cycles.forEachIndexed { index, cycleList ->
                        CycleRow(
                            cycleNumber   = cycles.size - index,
                            records       = cycleList,
                            onDayTap      = { selectedRecord = it },
                            isTestMode    = viewModel.isTestMode,
                            onDeleteCycle = if (viewModel.isTestMode) {{ deleteCycleTarget = cycleList }} else null
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        selectedRecord?.let { record ->
            DayDetailDialog(
                record     = record,
                isTestMode = viewModel.isTestMode,
                onDismiss  = { selectedRecord = null },
                onEdit     = { editingRecord = record; selectedRecord = null },
                onDelete   = { deleteTarget  = record; selectedRecord = null }
            )
        }

        editingRecord?.let { record ->
            EditRecordDialog(
                record    = record,
                onSave    = { updated -> viewModel.updateRecord(updated); editingRecord = null },
                onDismiss = { editingRecord = null }
            )
        }

        deleteTarget?.let { record ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title   = { Text("Borrar registro") },
                text    = { Text("Borrar el registro del dia ${record.cycleDay} (${record.officialCode}). No se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteRecord(record); deleteTarget = null }) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
                }
            )
        }

        deleteCycleTarget?.let { cycleList ->
            AlertDialog(
                onDismissRequest = { deleteCycleTarget = null },
                title   = { Text("Borrar ciclo completo") },
                text    = { Text("Borrar los ${cycleList.size} registros de este ciclo. No se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        cycleList.forEach { viewModel.deleteRecord(it) }
                        deleteCycleTarget = null
                    }) { Text("Borrar ciclo", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteCycleTarget = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

// ─── Agrupación por ciclos ────────────────────────────────────────────────────

private fun groupByCycles(records: List<DailyRecord>): List<List<DailyRecord>> {
    if (records.isEmpty()) return emptyList()
    val sorted = records.sortedBy { it.date }
    val cycles = mutableListOf<MutableList<DailyRecord>>()
    var current = mutableListOf<DailyRecord>()
    for (record in sorted) {
        val isNewCycle = record.bleedingLevel == "H" || record.bleedingLevel == "M"
        if (isNewCycle && current.isNotEmpty()) { cycles.add(current); current = mutableListOf() }
        current.add(record)
    }
    if (current.isNotEmpty()) cycles.add(current)
    return cycles.reversed()
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEADER con statusBarsPadding
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PlanillaHeader(cycleDay: Int, cycleCount: Int, onBack: () -> Unit, onRegistrar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteAlpha40)
            // ← reserva el espacio de la status bar (hora, batería, notificaciones)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Planilla NaProTRACKING™",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("Día $cycleDay del ciclo · $cycleCount ciclo${if (cycleCount != 1) "s" else ""}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRegistrar) {
            Icon(Icons.Outlined.Add, "Nuevo registro", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ─── Leyenda ──────────────────────────────────────────────────────────────────

@Composable
private fun StampLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(Color(0xFFEF4444), "Menstrual")
        LegendItem(Emerald400,        "Infértil / Seco")
        LegendItem(Color.White,       "Fértil / Moco",    border = Emerald400)
        LegendItem(Emerald200,        "Post-Pico seco",   border = Emerald400)
        LegendItem(Color(0xFFFBBF24), "Especial / Amarilla")
    }
}

@Composable
private fun LegendItem(color: Color, label: String, border: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(color)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(3.dp)) else Modifier))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FILA DE CICLO
// ═══════════════════════════════════════════════════════════════════════════════

private val CELL_WIDTH = 38.dp

@Composable
private fun CycleRow(
    cycleNumber: Int,
    records: List<DailyRecord>,
    onDayTap: (DailyRecord) -> Unit,
    isTestMode: Boolean = false,
    onDeleteCycle: (() -> Unit)? = null
) {
    val hScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteAlpha50)
            .border(1.dp, WhiteAlpha40, RoundedCornerShape(16.dp))
    ) {
        // Encabezado del ciclo
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ciclo $cycleNumber", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (records.isNotEmpty()) {
                    Text(SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(records.first().date)),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isTestMode && onDeleteCycle != null) {
                    IconButton(onClick = onDeleteCycle, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.DeleteSweep, "Borrar ciclo",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Números 1-35 (comparten el mismo scroll que las filas)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(48.dp))
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                repeat(35) { i ->
                    Box(modifier = Modifier.width(CELL_WIDTH).padding(horizontal = 1.dp),
                        contentAlignment = Alignment.Center) {
                        Text("${i + 1}", fontSize = 9.sp,
                            color = if (i < records.size) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            fontWeight = if (i < records.size) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        }

        // ESTAMPA
        PlanillaRow("Estampa", hScroll) {
            repeat(35) { i ->
                DayStampCell(records.getOrNull(i)) { records.getOrNull(i)?.let(onDayTap) }
            }
        }

        // FECHA
        PlanillaRow("Fecha", hScroll) {
            repeat(35) { i ->
                val r = records.getOrNull(i)
                Box(modifier = Modifier.width(CELL_WIDTH).padding(horizontal = 1.dp),
                    contentAlignment = Alignment.Center) {
                    if (r != null) Text(
                        SimpleDateFormat("d/M", Locale("es")).format(Date(r.date)),
                        fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            }
        }

        // CÓDIGO
        PlanillaRow("Código", hScroll) {
            repeat(35) { i ->
                val r = records.getOrNull(i)
                Box(modifier = Modifier.width(CELL_WIDTH).heightIn(min = 28.dp).padding(horizontal = 1.dp),
                    contentAlignment = Alignment.TopCenter) {
                    if (r != null) Text(r.officialCode, fontSize = 7.5.sp,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                        lineHeight = 9.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlanillaRow(label: String, scrollState: ScrollState, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(48.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        // Todas las filas comparten el mismo ScrollState para que el scroll sea sincronizado
        Row(modifier = Modifier.horizontalScroll(scrollState), content = content)
    }
}

@Composable
private fun DayStampCell(record: DailyRecord?, onClick: () -> Unit) {
    val stampColor = when (record?.stampType) {
        StampType.RED.name         -> Color(0xFFEF4444)
        StampType.GREEN_SOLID.name -> Emerald400
        StampType.WHITE_BABY.name  -> Color.White
        StampType.GREEN_BABY.name  -> Emerald200
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
    }
    val borderColor = when (record?.stampType) {
        StampType.RED.name         -> Color(0xFFDC2626)
        StampType.GREEN_SOLID.name -> Emerald600
        StampType.WHITE_BABY.name  -> Emerald400
        StampType.GREEN_BABY.name  -> Emerald400
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    val hasBaby = record?.stampType == StampType.WHITE_BABY.name || record?.stampType == StampType.GREEN_BABY.name

    Box(
        modifier = Modifier
            .width(CELL_WIDTH).height(38.dp).padding(2.dp)
            .clip(RoundedCornerShape(6.dp)).background(stampColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .then(if (record != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (record?.isPeakDay == true)
                Text("P", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = if (record.stampType == StampType.RED.name) Color.White else Color(0xFF065F46))
            if ((record?.postPeakCount ?: 0) > 0)
                Text("${record?.postPeakCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
            if (hasBaby && record?.isPeakDay != true && (record?.postPeakCount ?: 0) == 0)
                Text("👶", fontSize = 11.sp)
        }
        // Punto de intercurso — esquina inferior derecha
        if (record?.hasIntercourse == true) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                .size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

// ─── Estado vacío ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyPlanillaState(onRegistrar: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("📋", fontSize = 48.sp)
        Text("Tu planilla está vacía", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text("Comenzá a registrar tus observaciones diarias para ver tu gráfica aquí.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 18.sp)
        Button(onClick = onRegistrar,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Registrar primer día")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  DIALOG DE DETALLE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun DayDetailDialog(
    record: DailyRecord,
    onDismiss: () -> Unit,
    isTestMode: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val stampColor = when (record.stampType) {
        StampType.RED.name         -> Color(0xFFEF4444)
        StampType.GREEN_SOLID.name -> Emerald400
        StampType.WHITE_BABY.name  -> Color.White
        StampType.GREEN_BABY.name  -> Emerald200
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val stampLabel = StampType.entries.find { it.name == record.stampType }?.label ?: ""
    val dateStr = SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es"))
        .format(Date(record.date)).replaceFirstChar { it.uppercase() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Día ${record.cycleDay} del ciclo", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(stampColor)
                    .border(2.dp, stampColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    when {
                        record.isPeakDay -> Text("P", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = if (record.stampType == StampType.RED.name) Color.White else Color(0xFF065F46))
                        record.postPeakCount > 0 -> Text("${record.postPeakCount}", fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = Color(0xFF065F46))
                        record.stampType == StampType.WHITE_BABY.name || record.stampType == StampType.GREEN_BABY.name ->
                            Text("👶", fontSize = 18.sp)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(16.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CÓDIGO OFICIAL", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(record.officialCode,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(stampLabel, fontSize = 12.sp,
                        color = if (record.stampType == StampType.RED.name) Color(0xFFDC2626) else Emerald600)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!record.bleedingLevel.isNullOrEmpty())        DetailRow("Sangrado",     record.bleedingLevel)
                if (!record.sensation.isNullOrEmpty())            DetailRow("Sensación",    record.sensation)
                if (!record.lubricationSensation.isNullOrEmpty()) DetailRow("Lubricación",  record.lubricationSensation)
                if (!record.mucusConsistency.isNullOrEmpty())     DetailRow("Consistencia", record.mucusConsistency)
                if (!record.mucusColor.isNullOrEmpty())           DetailRow("Color",        record.mucusColor)
                if (!record.observationFrequency.isNullOrEmpty()) DetailRow("Frecuencia",   record.observationFrequency)
            }

            if (record.isPeakDay || record.hasIntercourse || record.breastSelfExam) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (record.isPeakDay)       MarkerBadge("Día Pico", Emerald600)
                    if (record.hasIntercourse)  MarkerBadge("I — Relación", MaterialTheme.colorScheme.primary)
                    if (record.breastSelfExam)  MarkerBadge("AM", Pink500)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isTestMode && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Borrar", fontSize = 13.sp)
                    }
                }
                if (onEdit != null) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar", fontSize = 13.sp)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}

@Composable
private fun MarkerBadge(text: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(color.copy(alpha = 0.12f))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

// =============================================================================
// EDIT RECORD DIALOG
// Allows editing peak day, intercourse and AM markers from planilla
// =============================================================================

@Composable
fun EditRecordDialog(record: DailyRecord, onSave: (DailyRecord) -> Unit, onDismiss: () -> Unit) {
    var isPeakDay   by remember { mutableStateOf(record.isPeakDay) }
    var hasI        by remember { mutableStateOf(record.hasIntercourse) }
    var hasAM       by remember { mutableStateOf(record.breastSelfExam) }

    val dateStr = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es"))
        .format(Date(record.date)).replaceFirstChar { it.uppercase() }

    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Column {
                Text("Editar registro", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Codigo: ${record.officialCode}", fontSize = 12.sp,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider()

            Text("Marcadores editables:", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Peak Day toggle
            val isPeakCandidate = record.lubricationSensation != null ||
                    record.mucusConsistency == MucusConsistency.STRETCHY.code ||
                    record.mucusColor == MucusColor.CLEAR.code ||
                    record.mucusColor == MucusColor.CLOUDY_CLEAR.code

            if (isPeakCandidate || record.isPeakDay) {
                EditMarkerRow(isPeakDay, { isPeakDay = it },
                    "Dia Pico — P", "Marca este dia como el ultimo dia de moco tipo Pico.",
                    Color(0xFF7C3AED))
            }
            EditMarkerRow(hasI, { hasI = it },
                "Relacion intima genital — I",
                "Se registra con I debajo de la estampa.",
                MaterialTheme.colorScheme.primary)
            EditMarkerRow(hasAM, { hasAM = it },
                "Autoexamen mamario — AM",
                "Autoexamen mamario realizado este dia.",
                Pink500)

            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFEF3C7)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Info, null, tint = Color(0xFFD97706),
                    modifier = Modifier.size(14.dp))
                Text("Solo se pueden editar marcadores. Para cambiar el codigo del dia borra y vuelve a registrar.",
                    fontSize = 10.sp, color = Color(0xFF92400E), lineHeight = 14.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        // Rebuild official code with updated peak day
                        val b   = record.bleedingLevel?.let { c -> BleedingLevel.entries.find { it.code == c } }
                        val s   = record.sensation?.let { c -> Sensation.entries.find { it.code == c } }
                        val c   = record.mucusConsistency?.let { c2 -> MucusConsistency.entries.find { it.code == c2 } }
                        val mc  = record.mucusColor?.let { c2 -> MucusColor.entries.find { it.code == c2 } }
                        val l   = record.lubricationSensation?.let { c2 -> LubricationSensation.entries.find { it.code == c2 } }
                        val frq = com.devdavinic.creightonapp.model.ObservationFrequency.entries
                            .find { it.code == record.observationFrequency }
                        val newCode = CreightonLogic.buildOfficialCode(b, s, c, mc, l, frq, isPeakDay)
                        onSave(record.copy(
                            isPeakDay      = isPeakDay,
                            hasIntercourse = hasI,
                            breastSelfExam = hasAM,
                            officialCode   = newCode
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) { Text("Guardar cambios") }
            }
        }
    }
}

@Composable
private fun EditMarkerRow(
    checked: Boolean, onChange: (Boolean) -> Unit,
    label: String, sublabel: String, color: Color
) {
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(if (checked) color.copy(alpha = 0.08f) else Color.Transparent)
        .border(if (checked) 1.5.dp else 1.dp,
            if (checked) color else MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(12.dp))
        .clickable { onChange(!checked) }.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = color))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                color = if (checked) color else MaterialTheme.colorScheme.onSurface)
            Text(sublabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp)
        }
    }
}