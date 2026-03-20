package com.devdavinic.creightonapp.model

// ═══════════════════════════════════════════════════════════════════════════════
//  ENUMS del Sistema Creighton / NaProTRACKING™
// ═══════════════════════════════════════════════════════════════════════════════

// ── A. Nivel de sangrado ──────────────────────────────────────────────────────

enum class BleedingLevel(val code: String, val label: String, val description: String) {
    H   ("H",  "H",   "Abundante"),
    M   ("M",  "M",   "Moderado"),
    L   ("L",  "L",   "Ligero"),
    VL  ("VL", "VL",  "Muy ligero / goteo"),
    B   ("B",  "B",   "Café, marrón o negro");

    /**
     * Los niveles L, VL y B requieren también registrar presencia/ausencia de moco
     * para evitar lecturas incorrectas de fertilidad.
     */
    val requiresMucusCheck: Boolean get() = this == L || this == VL || this == B
}

// ── B. Sensación vulvar (sin sangrado, sin lubricación) ───────────────────────

enum class Sensation(val code: String, val label: String, val description: String) {
    DRY      ("0",  "0",   "Seco"),
    WET      ("2",  "2",   "Húmedo sin lubricación"),
    VERY_WET ("2W", "2W",  "Mojado sin lubricación"),
    SHINY    ("4",  "4",   "Brillo sin lubricación");
}

// ── C. Consistencia / Elasticidad del moco ───────────────────────────────────

enum class MucusConsistency(val code: String, val label: String, val description: String) {
    STICKY  ("6",  "6",  "Pegajoso (~0.5 cm)"),
    TACKY   ("8",  "8",  "Ligoso (1–2 cm)"),
    STRETCHY("10", "10", "Elástico (2.5+ cm)");
}

// ── D. Color / Apariencia del moco ───────────────────────────────────────────

enum class MucusColor(val code: String, val label: String, val description: String) {
    CLOUDY      ("C",   "C",   "Nublado (blanco)"),
    CLOUDY_CLEAR("C/K", "C/K", "Nublado / Transparente"),
    CLEAR       ("K",   "K",   "Transparente o Claro"),
    GUMMY       ("G",   "G",   "Gomoso"),
    PASTY       ("P",   "P",   "Pastoso o Cremoso"),
    YELLOW      ("Y",   "Y",   "Amarillo pálido"),
    BROWN       ("B",   "B",   "Marrón o Café");
}

// ── E. Sensación con lubricación (Moco tipo Pico) ────────────────────────────

enum class LubricationSensation(val code: String, val label: String, val description: String) {
    WET_LUB      ("10DL", "10DL", "Húmedo con lubricación"),
    SHINY_LUB    ("10SL", "10SL", "Brillo con lubricación"),
    VERY_WET_LUB ("10WL", "10WL", "Mojado con lubricación");
}

// ── F. Frecuencia de observación ─────────────────────────────────────────────

enum class ObservationFrequency(val code: String, val label: String, val description: String) {
    X1 ("X1", "X1", "Visto 1 vez en el día"),
    X2 ("X2", "X2", "Visto 2 veces en el día"),
    X3 ("X3", "X3", "Visto 3 veces en el día"),
    AD ("AD", "AD", "A lo largo de todo el día (4 o más)");
}

// ── G. Tipo de estampa ────────────────────────────────────────────────────────

enum class StampType(val label: String) {
    RED        ("Menstrual"),
    GREEN_SOLID("Infértil / Seco"),
    WHITE_BABY ("Fértil – Moco"),
    GREEN_BABY ("Fértil – Post-Pico seco");
}

// ═══════════════════════════════════════════════════════════════════════════════
//  LÓGICA DE NEGOCIO: Construcción del código oficial y cálculo de la estampa
// ═══════════════════════════════════════════════════════════════════════════════

object CreightonLogic {

    /**
     * Construye el código alfanumérico oficial del día según el Sistema de
     * Registro de Flujo Vaginal (NaProTRACKING™).
     *
     * Regla del Mayor Signo: solo se guarda el signo más fértil del día.
     *
     * Formato resultante:
     *   • Día de sangrado puro        → "H" | "M" | "L" | "VL" | "B"
     *   • Día seco                    → "0 AD"
     *   • Día con moco                → "10 K AD"
     *   • Lubricación especial        → "10DL X2"
     *   • L/VL/B con moco presente    → "L 6 C X1"
     */
    fun buildOfficialCode(
        bleeding: BleedingLevel?,
        sensation: Sensation?,
        consistency: MucusConsistency?,
        color: MucusColor?,
        lubrication: LubricationSensation?,
        frequency: ObservationFrequency?,
        isPeakDay: Boolean
    ): String {
        val parts = mutableListOf<String>()

        // 1. Nivel de sangrado (si lo hay)
        if (bleeding != null) {
            parts += bleeding.code
        }

        // 2. Sensación con lubricación (tipo Pico) tiene prioridad sobre sensación normal
        when {
            lubrication != null -> parts += lubrication.code
            sensation   != null -> parts += sensation.code
        }

        // 3. Consistencia del moco (solo si hay sensación o sangrado leve + moco)
        if (consistency != null) parts += consistency.code

        // 4. Color del moco
        if (color != null) parts += color.code

        // 5. Marcador de Día Pico
        if (isPeakDay) parts += "P"

        // 6. Frecuencia (solo si hubo algún signo visible)
        val hasObservableSign = lubrication != null || sensation != null ||
                consistency != null || color != null
        if (frequency != null && hasObservableSign) parts += frequency.code

        return parts.joinToString(" ").ifBlank { "—" }
    }

    /**
     * Determina el tipo de estampa del día según la jerarquía del Modelo Creighton:
     *
     *   ROJO       → sangrado (H, M, L, VL, B)
     *   BLANCO+🍼  → moco cervical presente (signos fértiles visibles)
     *   VERDE+🍼   → seco pero en los 3 días post-Pico (postPeakCount 1–3)
     *   VERDE      → seco sin signos fértiles (infértil)
     */
    fun computeStamp(
        bleeding: BleedingLevel?,
        lubrication: LubricationSensation?,
        consistency: MucusConsistency?,
        color: MucusColor?,
        sensation: Sensation?,
        postPeakCount: Int
    ): StampType {
        // Sangrado siempre es rojo (incluye Brown "B")
        if (bleeding != null) return StampType.RED
        
        // El moco marrón ("B") también se considera sangrado en Creighton (estampa roja)
        if (color?.code == "B") return StampType.RED

        // Moco presente (lubricación, consistencia o color) → blanco con bebé (fértil)
        val hasMucusSign = lubrication != null || consistency != null || color != null
        if (hasMucusSign) return StampType.WHITE_BABY

        // Sensaciones húmedas sin moco también se consideran fértiles en la planilla
        if (sensation == Sensation.WET || sensation == Sensation.VERY_WET ||
            sensation == Sensation.SHINY) return StampType.WHITE_BABY

        // Seco pero dentro de los 3 días post-Pico → verde con bebé
        if (postPeakCount in 1..3) return StampType.GREEN_BABY

        // Seco sin signos → verde sólido (infértil)
        return StampType.GREEN_SOLID
    }

    /**
     * Devuelve true si la combinación actual corresponde a un signo tipo Pico
     * (moco transparente, elástico o lubricante) — candidato a Día Pico.
     */
    fun isPeakTypeSign(
        lubrication: LubricationSensation?,
        consistency: MucusConsistency?,
        color: MucusColor?
    ): Boolean {
        if (lubrication != null) return true
        if (consistency == MucusConsistency.STRETCHY) return true
        if (color == MucusColor.CLEAR || color == MucusColor.CLOUDY_CLEAR) return true
        return false
    }
}