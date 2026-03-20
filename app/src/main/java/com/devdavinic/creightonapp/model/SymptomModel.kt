package com.devdavinic.creightonapp.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// =============================================================================
// SYMPTOM MODEL
// Based on Creighton manual biomarkers + phase-correlated symptoms
// =============================================================================

// ── Symptom categories ────────────────────────────────────────────────────────

enum class SymptomCategory(val label: String, val icon: String) {
    PHYSICAL   ("Fisicos",           "🩺"),
    EMOTIONAL  ("Emocionales",       "💭"),
    UNUSUAL_BLEEDING("Sangrado inusual", "🔴"),
    ENERGY     ("Energia y estado",  "⚡"),
    NUTRITION  ("Alimentacion",      "🥗")
}

// ── Individual symptoms ───────────────────────────────────────────────────────

enum class SymptomType(
    val category: SymptomCategory,
    val label: String,
    val isBiomarker: Boolean = false,   // flagged in manual as clinical significance
    val biomarkerNote: String? = null   // what the manual says about it
) {
    // PHYSICAL
    BREAST_TENSION(
        SymptomCategory.PHYSICAL, "Tension mamaria",
        isBiomarker = true,
        biomarkerNote = "La tension mamaria en la fase post-Pico puede indicar niveles adecuados de progesterona. Si es intensa o prolongada, consultar al Profesional."
    ),
    PELVIC_PAIN(
        SymptomCategory.PHYSICAL, "Dolor pelvico o abdominal",
        isBiomarker = true,
        biomarkerNote = "El dolor a mitad del ciclo puede asociarse a la ovulacion (mittelschmerz). El dolor cronico requiere evaluacion medica."
    ),
    HEADACHE(SymptomCategory.PHYSICAL, "Cefalea / Dolor de cabeza"),
    BLOATING(SymptomCategory.PHYSICAL, "Hinchazon abdominal"),
    BACK_PAIN(SymptomCategory.PHYSICAL, "Dolor de espalda baja"),
    NAUSEA(SymptomCategory.PHYSICAL, "Nauseas"),
    SKIN_CHANGES(SymptomCategory.PHYSICAL, "Cambios en la piel (acne, grasa)"),

    // EMOTIONAL
    IRRITABILITY(SymptomCategory.EMOTIONAL, "Irritabilidad",
        isBiomarker = true,
        biomarkerNote = "La irritabilidad marcada en la fase pre-menstrual puede relacionarse con SPM y niveles hormonales."
    ),
    ANXIETY(SymptomCategory.EMOTIONAL, "Ansiedad o nerviosismo"),
    SAD_MOOD(SymptomCategory.EMOTIONAL, "Estado de animo bajo"),
    MOOD_SWINGS(SymptomCategory.EMOTIONAL, "Cambios de humor"),
    EMOTIONAL_SENSITIVITY(SymptomCategory.EMOTIONAL, "Sensibilidad emocional elevada"),
    WELLBEING(SymptomCategory.EMOTIONAL, "Bienestar / Buen animo"),

    // UNUSUAL BLEEDING — never triggers new cycle
    PREMENSTRUAL_SPOTTING(
        SymptomCategory.UNUSUAL_BLEEDING, "Manchado premenstrual (cafe/marron)",
        isBiomarker = true,
        biomarkerNote = "El manchado marron antes de la menstruacion es un biomarcador de niveles bajos de progesterona segun el manual Creighton. Consultar al Profesional de FertilityCare."
    ),
    INTERMENSTRUAL_BLEEDING(
        SymptomCategory.UNUSUAL_BLEEDING, "Sangrado intermenstrual",
        isBiomarker = true,
        biomarkerNote = "El sangrado entre periodos siempre debe evaluarse medicamente. El manual indica agregar 3 dias de fertilidad despues del ultimo dia de sangrado inusual."
    ),
    UNUSUAL_DISCHARGE(
        SymptomCategory.UNUSUAL_BLEEDING, "Flujo inusual (color, olor)",
        isBiomarker = true,
        biomarkerNote = "Un flujo con olor fuerte o color inusual puede indicar infeccion. El manual recomienda consultar al medico."
    ),

    // ENERGY
    HIGH_ENERGY(SymptomCategory.ENERGY, "Energia alta / Vitalidad"),
    LOW_ENERGY(SymptomCategory.ENERGY, "Cansancio / Fatiga"),
    POOR_SLEEP(SymptomCategory.ENERGY, "Mal descanso nocturno"),
    GOOD_SLEEP(SymptomCategory.ENERGY, "Buen descanso nocturno"),
    EXERCISE(SymptomCategory.ENERGY, "Actividad fisica realizada"),

    // NUTRITION
    BALANCED_EATING(SymptomCategory.NUTRITION, "Alimentacion equilibrada"),
    SUGAR_CRAVING(SymptomCategory.NUTRITION, "Antojos de dulce / carbohidratos"),
    APPETITE_LOSS(SymptomCategory.NUTRITION, "Poco apetito"),
    GOOD_HYDRATION(SymptomCategory.NUTRITION, "Buena hidratacion"),
    ALCOHOL(SymptomCategory.NUTRITION, "Consumo de alcohol")
}

// ── Intensity scale ───────────────────────────────────────────────────────────

enum class SymptomIntensity(val label: String, val value: Int) {
    MILD    ("Leve",     1),
    MODERATE("Moderado", 2),
    SEVERE  ("Intenso",  3)
}

// ── Daily symptom record — stored in Room ─────────────────────────────────────

@Entity(
    tableName = "daily_symptoms",
    indices   = [Index(value = ["userId", "date"])]
)
data class DailySymptom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val date: Long,
    val cycleDay: Int,
    // Stored as comma-separated "SYMPTOM_TYPE:INTENSITY" pairs
    // e.g. "BREAST_TENSION:2,HEADACHE:1,LOW_ENERGY:3"
    val symptomsEncoded: String = "",
    val notes: String = "",           // free text note
    val isUnusualBleeding: Boolean = false  // flag — never triggers new cycle
) {
    // Parse symptomsEncoded into a map
    val symptoms: Map<SymptomType, SymptomIntensity> get() {
        if (symptomsEncoded.isBlank()) return emptyMap()
        return symptomsEncoded.split(",").mapNotNull { pair ->
            val parts = pair.split(":")
            if (parts.size != 2) return@mapNotNull null
            val type      = runCatching { SymptomType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val intensity = runCatching { SymptomIntensity.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            type to intensity
        }.toMap()
    }

    companion object {
        fun encodeSymptoms(map: Map<SymptomType, SymptomIntensity>): String =
            map.entries.joinToString(",") { "${it.key.name}:${it.value.name}" }
    }
}

// ── Biomarker analysis result ─────────────────────────────────────────────────

data class BiomarkerAlert(
    val type: SymptomType,
    val title: String,
    val message: String,
    val severity: AlertSeverity
)

object BiomarkerAnalyzer {

    fun analyze(
        recentSymptoms: List<DailySymptom>,
        analysis: CycleAnalysis?
    ): List<BiomarkerAlert> {
        val alerts = mutableListOf<BiomarkerAlert>()
        val last14 = recentSymptoms.takeLast(14)

        // Premenstrual spotting — low progesterone
        val hasSpotting = last14.any { record ->
            record.symptoms.keys.contains(SymptomType.PREMENSTRUAL_SPOTTING) &&
                    (analysis?.currentPhase == CyclePhase.POST_PEAK ||
                            analysis?.currentPhase == CyclePhase.POST_PEAK_123)
        }
        if (hasSpotting) {
            alerts += BiomarkerAlert(
                type     = SymptomType.PREMENSTRUAL_SPOTTING,
                title    = "Manchado premenstrual detectado",
                message  = "El manchado marron o cafe en la fase post-Pico es un biomarcador de niveles bajos de progesterona segun el manual Creighton. Se recomienda consultar con el Profesional de FertilityCare.",
                severity = AlertSeverity.WARNING
            )
        }

        // Repeated breast tension in post-peak
        val breastCount = last14.count { it.symptoms.containsKey(SymptomType.BREAST_TENSION) }
        if (breastCount >= 5 && analysis?.currentPhase == CyclePhase.POST_PEAK) {
            alerts += BiomarkerAlert(
                type     = SymptomType.BREAST_TENSION,
                title    = "Tension mamaria frecuente",
                message  = "Has registrado tension mamaria $breastCount dias en los ultimos 14. En la fase post-Pico puede indicar niveles elevados de estrogeno o bajos de progesterona.",
                severity = AlertSeverity.INFO
            )
        }

        // Unusual bleeding — always flag
        val unusualBleeding = last14.filter { it.isUnusualBleeding }
        if (unusualBleeding.isNotEmpty()) {
            alerts += BiomarkerAlert(
                type     = SymptomType.INTERMENSTRUAL_BLEEDING,
                title    = "Sangrado inusual registrado",
                message  = "Registraste sangrado inusual en los ultimos dias. El manual indica que siempre debe evaluarse medicamente. Recuerda que el sangrado inusual agrega 3 dias de fertilidad.",
                severity = AlertSeverity.WARNING
            )
        }

        // Repeated severe PMS symptoms
        val severePms = last14.count { record ->
            record.symptoms.any { (type, intensity) ->
                type in listOf(SymptomType.IRRITABILITY, SymptomType.MOOD_SWINGS,
                    SymptomType.BLOATING, SymptomType.HEADACHE) &&
                        intensity == SymptomIntensity.SEVERE
            }
        }
        if (severePms >= 4) {
            alerts += BiomarkerAlert(
                type     = SymptomType.IRRITABILITY,
                title    = "Patron de SPM intenso",
                message  = "Has registrado sintomas severos de sindrome premenstrual en $severePms de los ultimos 14 dias. El manual Creighton asocia el SPM intenso con desequilibrios hormonales tratables.",
                severity = AlertSeverity.INFO
            )
        }

        return alerts
    }
}