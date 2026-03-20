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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =============================================================================
// MODULE 8 - CALENDAR WITH VISUAL PREDICTIONS
// =============================================================================

// Day types for calendar rendering
sealed class CalendarDay {
    data class Registered(val record: DailyRecord) : CalendarDay()
    data class PredictedPeak(val date: Calendar)   : CalendarDay()
    data class PredictedPeriod(val date: Calendar) : CalendarDay()
    data class Today(val date: Calendar)            : CalendarDay()
    data class Empty(val date: Calendar)            : CalendarDay()
    object Blank                                    : CalendarDay()
}

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val allRecords by viewModel.allRecords.collectAsState()
    val analysis   by viewModel.cycleAnalysis.collectAsState()

    var displayedMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }

    // Build a map of date -> DailyRecord for fast lookup
    val recordsByDay = remember(allRecords) {
        allRecords.associate { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.date }
            val key = dayKey(cal)
            key to record
        }
    }

    // Predicted dates — up to 3 future cycles
    val todayCal = Calendar.getInstance()
    val futurePredictions: List<Pair<Calendar, Calendar>> = remember(analysis) {
        val avgCycle    = analysis?.avgCycleLength    ?: 28
        val avgPostPeak = analysis?.avgPostPeakLength ?: 13
        val nextPeriod  = analysis?.estimatedNextPeriod
        val nextPeak    = analysis?.estimatedPeakDay
        val canPredict  = analysis?.predictionConfidence?.predictionsUnlocked == true &&
                nextPeriod != null
        if (!canPredict) return@remember emptyList()
        // Build 3 cycles: each is a (peakCal, periodCal) pair
        buildList {
            var periodOffset = nextPeriod!!
            var peakOffset   = nextPeak ?: (periodOffset - avgPostPeak)
            repeat(3) {
                val peakCal   = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, peakOffset) }
                val periodCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, periodOffset) }
                add(peakCal to periodCal)
                periodOffset += avgCycle
                peakOffset   += avgCycle
            }
        }
    }
    // Legacy single prediction (for legend + summary)
    val predictedPeakCal   = futurePredictions.firstOrNull()?.first
    val predictedPeriodCal = futurePredictions.firstOrNull()?.second

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(Emerald200, Purple100, Pink200)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CalendarHeader(
                month      = displayedMonth,
                onPrevious = {
                    displayedMonth = (displayedMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                    }
                },
                onNext = {
                    displayedMonth = (displayedMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                },
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Legend
                CalendarLegend(
                    hasPredictions = predictedPeakCal != null || predictedPeriodCal != null
                )

                // Calendar grid
                CalendarGrid(
                    month              = displayedMonth,
                    recordsByDay       = recordsByDay,
                    today              = todayCal,
                    predictedPeak      = predictedPeakCal,
                    predictedPeriod    = predictedPeriodCal,
                    futurePredictions  = futurePredictions,
                    onDayClick         = { day -> selectedDay = day }
                )

                // Analysis summary
                analysis?.let { a ->
                    CalendarAnalysisSummary(analysis = a)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Day detail dialog
        selectedDay?.let { day ->
            when (day) {
                is CalendarDay.Registered -> DayDetailDialog(
                    record    = day.record,
                    onDismiss = { selectedDay = null }
                )
                is CalendarDay.PredictedPeak -> PredictionDetailDialog(
                    title     = "Dia Pico estimado",
                    message   = "Segun tu historial, el proximo Dia Pico podria ocurrir alrededor de esta fecha. Recorda que el Modelo Creighton es prospectivo — observa el moco cada dia.",
                    color     = Emerald600,
                    onDismiss = { selectedDay = null }
                )
                is CalendarDay.PredictedPeriod -> PredictionDetailDialog(
                    title     = "Menstruacion estimada",
                    message   = "Segun tu fase post-Pico promedio, la proxima menstruacion podria comenzar alrededor de esta fecha. Es una estimacion basada en tu historial.",
                    color     = Color(0xFFDC2626),
                    onDismiss = { selectedDay = null }
                )
                else -> {}
            }
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun CalendarHeader(
    month: Calendar,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val monthName = SimpleDateFormat("MMMM yyyy", Locale("es"))
        .format(month.time)
        .replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x66FFFFFF))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Atras",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Calendario del Ciclo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(48.dp))
        }

        // Month navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Outlined.ChevronLeft, "Mes anterior",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                monthName,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Outlined.ChevronRight, "Mes siguiente",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Day of week headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            listOf("Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sab").forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// =============================================================================
// CALENDAR GRID
// =============================================================================

@Composable
private fun CalendarGrid(
    month: Calendar,
    recordsByDay: Map<String, DailyRecord>,
    today: Calendar,
    predictedPeak: Calendar?,
    predictedPeriod: Calendar?,
    futurePredictions: List<Pair<Calendar, Calendar>> = emptyList(),
    onDayClick: (CalendarDay) -> Unit
) {
    // Build sets of all predicted day keys for fast lookup
    val allPredictedPeakKeys   = futurePredictions.map { dayKey(it.first) }.toSet()
    val allPredictedPeriodKeys = futurePredictions.map { dayKey(it.second) }.toSet()

    // Build the grid of days
    val firstDay = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val startDow = firstDay.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = mutableListOf<CalendarDay>()

    // Leading blanks
    repeat(startDow) { cells.add(CalendarDay.Blank) }

    // Days of month
    for (d in 1..daysInMonth) {
        val cal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
        val key = dayKey(cal)

        val day = when {
            recordsByDay.containsKey(key) ->
                CalendarDay.Registered(recordsByDay[key]!!)
            key in allPredictedPeakKeys ->
                CalendarDay.PredictedPeak(cal)
            key in allPredictedPeriodKeys ->
                CalendarDay.PredictedPeriod(cal)
            dayKey(today) == key ->
                CalendarDay.Today(cal)
            else ->
                CalendarDay.Empty(cal)
        }
        cells.add(day)
    }

    // Trailing blanks to complete last row
    while (cells.size % 7 != 0) cells.add(CalendarDay.Blank)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x99FFFFFF))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        CalendarDayCell(day = day, onClick = { onDayClick(day) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: CalendarDay, onClick: () -> Unit) {
    if (day is CalendarDay.Blank) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val (bg, border, textColor, label, topLabel) = when (day) {
        is CalendarDay.Registered -> {
            val stamp = day.record.stampType
            val bg = when (stamp) {
                StampType.RED.name         -> Color(0xFFEF4444)
                StampType.GREEN_SOLID.name -> Emerald400
                StampType.WHITE_BABY.name  -> Color.White
                StampType.GREEN_BABY.name  -> Emerald200
                else                       -> Color(0xFFE5E7EB)
            }
            val border = when (stamp) {
                StampType.RED.name         -> Color(0xFFDC2626)
                StampType.GREEN_SOLID.name -> Emerald600
                StampType.WHITE_BABY.name  -> Emerald400
                StampType.GREEN_BABY.name  -> Emerald400
                else                       -> Color(0xFFD1D5DB)
            }
            val txt = when (stamp) {
                StampType.RED.name -> Color.White
                else               -> Color(0xFF065F46)
            }
            val top = if (day.record.isPeakDay) "P" else
                if (day.record.postPeakCount > 0) "${day.record.postPeakCount}" else null
            DayCellStyle(bg, border, txt,
                Calendar.getInstance().apply { timeInMillis = day.record.date }
                    .get(Calendar.DAY_OF_MONTH).toString(),
                top)
        }
        is CalendarDay.PredictedPeak -> DayCellStyle(
            Emerald200.copy(alpha = 0.5f), Emerald600,
            Emerald600, day.date.get(Calendar.DAY_OF_MONTH).toString(), "~P"
        )
        is CalendarDay.PredictedPeriod -> DayCellStyle(
            Color(0xFFFEE2E2), Color(0xFFEF4444),
            Color(0xFFDC2626), day.date.get(Calendar.DAY_OF_MONTH).toString(), "~M"
        )
        is CalendarDay.Today -> DayCellStyle(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary,
            day.date.get(Calendar.DAY_OF_MONTH).toString(), null
        )
        is CalendarDay.Empty -> DayCellStyle(
            Color.Transparent, Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            day.date.get(Calendar.DAY_OF_MONTH).toString(), null
        )
        else -> DayCellStyle(Color.Transparent, Color.Transparent, Color.Transparent, "", null)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (border != Color.Transparent)
                    Modifier.border(1.5.dp, border, RoundedCornerShape(8.dp))
                else Modifier
            )
            .then(
                if (day !is CalendarDay.Empty && day !is CalendarDay.Blank)
                    Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            topLabel?.let {
                Text(it, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
            // Baby emoji for fertile stamps
            if (day is CalendarDay.Registered &&
                (day.record.stampType == StampType.WHITE_BABY.name ||
                        day.record.stampType == StampType.GREEN_BABY.name)) {
                Text("👶", fontSize = 8.sp)
            }
        }
        // Intercourse dot
        if (day is CalendarDay.Registered && day.record.hasIntercourse) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private data class DayCellStyle(
    val bg: Color, val border: Color, val textColor: Color,
    val label: String, val topLabel: String?
)

// =============================================================================
// LEGEND
// =============================================================================

@Composable
private fun CalendarLegend(hasPredictions: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(Color(0xFFEF4444), "Menstrual")
        LegendItem(Emerald400,        "Infertil")
        LegendItem(Color.White,       "Fertil/Moco",  border = Emerald400)
        LegendItem(Emerald200,        "Post-Pico",    border = Emerald400)
        if (hasPredictions) {
            LegendItem(Emerald200.copy(alpha = 0.5f), "~Pico est.", border = Emerald600, dashed = true)
            LegendItem(Color(0xFFFEE2E2), "~Mens. est.", border = Color(0xFFEF4444), dashed = true)
        }
    }
}

@Composable
private fun LegendItem(
    color: Color, label: String,
    border: Color? = null, dashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(
                    if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(3.dp))
                    else Modifier
                )
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// =============================================================================
// ANALYSIS SUMMARY CARD
// =============================================================================

@Composable
private fun CalendarAnalysisSummary(analysis: CycleAnalysis) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x99FFFFFF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.AutoGraph, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text("Predicciones del ciclo",
                fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            analysis.estimatedPeakDay?.let { days ->
                SummaryChip(
                    label = "Proximo Pico",
                    value = "En ~$days dias",
                    color = Emerald600,
                    modifier = Modifier.weight(1f)
                )
            }
            analysis.estimatedNextPeriod?.let { days ->
                SummaryChip(
                    label = "Proxima mens.",
                    value = if (days == 0) "Pronto" else "En ~$days dias",
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            "Las predicciones son estimaciones basadas en tu historial. El Modelo Creighton es prospectivo: observa cada dia.",
            fontSize   = 10.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// =============================================================================
// PREDICTION DETAIL DIALOG
// =============================================================================

@Composable
private fun PredictionDetailDialog(
    title: String, message: String, color: Color, onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoGraph, null,
                        tint = color, modifier = Modifier.size(20.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = color, fontWeight = FontWeight.Bold)
            }
            Text(message, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)
            Row(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFEF3C7)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Info, null,
                    tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                Text("Esta es una prediccion educativa, no un diagnostico.",
                    fontSize = 11.sp, color = Color(0xFF92400E), lineHeight = 15.sp)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cerrar")
            }
        }
    }
}

// =============================================================================
// HELPER
// =============================================================================

private fun dayKey(cal: Calendar): String {
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
}