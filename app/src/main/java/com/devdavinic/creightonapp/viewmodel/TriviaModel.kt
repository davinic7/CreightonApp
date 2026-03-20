package com.devdavinic.creightonapp.model

// =============================================================================
// TRIVIA MODEL
// 40+ questions covering the Creighton method manual
// Types: MULTIPLE_CHOICE, TRUE_FALSE, STAMP_GUESS
// =============================================================================

enum class TriviaType { MULTIPLE_CHOICE, TRUE_FALSE, STAMP_GUESS }
enum class TriviaDifficulty { EASY, MEDIUM, HARD }
enum class TriviaCategory(val label: String) {
    BASICS       ("Fundamentos"),
    OBSERVATION  ("Observacion"),
    STAMPS       ("Estampas y codigos"),
    FERTILITY    ("Fertilidad"),
    PEAK         ("Dia Pico"),
    POST_PEAK    ("Post-Pico"),
    SPICE        ("S-P-I-C-E"),
    SPECIAL      ("Casos especiales")
}

data class TriviaQuestion(
    val id: String,
    val type: TriviaType,
    val category: TriviaCategory,
    val difficulty: TriviaDifficulty,
    val question: String,
    val options: List<String>,        // for MULTIPLE_CHOICE and TRUE_FALSE
    val correctIndex: Int,            // index in options list
    val explanation: String,          // shown after answering
    // For STAMP_GUESS: describe the observation and ask which stamp
    val stampContext: String? = null
)

object TriviaBank {

    val all: List<TriviaQuestion> = listOf(

        // ── BASICS ────────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "b01", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.BASICS, difficulty = TriviaDifficulty.EASY,
            question = "El Modelo Creighton se basa en la observacion del moco cervical para identificar los dias fertiles e infertiles.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Correcto. El Modelo Creighton (NaProTRACKING) usa el moco cervical como biomarcador principal de fertilidad."
        ),
        TriviaQuestion(
            id = "b02", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.BASICS, difficulty = TriviaDifficulty.EASY,
            question = "Segun el manual, el registro debe realizarse:",
            options = listOf("Al inicio del dia", "A mitad del dia", "Al final del dia", "Cuando se recuerde"),
            correctIndex = 2,
            explanation = "El manual indica que el registro se realiza al FINAL del dia, registrando el signo MAS fertil observado durante toda la jornada."
        ),
        TriviaQuestion(
            id = "b03", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.BASICS, difficulty = TriviaDifficulty.EASY,
            question = "Que significa la sigla NaProTRACKING?",
            options = listOf(
                "Natural Progesterone Tracking",
                "Natural Procreative Technology Tracking",
                "NaPro Tracking and Recording",
                "Natural Profile Tracking"
            ),
            correctIndex = 1,
            explanation = "NaProTRACKING viene de Natural Procreative Technology Tracking — tecnologia de reproduccion natural."
        ),
        TriviaQuestion(
            id = "b04", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.BASICS, difficulty = TriviaDifficulty.MEDIUM,
            question = "La frecuencia AD significa que el signo fue observado en todos los momentos del dia (4 o mas veces).",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "AD (Ad libitum) indica que el signo fue observado en todas las observaciones del dia, es decir, 4 o mas veces."
        ),

        // ── OBSERVATION ───────────────────────────────────────────────────────
        TriviaQuestion(
            id = "o01", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.OBSERVATION, difficulty = TriviaDifficulty.EASY,
            question = "La regla del Mayor Signo indica que debes registrar:",
            options = listOf(
                "El primer signo del dia",
                "El signo mas comun del dia",
                "El signo MAS fertil observado en el dia",
                "El ultimo signo del dia"
            ),
            correctIndex = 2,
            explanation = "Siempre se registra el signo MAS fertil del dia, siguiendo la jerarquia: lubricacion > moco elastico > moco > sensacion > seco."
        ),
        TriviaQuestion(
            id = "o02", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.OBSERVATION, difficulty = TriviaDifficulty.MEDIUM,
            question = "La sensacion lubricante al limpiarse es siempre un signo Tipo Pico, aunque no se vea moco visible.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Correcto. La lubricacion (10DL, 10SL, 10WL) es siempre Tipo Pico independientemente de si hay moco visible o no."
        ),
        TriviaQuestion(
            id = "o03", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.OBSERVATION, difficulty = TriviaDifficulty.MEDIUM,
            question = "La consistencia '10' corresponde a moco:",
            options = listOf("Pegajoso (0.5 cm)", "Ligoso (1-2 cm)", "Elastico (mas de 2.5 cm)", "Sin elasticidad"),
            correctIndex = 2,
            explanation = "El codigo 10 indica moco muy elastico que se estira mas de 2.5 cm antes de romperse. Es signo Tipo Pico."
        ),
        TriviaQuestion(
            id = "o04", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.OBSERVATION, difficulty = TriviaDifficulty.HARD,
            question = "Si observas moco pegajoso en la manana y lubricacion en la tarde, que registras?",
            options = listOf(
                "6 (pegajoso) porque fue lo primero",
                "10DL (lubricacion) porque es el mas fertil",
                "Ambos signos por separado",
                "El promedio entre los dos"
            ),
            correctIndex = 1,
            explanation = "Regla del Mayor Signo: siempre registras el mas fertil. La lubricacion (10DL) es mas fertil que el moco pegajoso (6)."
        ),
        TriviaQuestion(
            id = "o05", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.OBSERVATION, difficulty = TriviaDifficulty.MEDIUM,
            question = "La sensacion seca (0) registrada como unico signo del dia corresponde a frecuencia X1.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 1,
            explanation = "Incorrecto. Cuando el signo del dia es seco (0), la frecuencia es automaticamente AD, independientemente de cuantas veces se observe."
        ),

        // ── STAMPS ────────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "s01", type = TriviaType.STAMP_GUESS,
            category = TriviaCategory.STAMPS, difficulty = TriviaDifficulty.EASY,
            question = "Una usuaria observa sangrado abundante hoy. Que estampa corresponde?",
            options = listOf("Estampa ROJA", "Estampa VERDE solida", "Estampa BLANCA con bebe", "Estampa VERDE con bebe"),
            correctIndex = 0,
            explanation = "Cualquier sangrado (H, M, L, VL, B) corresponde a estampa ROJA.",
            stampContext = "Sangrado: H (abundante)"
        ),
        TriviaQuestion(
            id = "s02", type = TriviaType.STAMP_GUESS,
            category = TriviaCategory.STAMPS, difficulty = TriviaDifficulty.EASY,
            question = "Dia completamente seco, sin moco ni sensacion especial. Que estampa corresponde?",
            options = listOf("Estampa ROJA", "Estampa VERDE solida", "Estampa BLANCA con bebe", "Estampa VERDE con bebe"),
            correctIndex = 1,
            explanation = "Un dia seco sin ningun signo fertil corresponde a estampa VERDE solida (infertil).",
            stampContext = "Sensation: 0 (seco)"
        ),
        TriviaQuestion(
            id = "s03", type = TriviaType.STAMP_GUESS,
            category = TriviaCategory.STAMPS, difficulty = TriviaDifficulty.MEDIUM,
            question = "Observacion: moco elastico transparente (10K). Que estampa corresponde?",
            options = listOf("Estampa ROJA", "Estampa VERDE solida", "Estampa BLANCA con bebe", "Estampa VERDE con bebe"),
            correctIndex = 2,
            explanation = "El moco elastico transparente (10K) es signo Tipo Pico = moco presente = estampa BLANCA con bebe.",
            stampContext = "10K AD"
        ),
        TriviaQuestion(
            id = "s04", type = TriviaType.STAMP_GUESS,
            category = TriviaCategory.STAMPS, difficulty = TriviaDifficulty.MEDIUM,
            question = "Es el segundo dia post-Pico (P+2), sin moco. Que estampa corresponde?",
            options = listOf("Estampa ROJA", "Estampa VERDE solida", "Estampa BLANCA con bebe", "Estampa VERDE con bebe"),
            correctIndex = 3,
            explanation = "Los dias P+1, P+2, P+3 sin moco corresponden a estampa VERDE con bebe. La fertilidad esta terminando pero la cuenta aun no se completo.",
            stampContext = "0 AD — P+2"
        ),
        TriviaQuestion(
            id = "s05", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.STAMPS, difficulty = TriviaDifficulty.HARD,
            question = "El codigo oficial '8C AD' corresponde a:",
            options = listOf(
                "Moco ligoso, nublado, observado todo el dia",
                "Moco elastico, claro, observado una vez",
                "Lubricacion humeda, observada todo el dia",
                "Moco pegajoso, claro, observado dos veces"
            ),
            correctIndex = 0,
            explanation = "8 = moco ligoso (se estira 1-2 cm), C = nublado, AD = observado en todas las observaciones del dia."
        ),
        TriviaQuestion(
            id = "s06", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.STAMPS, difficulty = TriviaDifficulty.HARD,
            question = "El codigo '10DL X2 P' indica moco lubricante observado dos veces y que es el Dia Pico.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Correcto. 10DL = lubricacion humeda (Tipo Pico), X2 = observado 2 veces, P = marca de Dia Pico."
        ),

        // ── FERTILITY ─────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "f01", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.FERTILITY, difficulty = TriviaDifficulty.EASY,
            question = "La fase fertil comienza cuando aparece el primer signo de moco cervical.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Correcto. La presencia de cualquier moco cervical indica inicio de la fase fertil."
        ),
        TriviaQuestion(
            id = "f02", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.FERTILITY, difficulty = TriviaDifficulty.MEDIUM,
            question = "Cuando termina la fase fertil en el Modelo Creighton?",
            options = listOf(
                "Al final del Dia Pico",
                "Al final del dia P+1",
                "Al final del dia P+3",
                "Cuando desaparece el moco"
            ),
            correctIndex = 2,
            explanation = "La fase fertil termina al FINAL del tercer dia post-Pico (P+3). Los dias P+1, P+2, P+3 son aun considerados potencialmente fertiles."
        ),
        TriviaQuestion(
            id = "f03", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.FERTILITY, difficulty = TriviaDifficulty.MEDIUM,
            question = "Una mujer puede tener mas de un ciclo de moco en el mismo ciclo menstrual.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Si, puede ocurrir el Doble Pico — dos ciclos de moco con un Pico cada uno. Es una variacion normal que requiere atencion especial."
        ),

        // ── PEAK ──────────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "p01", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.PEAK, difficulty = TriviaDifficulty.MEDIUM,
            question = "El Dia Pico se identifica:",
            options = listOf(
                "El mismo dia que ocurre",
                "Retrospectivamente, al dia siguiente cuando el moco baja de calidad",
                "Cuando la temperatura basal sube",
                "Al inicio del sangrado siguiente"
            ),
            correctIndex = 1,
            explanation = "El Dia Pico es el ULTIMO dia de moco tipo Pico. Se identifica retrospectivamente al dia siguiente, cuando el signo baja de calidad."
        ),
        TriviaQuestion(
            id = "p02", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.PEAK, difficulty = TriviaDifficulty.HARD,
            question = "Un dia completamente seco puede ser el Dia Pico.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 1,
            explanation = "No. El Dia Pico requiere moco tipo Pico (elastico, transparente o lubricacion). Un dia seco no puede ser Dia Pico."
        ),
        TriviaQuestion(
            id = "p03", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.PEAK, difficulty = TriviaDifficulty.MEDIUM,
            question = "Las preguntas del Doble Pico se hacen el dia:",
            options = listOf("P+1", "P+2", "P+3", "P+4"),
            correctIndex = 2,
            explanation = "Las preguntas del Doble Pico se realizan el dia P+3, cuando la cuenta obligatoria esta por terminar."
        ),

        // ── POST PEAK ─────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "pp01", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.POST_PEAK, difficulty = TriviaDifficulty.MEDIUM,
            question = "La duracion normal de la fase post-Pico es:",
            options = listOf("5 a 8 dias", "9 a 17 dias", "18 a 25 dias", "3 a 5 dias"),
            correctIndex = 1,
            explanation = "El manual indica que la fase post-Pico normal dura entre 9 y 17 dias. Menos de 9 puede indicar progesterona baja."
        ),
        TriviaQuestion(
            id = "pp02", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.POST_PEAK, difficulty = TriviaDifficulty.MEDIUM,
            question = "El manchado cafe o marron antes de la menstruacion es normal y no requiere atencion.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 1,
            explanation = "Incorrecto. El manchado premenstrual es un biomarcador de progesterona baja segun el manual Creighton. Debe ser evaluado por el Profesional de FertilityCare."
        ),
        TriviaQuestion(
            id = "pp03", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.POST_PEAK, difficulty = TriviaDifficulty.HARD,
            question = "Una fase post-Pico de 7 dias puede indicar:",
            options = listOf(
                "Un ciclo completamente normal",
                "Niveles bajos de progesterona",
                "Doble Pico inminente",
                "Inicio de la peri-menopausia siempre"
            ),
            correctIndex = 1,
            explanation = "Una fase post-Pico menor a 9 dias puede senalar niveles inadecuados de progesterona. El manual recomienda consultar al Profesional."
        ),

        // ── SPICE ─────────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "sp01", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.SPICE, difficulty = TriviaDifficulty.EASY,
            question = "En S-P-I-C-E, la 'C' representa:",
            options = listOf("Conyugal", "Creativo/Comunicativo", "Comunitario", "Consciente"),
            correctIndex = 1,
            explanation = "C = Creativo/Comunicativo. Las 5 dimensiones son: Spiritual, Physical, Intellectual, Creative/Communicative, Emotional."
        ),
        TriviaQuestion(
            id = "sp02", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.SPICE, difficulty = TriviaDifficulty.EASY,
            question = "S-P-I-C-E es un marco para el desarrollo de la intimidad conyugal integral.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Correcto. S-P-I-C-E (Espiritual, Fisico, Intelectual, Creativo/Comunicativo, Emocional) es el marco del Creighton para el crecimiento conyugal."
        ),

        // ── SPECIAL ───────────────────────────────────────────────────────────
        TriviaQuestion(
            id = "sc01", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.SPECIAL, difficulty = TriviaDifficulty.MEDIUM,
            question = "El BIP (Beginning Infertile Pattern) es:",
            options = listOf(
                "El primer dia de sangrado del ciclo",
                "El patron de moco inicial antes del primer uso del metodo",
                "Un patron de dias secos establecido al inicio del uso del metodo",
                "El primer Dia Pico registrado"
            ),
            correctIndex = 2,
            explanation = "El BIP es el patron basico de infertilidad al inicio del metodo — generalmente dias secos o un patron repetitivo de moco infertil."
        ),
        TriviaQuestion(
            id = "sc02", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.SPECIAL, difficulty = TriviaDifficulty.HARD,
            question = "Durante la lactancia exclusiva, el sistema Creighton no puede usarse.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 1,
            explanation = "Incorrecto. El Creighton tiene reglas especificas para la lactancia. Puede usarse pero requiere instruccion especial de un Profesional de FertilityCare."
        ),
        TriviaQuestion(
            id = "sc03", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.SPECIAL, difficulty = TriviaDifficulty.HARD,
            question = "El sangrado intermenstrual en el Creighton:",
            options = listOf(
                "Reinicia el conteo del ciclo",
                "Se ignora y no se registra",
                "Se registra pero NO reinicia el ciclo. Agrega 3 dias de fertilidad",
                "Indica siempre inicio de un nuevo ciclo"
            ),
            correctIndex = 2,
            explanation = "El sangrado intermenstrual NO reinicia el ciclo menstrual. Se registra como informacion clinica y agrega 3 dias de fertilidad desde el ultimo dia de sangrado."
        ),
        TriviaQuestion(
            id = "sc04", type = TriviaType.MULTIPLE_CHOICE,
            category = TriviaCategory.SPECIAL, difficulty = TriviaDifficulty.MEDIUM,
            question = "Cuando se considera que un nuevo ciclo menstrual ha comenzado?",
            options = listOf(
                "Con cualquier tipo de sangrado",
                "Solo con sangrado H o M (abundante o moderado)",
                "Con cualquier mancha de sangre",
                "Cuando la usuaria lo decide"
            ),
            correctIndex = 1,
            explanation = "Un nuevo ciclo comienza con sangrado H (abundante) o M (moderado). El sangrado L, VL y B no necesariamente inician un nuevo ciclo."
        ),
        TriviaQuestion(
            id = "sc05", type = TriviaType.TRUE_FALSE,
            category = TriviaCategory.SPECIAL, difficulty = TriviaDifficulty.HARD,
            question = "El autoexamen mamario debe realizarse el dia 7 del ciclo menstrual segun el manual Creighton.",
            options = listOf("Verdadero", "Falso"),
            correctIndex = 0,
            explanation = "Correcto. El manual Creighton recomienda el autoexamen mamario (AM) el dia 7 del ciclo, cuando los niveles hormonales son mas estables."
        )
    )

    fun byCategory(category: TriviaCategory) = all.filter { it.category == category }
    fun randomSet(count: Int = 10) = all.shuffled().take(count)
    fun byCategoryAndDifficulty(cat: TriviaCategory, diff: TriviaDifficulty) =
        all.filter { it.category == cat && it.difficulty == diff }
}

// Trivia session state
data class TriviaSession(
    val questions: List<TriviaQuestion>,
    val answers: Map<String, Int> = emptyMap(),       // questionId -> selectedIndex
    val currentIndex: Int = 0,
    val isComplete: Boolean = false
) {
    val currentQuestion get() = questions.getOrNull(currentIndex)
    val score get() = answers.count { (id, ans) ->
        questions.find { it.id == id }?.correctIndex == ans
    }
    val totalAnswered get() = answers.size
    val percentage get() = if (questions.isEmpty()) 0
    else (score * 100) / questions.size
}