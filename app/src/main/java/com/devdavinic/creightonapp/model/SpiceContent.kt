package com.devdavinic.creightonapp.model

// =============================================================================
// MODULE 5 - S-P-I-C-E EDUCATION MODEL
// Based on Chapters 2 and 8 of the Creighton manual
// S = Spiritual (eSpiritual)
// P = Physical (Fisico)
// I = Intellectual (Intelectual)
// C = Creative/Communicative (Creativo/Comunicativo)
// E = Emotional/Psychological (Emocional/Psicologico)
// =============================================================================

enum class SpiceDimension(val label: String, val icon: String) {
    SPIRITUAL    ("Espiritual",            "S"),
    PHYSICAL     ("Fisico",                "P"),
    INTELLECTUAL ("Intelectual",           "I"),
    CREATIVE     ("Creativo/Comunicativo", "C"),
    EMOTIONAL    ("Emocional",             "E")
}

data class SpiceTip(
    val id: String,
    val dimension: SpiceDimension,
    val title: String,
    val body: String,
    val applicablePhases: List<CyclePhase>,  // empty = applies to all phases
    val actionSuggestion: String? = null
)

// =============================================================================
// SPICE CONTENT REPOSITORY
// All tips extracted from Chapters 2 and 8 of the manual
// =============================================================================

object SpiceContent {

    val allTips: List<SpiceTip> = listOf(

        // =====================================================================
        // S - SPIRITUAL
        // =====================================================================

        SpiceTip(
            id = "S1",
            dimension = SpiceDimension.SPIRITUAL,
            title = "La oracion como base",
            body = "El manual destaca que la oracion es un componente esencial en el desarrollo y fortalecimiento de las necesidades espirituales de las personas y de la pareja.",
            applicablePhases = emptyList(),
            actionSuggestion = "Reserven un momento hoy para una oracion compartida, aunque sea breve."
        ),
        SpiceTip(
            id = "S2",
            dimension = SpiceDimension.SPIRITUAL,
            title = "La fertilidad como don",
            body = "El Creighton MODEL considera la fertilidad como parte de la salud y no como una enfermedad. Apreciar la fertilidad es aceptarla como un proceso normal y saludable, un precioso don que hay que amar, respetar y utilizar sabiamente.",
            applicablePhases = emptyList(),
            actionSuggestion = "Reflexionen juntos sobre lo que significa para ustedes la fertilidad como parte de su vida."
        ),
        SpiceTip(
            id = "S3",
            dimension = SpiceDimension.SPIRITUAL,
            title = "El amor total",
            body = "Al usar el metodo natural, se le dice al conyuge: 'te acepto y te amo tal y como eres'. La pareja puede descubrir que en ultima instancia su amor es algo profundo y verdadero.",
            applicablePhases = listOf(CyclePhase.POST_PEAK, CyclePhase.PRE_PEAK),
            actionSuggestion = "Dediquen hoy un momento a expresar gratitud genuina el uno al otro."
        ),
        SpiceTip(
            id = "S4",
            dimension = SpiceDimension.SPIRITUAL,
            title = "Tiempo de continencia genital",
            body = "El periodo en que la pareja evita el contacto genital, a menudo se ve como un 'descanso' de la necesidad de actuar todas las noches. Es fisicamente mas saludable y tambien mas saludable a nivel emocional, espiritual y de relacion.",
            applicablePhases = listOf(CyclePhase.PRE_PEAK, CyclePhase.PEAK_DAY, CyclePhase.POST_PEAK_123),
            actionSuggestion = "Usen este tiempo para explorar formas no genitales de expresar su amor."
        ),

        // =====================================================================
        // P - PHYSICAL
        // =====================================================================

        SpiceTip(
            id = "P1",
            dimension = SpiceDimension.PHYSICAL,
            title = "No dormir separados",
            body = "Cuando las parejas estan evitando el contacto genital, no deben dormir en habitaciones separadas; deben dormir juntos, como lo hacen normalmente. La cercania fisica que afirma es muy importante.",
            applicablePhases = listOf(CyclePhase.PRE_PEAK, CyclePhase.PEAK_DAY, CyclePhase.POST_PEAK_123),
            actionSuggestion = "La cercania sin presion crea una intimidad genuina y profunda."
        ),
        SpiceTip(
            id = "P2",
            dimension = SpiceDimension.PHYSICAL,
            title = "El efecto luna de miel",
            body = "Cuando las parejas evitan el contacto genital durante el tiempo de la fertilidad, el inicio del tiempo de infertilidad crea una expectacion o un sentimiento muy especial ante la relacion intima genital. Este 'efecto luna de miel' es bien conocido.",
            applicablePhases = listOf(CyclePhase.POST_PEAK),
            actionSuggestion = "La espera hace que cada encuentro sea mas significativo y deseado."
        ),
        SpiceTip(
            id = "P3",
            dimension = SpiceDimension.PHYSICAL,
            title = "Abrazos y caricias que afirman",
            body = "Es deseable que las parejas se besen y abracen sin llegar al orgasmo. Besarse, abrazarse, tomarse de las manos o simplemente estar juntos pueden ser experiencias muy gratificantes. La 'caricia que afirma' expresa ternura y afecto.",
            applicablePhases = emptyList(),
            actionSuggestion = "Hoy busquen un momento para abrazarse sin ninguna otra intencion."
        ),
        SpiceTip(
            id = "P4",
            dimension = SpiceDimension.PHYSICAL,
            title = "Los abrazos creativos",
            body = "Los abrazos creativos son un medio de expresar fisicamente la cercania sin que tenga que llegarse al coito. Una de las experiencias mas emocionantes es quedarse dormido uno en brazos del otro sin que exista ninguna demanda de ir mas alla.",
            applicablePhases = listOf(CyclePhase.PRE_PEAK, CyclePhase.PEAK_DAY, CyclePhase.POST_PEAK_123),
            actionSuggestion = "Esta noche duermanse juntos en un abrazo sin presiones."
        ),
        SpiceTip(
            id = "P5",
            dimension = SpiceDimension.PHYSICAL,
            title = "Actividades fisicas compartidas",
            body = "Salir a caminar, establecer momentos especiales para hablar, ver juntos una pelicula, estas actividades crean vinculos y estimulan la comunicacion. Desarrollan la confianza en la relacion y aumentan la autoestima.",
            applicablePhases = emptyList(),
            actionSuggestion = "Planeen hoy una actividad fisica sencilla para hacer juntos esta semana."
        ),

        // =====================================================================
        // I - INTELLECTUAL
        // =====================================================================

        SpiceTip(
            id = "I1",
            dimension = SpiceDimension.INTELLECTUAL,
            title = "Comprender el ciclo juntos",
            body = "Cuando ambos esposos aprenden a entender el ciclo de la mujer, mejora la comunicacion porque los esposos deben hablarse para lograr su mision conjunta de planear la familia. Los varones deben escuchar a sus esposas en una manera en que no habian sido capaces en el pasado.",
            applicablePhases = emptyList(),
            actionSuggestion = "Compartan juntos la grafica de esta semana y expliquen los codigos registrados."
        ),
        SpiceTip(
            id = "I2",
            dimension = SpiceDimension.INTELLECTUAL,
            title = "Discutir prioridades",
            body = "La pareja puede discutir sus prioridades con respecto al uso del metodo. ¿Desean tener otro hijo en este momento? ¿Si no lo desean, por que? Hablar sobre sus intenciones con el uso del metodo es muy importante para desarrollar una actitud positiva.",
            applicablePhases = emptyList(),
            actionSuggestion = "Reserven 15 minutos esta semana para hablar sobre sus intenciones actuales."
        ),
        SpiceTip(
            id = "I3",
            dimension = SpiceDimension.INTELLECTUAL,
            title = "El metodo es prospectivo",
            body = "El Creighton MODEL es un metodo prospectivo que no se basa en ciclos pasados ni en calculos previos. Cada dia se observa y registra tal como se presenta. Esto significa que ambos pueden aprender juntos cada dia.",
            applicablePhases = emptyList(),
            actionSuggestion = "Lean juntos la nota educativa del dia en la pantalla de Prediccion."
        ),
        SpiceTip(
            id = "I4",
            dimension = SpiceDimension.INTELLECTUAL,
            title = "Entender la hormona del ciclo",
            body = "La progesterona es la hormona postovulatoria dominante. Esta hormona detiene la produccion del moco y es esencial para el soporte hormonal del embarazo. Entender esto ayuda a comprender el patron de fertilidad e infertilidad.",
            applicablePhases = listOf(CyclePhase.POST_PEAK, CyclePhase.POST_PEAK_123),
            actionSuggestion = "Vean juntos el indicador hormonal en la pantalla de Prediccion."
        ),

        // =====================================================================
        // C - CREATIVE / COMMUNICATIVE
        // =====================================================================

        SpiceTip(
            id = "C1",
            dimension = SpiceDimension.CREATIVE,
            title = "Gestos especiales",
            body = "Uno de los esposos podria hacer algo especial para el otro, tal vez una comida especial o una cena tranquila juntos. El hecho de que el esposo traiga flores a su esposa o haga algo especial que el sabe que a ella le gustara ayuda a desarrollar una comunion entre marido y mujer.",
            applicablePhases = listOf(CyclePhase.PRE_PEAK, CyclePhase.PEAK_DAY, CyclePhase.POST_PEAK_123),
            actionSuggestion = "Sorprendan al otro con un gesto pequeno pero pensado especialmente para el o ella."
        ),
        SpiceTip(
            id = "C2",
            dimension = SpiceDimension.CREATIVE,
            title = "Cartas de amor",
            body = "Algunas parejas escriben cartas de amor a su conyuge y encuentran que escribir lo que sienten es una forma especial de comunicarse. La esposa puede escribir una nota que diga 'Te amo' y ponerla en la cartera de su esposo.",
            applicablePhases = emptyList(),
            actionSuggestion = "Escriban hoy un mensaje corto expresando algo especifico que aman del otro."
        ),
        SpiceTip(
            id = "C3",
            dimension = SpiceDimension.CREATIVE,
            title = "Proyectos conjuntos",
            body = "Realizar proyectos conjuntos, organizarlos y planearlos crea la oportunidad de trabajar unidos de manera articulada. Algunos ejemplos de este tipo de actividades son los proyectos en el hogar, plantar un jardin, las salidas familiares.",
            applicablePhases = emptyList(),
            actionSuggestion = "Elijan un proyecto pequeno para hacer juntos este fin de semana."
        ),
        SpiceTip(
            id = "C4",
            dimension = SpiceDimension.CREATIVE,
            title = "Lista de formas de amor no genitales",
            body = "Se puede hacer individualmente una lista de diez formas de demostrar el amor que no sea en forma genital, e intercambiarlas y aprender de ellas la forma en que el otro piensa y siente. Esta actividad puede auxiliar a las parejas a formular en una forma creativa los rasgos de interaccion sexual no genital que son especiales para ellos.",
            applicablePhases = emptyList(),
            actionSuggestion = "Esta semana, cada uno escriba su lista y luego compartan lo que descubrieron."
        ),

        // =====================================================================
        // E - EMOTIONAL / PSYCHOLOGICAL
        // =====================================================================

        SpiceTip(
            id = "E1",
            dimension = SpiceDimension.EMOTIONAL,
            title = "Tiempo especial para hablar",
            body = "La pareja debe reservar un tiempo especial para poder hablar de lo que sucedio en el dia, u otras cosas que son de importancia para ellos. Este tiempo de comunicacion fundamenta una relacion matrimonial gozosa y duradera.",
            applicablePhases = emptyList(),
            actionSuggestion = "Esta noche, apaguen los telefonos 20 minutos y hablen solo entre ustedes."
        ),
        SpiceTip(
            id = "E2",
            dimension = SpiceDimension.EMOTIONAL,
            title = "Explorar y expresar sentimientos",
            body = "Las parejas deben explorar y expresar sus sentimientos. Es posible que la expresion de sus necesidades y deseos sea una experiencia nueva para ellos que hay que desarrollar y fomentar. Estos esfuerzos, hechos dentro del contexto de la confianza, pueden ser muy importantes.",
            applicablePhases = emptyList(),
            actionSuggestion = "Compartan algo que sientan pero que rara vez expresan en palabras."
        ),
        SpiceTip(
            id = "E3",
            dimension = SpiceDimension.EMOTIONAL,
            title = "Hablar sobre la frustracion",
            body = "Hay que permitir a las parejas exponer y hablar sobre sus frustraciones y que sepan que son normales. A traves del proceso de 'ventilar' la frustracion, la pareja puede comenzar a reconocer las preocupaciones importantes del otro y trabajar en la solucion.",
            applicablePhases = listOf(CyclePhase.PRE_PEAK, CyclePhase.PEAK_DAY, CyclePhase.POST_PEAK_123),
            actionSuggestion = "Si algo genera tension, hablenlo con respeto y sin buscar culpables."
        ),
        SpiceTip(
            id = "E4",
            dimension = SpiceDimension.EMOTIONAL,
            title = "Apertura total en la relacion",
            body = "Las parejas deben ser totalmente abiertas en su relacion. Al tiempo en que se 'abre' la relacion se crea un vinculo y el sentimiento de proximidad e intimidad se hace profundo. El contacto genital no es imprescindible para la creacion de este vinculo.",
            applicablePhases = emptyList(),
            actionSuggestion = "Compartan una esperanza o un temor que normalmente no se cuentan."
        ),
        SpiceTip(
            id = "E5",
            dimension = SpiceDimension.EMOTIONAL,
            title = "Sentido del humor",
            body = "Finalmente, la pareja debe tener sentido del humor. Ser capaces de reirse de uno mismo y del otro en el momento oportuno es una forma de liberar las tensiones. El CrMS solo se obtiene si las parejas se adentran en todas estas nuevas areas.",
            applicablePhases = emptyList(),
            actionSuggestion = "Busquen hoy un momento para reirse juntos sin ninguna razon especial."
        ),
        SpiceTip(
            id = "E6",
            dimension = SpiceDimension.EMOTIONAL,
            title = "La confianza nace de la comunicacion",
            body = "La confianza en la relacion surge del desarrollo de las formas no genitales de comunicacion. A traves de estas formas se logra una 'comun-union' entre el hombre y la mujer que es liberadora, amorosa y permanente.",
            applicablePhases = emptyList(),
            actionSuggestion = "Cada dia de este ciclo, practiquen al menos una forma no genital de conexion."
        )
    )

    /** Devuelve los tips mas relevantes para la fase actual del ciclo */
    fun getTipsForPhase(phase: CyclePhase): List<SpiceTip> {
        return allTips.filter { tip ->
            tip.applicablePhases.isEmpty() || phase in tip.applicablePhases
        }
    }

    /** Tip del dia: seleccionado deterministicamente segun el dia del ciclo */
    fun getDailyTip(cycleDay: Int, phase: CyclePhase): SpiceTip {
        val relevant = getTipsForPhase(phase)
        return relevant[cycleDay % relevant.size]
    }

    /** Tips por dimension */
    fun getTipsByDimension(dimension: SpiceDimension): List<SpiceTip> {
        return allTips.filter { it.dimension == dimension }
    }
}