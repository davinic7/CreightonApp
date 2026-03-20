package com.devdavinic.creightonapp.model

// =============================================================================
// AI CONTEXT BUILDER - Module 4
// Builds the system prompt from real cycle data so Claude can answer
// personalized questions about the Creighton Model.
// =============================================================================

object AiContextBuilder {

    /**
     * Builds the full system prompt injecting the user's real cycle data.
     * This is sent with every API call so Claude always has full context.
     */
    fun buildSystemPrompt(
        analysis: CycleAnalysis?,
        recentRecords: List<DailyRecord>
    ): String {
        val cycleContext = if (analysis != null) buildCycleContext(analysis, recentRecords)
        else "La usuaria no tiene registros todavia. Animalala a comenzar a registrar."

        return """
Sos una asistente educativa especializada en el CREIGHTON MODEL FertilityCare System (CrMS) y NaProTRACKING.

Tu rol es:
- Responder preguntas sobre el Modelo Creighton, el sistema de registro y la fertilidad
- Interpretar los registros y patrones de la usuaria en lenguaje simple y amable
- Explicar conceptos del manual (Dia Pico, moco tipo Pico, fase post-Pico, estampas, etc.)
- Dar consejos educativos sobre S-P-I-C-E cuando sea relevante
- Alertar sobre patrones que el manual considera señales importantes

Limitaciones importantes que SIEMPRE debes respetar:
- NO das diagnosticos medicos ni reemplazas al Profesional de FertilityCare
- Si hay alertas serias (fase post-Pico muy corta, ciclo seco, sangrado inusual), siempre recomiendas consultar al Profesional
- Cuando no estas segura de algo, lo dices claramente
- Respondas en español, en tono calido, claro y sin tecnicismos innecesarios
- Respuestas concisas: maximo 3-4 parrafos salvo que la pregunta requiera mas detalle

DATOS REALES DEL CICLO DE LA USUARIA:
$cycleContext

Usa estos datos para personalizar tus respuestas. Si la pregunta es sobre su ciclo actual, referite siempre a sus datos reales.
        """.trimIndent()
    }

    private fun buildCycleContext(
        analysis: CycleAnalysis,
        recentRecords: List<DailyRecord>
    ): String {
        val sb = StringBuilder()

        sb.appendLine("=== CICLO ACTUAL ===")
        sb.appendLine("Dia del ciclo: ${analysis.cycleDay}")
        sb.appendLine("Fase actual: ${analysis.currentPhase.label} - ${analysis.currentPhase.description}")
        sb.appendLine("Hormonal estimada: ${analysis.hormonalPhase.label} (${analysis.hormonalPhase.dominantHormone})")
        sb.appendLine("Fertil ahora: ${if (analysis.isCurrentlyFertile) "Si" else "No"}")

        if (analysis.postPeakDay > 0) {
            sb.appendLine("Dia post-Pico: ${analysis.postPeakDay}")
        }

        sb.appendLine()
        sb.appendLine("=== METRICAS DEL CICLO ACTUAL ===")
        sb.appendLine("Dias con moco: ${analysis.mucusCycleDays}")
        sb.appendLine("Dias fase pre-Pico: ${analysis.prePicoPhaseDays}")
        if (analysis.postPicoPhaseDays > 0) {
            sb.appendLine("Dias fase post-Pico: ${analysis.postPicoPhaseDays}")
        }

        if (analysis.totalCyclesRecorded >= 2) {
            sb.appendLine()
            sb.appendLine("=== PROMEDIOS HISTORICOS (${analysis.totalCyclesRecorded} ciclos) ===")
            analysis.avgCycleLength?.let { sb.appendLine("Duracion promedio del ciclo: $it dias") }
            analysis.avgPostPeakLength?.let {
                sb.appendLine("Fase post-Pico promedio: $it dias (normal: 9-17 dias)")
                if (it < 9) sb.appendLine("ALERTA: fase post-Pico por debajo del rango normal")
                if (it > 16) sb.appendLine("ALERTA: fase post-Pico por encima del rango normal")
            }
            analysis.avgMucusCycleDays?.let { sb.appendLine("Dias promedio con moco: $it") }
        }

        if (analysis.estimatedPeakDay != null || analysis.estimatedNextPeriod != null) {
            sb.appendLine()
            sb.appendLine("=== PREDICCIONES ===")
            analysis.estimatedPeakDay?.let { sb.appendLine("Dia Pico estimado: dia $it del ciclo") }
            analysis.estimatedNextPeriod?.let { sb.appendLine("Proxima menstruacion estimada: en ~$it dias") }
        }

        if (analysis.alerts.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("=== ALERTAS ACTIVAS ===")
            analysis.alerts.forEach { alert ->
                sb.appendLine("[${alert.severity.name}] ${alert.title}: ${alert.message}")
            }
        }

        // Last 7 days of records
        if (recentRecords.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("=== ULTIMOS REGISTROS (hasta 7 dias) ===")
            recentRecords.takeLast(7).forEach { record ->
                val line = buildString {
                    append("Dia ${record.cycleDay}: ${record.officialCode}")
                    append(" | Estampa: ${record.stampType}")
                    if (record.isPeakDay) append(" | DIA PICO")
                    if (record.postPeakCount > 0) append(" | Post-Pico ${record.postPeakCount}")
                    if (record.hasIntercourse) append(" | Intercurso (I)")
                }
                sb.appendLine(line)
            }
        }

        return sb.toString()
    }

    /**
     * Builds a short context summary for display in the UI
     * (shown to the user so she knows what context Claude has)
     */
    fun buildContextSummary(analysis: CycleAnalysis?): String {
        if (analysis == null) return "Sin datos de ciclo"
        return "Dia ${analysis.cycleDay} - ${analysis.currentPhase.label} - " +
                "${if (analysis.isCurrentlyFertile) "Fertil" else "Infertil"}"
    }
}