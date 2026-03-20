package com.devdavinic.creightonapp.model

// =============================================================================
// CYCLE ANALYSIS ENGINE - Module 3
// Based on Chapters 7, 9, 10 and Appendix A of the Creighton manual
// =============================================================================

data class CycleAnalysis(
    val currentPhase: CyclePhase,
    val cycleDay: Int,
    val postPeakDay: Int,
    val isCurrentlyFertile: Boolean,
    val mucusCycleDays: Int,
    val prePicoPhaseDays: Int,
    val postPicoPhaseDays: Int,
    val avgCycleLength: Int?,
    val avgPostPeakLength: Int?,
    val avgMucusCycleDays: Int?,
    val totalCyclesRecorded: Int,
    val estimatedPeakDay: Int?,
    val estimatedNextPeriod: Int?,
    val alerts: List<CycleAlert>,
    val hormonalPhase: HormonalPhase,
    val predictionConfidence: PredictionConfidence = PredictionConfidence.INSUFFICIENT_DATA
)

// Prediction reliability based on recorded cycles
enum class PredictionConfidence(
    val label: String,
    val description: String,
    val predictionsUnlocked: Boolean
) {
    INSUFFICIENT_DATA(
        label = "Sin datos suficientes",
        description = "Se necesitan al menos 3 ciclos completos para predicciones confiables.",
        predictionsUnlocked = false
    ),
    LOW(
        label = "Confianza baja",
        description = "3 ciclos registrados. Las predicciones son aproximadas.",
        predictionsUnlocked = true
    ),
    MODERATE(
        label = "Confianza moderada",
        description = "6 ciclos registrados. Las predicciones mejoran con mas datos.",
        predictionsUnlocked = true
    ),
    HIGH(
        label = "Confianza alta",
        description = "12 o mas ciclos registrados. Patron bien establecido.",
        predictionsUnlocked = true
    )
}

// -----------------------------------------------------------------------------
// CYCLE PHASE
// -----------------------------------------------------------------------------

enum class CyclePhase(val label: String, val description: String) {
    MENSTRUAL    ("Menstrual",    "Periodo menstrual activo"),
    PRE_PEAK     ("Pre-Pico",     "Fase preovulatoria - hasta el Dia Pico"),
    PEAK_DAY     ("Dia Pico",     "Ultimo dia de moco tipo Pico (asociado a ovulacion)"),
    POST_PEAK_123("Post-Pico 1-3","Tres dias de cuenta obligatoria despues del Pico"),
    POST_PEAK    ("Post-Pico",    "Fase postovulatoria - desde P+4 hasta la menstruacion"),
    UNKNOWN      ("Sin datos",    "No hay suficientes registros aun")
}

// -----------------------------------------------------------------------------
// HORMONAL PHASE (educational estimate)
// -----------------------------------------------------------------------------

enum class HormonalPhase(
    val label: String,
    val description: String,
    val dominantHormone: String
) {
    FOLLICULAR(
        "Fase Folicular",
        "El estrogeno esta subiendo y estimula la produccion de moco cervical.",
        "Estrogeno en aumento"
    ),
    OVULATORY(
        "Fase Ovulatoria",
        "El estrogeno alcanza su pico. El moco tipo Pico indica el momento de mayor fertilidad.",
        "Estrogeno maximo"
    ),
    LUTEAL(
        "Fase Lutea",
        "La progesterona es dominante y suprime la produccion de moco cervical.",
        "Progesterona en aumento"
    ),
    MENSTRUAL(
        "Fase Menstrual",
        "Descenso de progesterona y estrogeno. El ciclo comienza nuevamente.",
        "Ambas hormonas en descenso"
    ),
    UNKNOWN(
        "Desconocida",
        "Se necesitan mas registros para estimar.",
        "--"
    )
}

// -----------------------------------------------------------------------------
// CYCLE ALERT
// -----------------------------------------------------------------------------

data class CycleAlert(
    val type: AlertType,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val actionHint: String? = null
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }

enum class AlertType {
    SHORT_POST_PEAK,
    LONG_POST_PEAK,
    PREMENSTRUAL_BLEEDING,
    LIMITED_MUCUS_CYCLE,
    DRY_CYCLE,
    LONG_MUCUS_CYCLE,
    DOUBLE_PEAK_SUSPECTED,
    UNUSUAL_BLEEDING,
    POST_PEAK_MUCUS,
    EARLY_OVULATION,
    VARIABLE_PEAK_RETURN,
    DAY_7_BREAST_EXAM,
    FERTILE_WINDOW_OPEN,
    INFERTILE_PHASE
}

// =============================================================================
// ANALYSIS ENGINE
// =============================================================================

object CycleAnalysisEngine {

    fun analyze(
        currentCycle: List<DailyRecord>,
        allCycles: List<List<DailyRecord>>
    ): CycleAnalysis {

        val cycleDay   = currentCycle.size
        val lastRecord = currentCycle.lastOrNull()

        val peakIndex   = currentCycle.indexOfLast { it.isPeakDay }
        val hasPeak     = peakIndex >= 0
        val postPeakDay = if (hasPeak) cycleDay - peakIndex else 0

        val mucusCycleDays   = currentCycle.count { it.stampType == StampType.WHITE_BABY.name }
        val prePicoPhaseDays = if (hasPeak) peakIndex + 1 else cycleDay
        val postPicoPhaseDays = if (hasPeak) postPeakDay else 0

        val currentPhase  = determinePhase(lastRecord, postPeakDay, hasPeak, currentCycle)
        val hormonalPhase = estimateHormonalPhase(currentPhase)

        val completedCycles   = allCycles.dropLast(1)
        val avgCycleLength    = computeAvgCycleLength(completedCycles)
        val avgPostPeakLength = computeAvgPostPeakLength(completedCycles)
        val avgMucusDays      = computeAvgMucusDays(completedCycles)

        // Predictions require >= 3 complete cycles (manual recommendation)
        val confidence = when {
            completedCycles.size >= 12 -> PredictionConfidence.HIGH
            completedCycles.size >= 6  -> PredictionConfidence.MODERATE
            completedCycles.size >= 3  -> PredictionConfidence.LOW
            else                       -> PredictionConfidence.INSUFFICIENT_DATA
        }
        val predictionsEnabled = confidence.predictionsUnlocked

        val estimatedPeakDay    = if (predictionsEnabled)
            estimatePeakDay(completedCycles, avgCycleLength) else null
        val estimatedNextPeriod = if (predictionsEnabled)
            estimateNextPeriod(cycleDay, avgCycleLength, hasPeak, postPeakDay, avgPostPeakLength)
        else null

        val alerts = buildAlerts(
            currentCycle      = currentCycle,
            cycleDay          = cycleDay,
            hasPeak           = hasPeak,
            postPeakDay       = postPeakDay,
            mucusCycleDays    = mucusCycleDays,
            prePicoPhaseDays  = prePicoPhaseDays,
            avgPostPeakLength = avgPostPeakLength,
            currentPhase      = currentPhase
        )

        return CycleAnalysis(
            currentPhase         = currentPhase,
            cycleDay             = cycleDay,
            postPeakDay          = postPeakDay,
            isCurrentlyFertile   = isFertile(currentPhase),
            mucusCycleDays       = mucusCycleDays,
            prePicoPhaseDays     = prePicoPhaseDays,
            postPicoPhaseDays    = postPicoPhaseDays,
            avgCycleLength       = avgCycleLength,
            avgPostPeakLength    = avgPostPeakLength,
            avgMucusCycleDays    = avgMucusDays,
            totalCyclesRecorded  = allCycles.size,
            estimatedPeakDay     = estimatedPeakDay,
            estimatedNextPeriod  = estimatedNextPeriod,
            alerts               = alerts,
            hormonalPhase        = hormonalPhase,
            predictionConfidence = confidence
        )
    }

    // -------------------------------------------------------------------------
    // PHASE DETERMINATION
    // -------------------------------------------------------------------------

    private fun determinePhase(
        last: DailyRecord?,
        postPeakDay: Int,
        hasPeak: Boolean,
        cycle: List<DailyRecord>
    ): CyclePhase {
        if (last == null || cycle.isEmpty()) return CyclePhase.UNKNOWN
        if (last.stampType == StampType.RED.name) return CyclePhase.MENSTRUAL
        if (last.isPeakDay) return CyclePhase.PEAK_DAY
        if (postPeakDay in 1..3) return CyclePhase.POST_PEAK_123
        if (hasPeak && postPeakDay >= 4) return CyclePhase.POST_PEAK
        return CyclePhase.PRE_PEAK
    }

    private fun isFertile(phase: CyclePhase) =
        phase == CyclePhase.PRE_PEAK ||
                phase == CyclePhase.PEAK_DAY ||
                phase == CyclePhase.POST_PEAK_123 ||
                phase == CyclePhase.MENSTRUAL

    // -------------------------------------------------------------------------
    // HORMONAL PHASE ESTIMATE
    // -------------------------------------------------------------------------

    private fun estimateHormonalPhase(phase: CyclePhase): HormonalPhase = when (phase) {
        CyclePhase.MENSTRUAL     -> HormonalPhase.MENSTRUAL
        CyclePhase.PRE_PEAK      -> HormonalPhase.FOLLICULAR
        CyclePhase.PEAK_DAY      -> HormonalPhase.OVULATORY
        CyclePhase.POST_PEAK_123,
        CyclePhase.POST_PEAK     -> HormonalPhase.LUTEAL
        CyclePhase.UNKNOWN       -> HormonalPhase.UNKNOWN
    }

    // -------------------------------------------------------------------------
    // HISTORICAL AVERAGES
    // -------------------------------------------------------------------------

    private fun computeAvgCycleLength(cycles: List<List<DailyRecord>>): Int? {
        if (cycles.size < 2) return null
        return cycles.map { it.size }.average().toInt().takeIf { it > 0 }
    }

    private fun computeAvgPostPeakLength(cycles: List<List<DailyRecord>>): Int? {
        val lengths = cycles.mapNotNull { cycle ->
            val peakIdx = cycle.indexOfLast { it.isPeakDay }
            if (peakIdx < 0) null else cycle.size - peakIdx - 1
        }
        if (lengths.size < 2) return null
        return lengths.average().toInt().takeIf { it > 0 }
    }

    private fun computeAvgMucusDays(cycles: List<List<DailyRecord>>): Int? {
        if (cycles.isEmpty()) return null
        return cycles.map { cycle ->
            cycle.count { it.stampType == StampType.WHITE_BABY.name }
        }.average().toInt()
    }

    // -------------------------------------------------------------------------
    // PREDICTIONS
    // -------------------------------------------------------------------------

    private fun estimatePeakDay(
        completedCycles: List<List<DailyRecord>>,
        avgCycleLength: Int?
    ): Int? {
        val peakDays = completedCycles.mapNotNull { cycle ->
            val idx = cycle.indexOfLast { it.isPeakDay }
            if (idx >= 0) idx + 1 else null
        }
        if (peakDays.size < 2) {
            return avgCycleLength?.let { it - 13 }?.takeIf { it > 0 }
        }
        return peakDays.average().toInt()
    }

    private fun estimateNextPeriod(
        cycleDay: Int,
        avgCycleLength: Int?,
        hasPeak: Boolean,
        postPeakDay: Int,
        avgPostPeakLength: Int?
    ): Int? {
        if (hasPeak && postPeakDay > 0) {
            val postPeakTarget = avgPostPeakLength ?: 13
            val daysRemaining  = postPeakTarget - postPeakDay
            return daysRemaining.takeIf { it >= 0 }
        }
        return avgCycleLength?.let { (it - cycleDay).takeIf { d -> d >= 0 } }
    }

    // -------------------------------------------------------------------------
    // ALERTS - based on manual Chapters 7, 9, 10 and Appendix A
    // -------------------------------------------------------------------------

    private fun buildAlerts(
        currentCycle: List<DailyRecord>,
        cycleDay: Int,
        hasPeak: Boolean,
        postPeakDay: Int,
        mucusCycleDays: Int,
        prePicoPhaseDays: Int,
        avgPostPeakLength: Int?,
        currentPhase: CyclePhase
    ): List<CycleAlert> {
        val alerts = mutableListOf<CycleAlert>()

        // Short post-peak phase (< 9 days) - Chapter 10: low progesterone
        if (hasPeak && currentCycle.lastOrNull()?.stampType == StampType.RED.name) {
            if (postPeakDay in 1..8) {
                alerts += CycleAlert(
                    type       = AlertType.SHORT_POST_PEAK,
                    title      = "Fase post-Pico corta",
                    message    = "Tu fase post-Pico duro $postPeakDay dias. El manual indica que menos de 9 dias puede senalar niveles bajos de progesterona.",
                    severity   = AlertSeverity.WARNING,
                    actionHint = "Consulta con tu Profesional de FertilityCare."
                )
            }
        }

        // Long post-peak phase (>= 17 days) - Chapter 10: possible LUF or pregnancy
        if (hasPeak && postPeakDay >= 17) {
            alerts += CycleAlert(
                type       = AlertType.LONG_POST_PEAK,
                title      = "Fase post-Pico prolongada",
                message    = "Llevas $postPeakDay dias post-Pico sin menstruacion. El manual senala que 17 o mas dias puede indicar LUF o un posible embarazo.",
                severity   = AlertSeverity.WARNING,
                actionHint = "Considera hacer una prueba de embarazo y consultar al Profesional."
            )
        }

        // Double peak suspected - Appendix A: post-peak > 16 days without period
        if (hasPeak && postPeakDay >= 16 && currentPhase != CyclePhase.MENSTRUAL) {
            alerts += CycleAlert(
                type       = AlertType.DOUBLE_PEAK_SUSPECTED,
                title      = "Posible \"Doble\" Pico",
                message    = "El manual indica que si la fase post-Pico dura 16 dias o mas, puede anticiparse un \"Doble\" Pico. Aplica las preguntas del manual en P+3.",
                severity   = AlertSeverity.INFO,
                actionHint = "Responde las 2 preguntas de \"Doble\" Pico junto a tu pareja."
            )
        }

        // Premenstrual bleeding - Chapter 10: B/VL/L 3+ days before menstruation
        if (hasPeak && currentPhase == CyclePhase.POST_PEAK) {
            val recentDays = currentCycle.takeLast(5)
            val premenstrualBleeding = recentDays.count {
                it.bleedingLevel == "B" || it.bleedingLevel == "VL" || it.bleedingLevel == "L"
            }
            if (premenstrualBleeding >= 3) {
                alerts += CycleAlert(
                    type       = AlertType.PREMENSTRUAL_BLEEDING,
                    title      = "Sangrado premenstrual detectado",
                    message    = "Tres o mas dias de sangrado ligero/marron antes de la menstruacion sugiere niveles bajos de progesterona en la fase post-Pico.",
                    severity   = AlertSeverity.WARNING,
                    actionHint = "Informa a tu Profesional de FertilityCare en tu proxima sesion."
                )
            }
        }

        // Limited mucus cycle - Chapter 9 and 10
        if (hasPeak && mucusCycleDays <= 2 && cycleDay > 10) {
            alerts += CycleAlert(
                type       = AlertType.LIMITED_MUCUS_CYCLE,
                title      = "Ciclo de moco limitado",
                message    = "Solo observaste $mucusCycleDays dias con moco en este ciclo. El manual lo asocia con infertilidad, abortos espontaneos o disfuncion hormonal.",
                severity   = AlertSeverity.WARNING,
                actionHint = "Lleva esta grafica a tu Profesional de FertilityCare."
            )
        }

        // Dry cycle - Chapter 10
        if (cycleDay > 15 && mucusCycleDays == 0 && currentPhase != CyclePhase.MENSTRUAL) {
            alerts += CycleAlert(
                type       = AlertType.DRY_CYCLE,
                title      = "Ciclo seco",
                message    = "No registraste moco cervical en este ciclo. El manual indica que esto puede deberse a medicamentos (antihistaminicos), cirugia cervical previa o baja produccion de estrogeno.",
                severity   = AlertSeverity.WARNING,
                actionHint = "Consulta con tu Profesional de FertilityCare."
            )
        }

        // Long mucus cycle (> 8 days) - Chapter 10
        if (mucusCycleDays > 8) {
            alerts += CycleAlert(
                type       = AlertType.LONG_MUCUS_CYCLE,
                title      = "Ciclo de moco prolongado",
                message    = "Llevas $mucusCycleDays dias con moco. El manual senala que mas de 8 dias puede deberse a estres cronico o deficiencia hormonal.",
                severity   = AlertSeverity.INFO,
                actionHint = "Si esto se repite, mencionalo en tu proxima sesion de seguimiento."
            )
        }

        // Early ovulation - Appendix A: pre-peak phase <= 10 days
        if (hasPeak && prePicoPhaseDays <= 10) {
            alerts += CycleAlert(
                type       = AlertType.EARLY_OVULATION,
                title      = "Ovulacion temprana detectada",
                message    = "Tu Dia Pico ocurrio en el dia $prePicoPhaseDays del ciclo. El manual indica que esto es mas comun en mujeres que se acercan a la menopausia o en ciclos cortos.",
                severity   = AlertSeverity.INFO,
                actionHint = "Presta especial atencion al moco durante los ultimos dias del periodo."
            )
        }

        // Peak-type mucus in post-peak phase - Chapter 10: possible cervix inflammation
        if (currentPhase == CyclePhase.POST_PEAK) {
            val hasPostPeakMucus = currentCycle
                .dropWhile { !it.isPeakDay }
                .drop(1)
                .any { it.stampType == StampType.WHITE_BABY.name }
            if (hasPostPeakMucus) {
                alerts += CycleAlert(
                    type       = AlertType.POST_PEAK_MUCUS,
                    title      = "Moco tipo Pico en fase post-Pico",
                    message    = "El manual senala que moco tipo Pico despues del Dia Pico puede indicar una inflamacion del cervix (eversion o erosion).",
                    severity   = AlertSeverity.WARNING,
                    actionHint = "Mencionalo a tu Profesional de FertilityCare."
                )
            }
        }

        // Fertile window open - informational
        if (currentPhase == CyclePhase.PRE_PEAK ||
            currentPhase == CyclePhase.PEAK_DAY ||
            currentPhase == CyclePhase.POST_PEAK_123) {
            alerts += CycleAlert(
                type     = AlertType.FERTILE_WINDOW_OPEN,
                title    = "Ventana fertil activa",
                message  = when (currentPhase) {
                    CyclePhase.PEAK_DAY      -> "Hoy es tu Dia Pico - maximo potencial de fertilidad."
                    CyclePhase.POST_PEAK_123 -> "Dia $postPeakDay de la cuenta post-Pico. La infertilidad comienza al final del dia 4."
                    else                     -> "Hay moco cervical presente. Estas en fase fertil pre-Pico."
                },
                severity = AlertSeverity.INFO
            )
        }

        // Infertile phase confirmed
        if (currentPhase == CyclePhase.POST_PEAK) {
            alerts += CycleAlert(
                type     = AlertType.INFERTILE_PHASE,
                title    = "Fase infertil post-Pico",
                message  = "Estas en el dia $postPeakDay post-Pico. La fase post-Pico es el periodo infertil mas confiable del ciclo.",
                severity = AlertSeverity.INFO
            )
        }

        // Day 7 breast exam reminder
        if (cycleDay == 7) {
            alerts += CycleAlert(
                type     = AlertType.DAY_7_BREAST_EXAM,
                title    = "Recordatorio: Autoexamen mamario",
                message  = "El manual recomienda realizar el autoexamen mamario (AM) en el dia 7 del ciclo.",
                severity = AlertSeverity.INFO
            )
        }

        return alerts
    }
}