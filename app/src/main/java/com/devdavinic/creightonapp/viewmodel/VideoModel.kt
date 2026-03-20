package com.devdavinic.creightonapp.model

// =============================================================================
// VIDEO MODEL
// Videos are stored in Firebase Storage.
// Paths follow the pattern: gs://creightondb-ff433.appspot.com/videos/<filename>
// Upload videos in Firebase Console > Storage > videos/ folder.
// The app downloads the URL at runtime using FirebaseStorage.getDownloadUrl().
// =============================================================================

enum class VideoCategory(val label: String, val icon: String, val description: String) {
    INTRO       ("Introduccion",        "🎓", "Que es el Modelo Creighton y como funciona"),
    OBSERVATION ("Observacion",         "🔍", "Como observar y registrar correctamente"),
    STAMPS      ("Estampas y Codigos",  "🎨", "Interpretacion de estampas y codigos oficiales"),
    FERTILITY   ("Fertilidad",          "🌸", "Fases fertil e infertil del ciclo"),
    PEAK        ("Dia Pico",            "⭐", "El Dia Pico y la ovulacion"),
    POST_PEAK   ("Post-Pico",           "🌿", "Fase post-Pico y progesterona"),
    SPICE       ("S-P-I-C-E",           "💑", "Dimension espiritual, fisica e intelectual"),
    SPECIAL     ("Casos Especiales",    "📋", "BIP, lactancia, peri-menopausia y mas")
}

data class VideoLesson(
    val id: String,
    val category: VideoCategory,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    // Path in Firebase Storage: gs://creightondb-ff433.appspot.com/videos/<storagePath>
    val storagePath: String,
    // Optional thumbnail path in Storage: gs://...appspot.com/thumbnails/<thumbnailPath>
    val thumbnailPath: String? = null,
    val order: Int = 0,
    val isForPartner: Boolean = false  // show in partner profile too
)

// =============================================================================
// VIDEO CATALOG
// All entries are placeholders — replace storagePath with actual filenames
// after uploading to Firebase Storage > videos/ folder.
// =============================================================================

object VideoCatalog {

    val all: List<VideoLesson> = listOf(

        // ── INTRO ─────────────────────────────────────────────────────────────
        VideoLesson(
            id              = "intro_01",
            category        = VideoCategory.INTRO,
            title           = "Que es el Modelo Creighton",
            description     = "Historia y fundamentos del NaProTRACKING. Por que es diferente a otros metodos.",
            durationMinutes = 8,
            storagePath     = "videos/intro_01_que_es_creighton.mp4",
            thumbnailPath   = "thumbnails/intro_01.jpg",
            order           = 1
        ),
        VideoLesson(
            id              = "intro_02",
            category        = VideoCategory.INTRO,
            title           = "La ciencia detras del metodo",
            description     = "Hormonal cycle, moco cervical y su relacion con la fertilidad.",
            durationMinutes = 10,
            storagePath     = "videos/intro_02_ciencia.mp4",
            thumbnailPath   = "thumbnails/intro_02.jpg",
            order           = 2,
            isForPartner    = true
        ),

        // ── OBSERVATION ───────────────────────────────────────────────────────
        VideoLesson(
            id              = "obs_01",
            category        = VideoCategory.OBSERVATION,
            title           = "Como observar el moco cervical",
            description     = "Tecnica correcta de observacion: cuando, como y que buscar en el papel.",
            durationMinutes = 12,
            storagePath     = "videos/obs_01_observar_moco.mp4",
            thumbnailPath   = "thumbnails/obs_01.jpg",
            order           = 1
        ),
        VideoLesson(
            id              = "obs_02",
            category        = VideoCategory.OBSERVATION,
            title           = "Sensacion vs lubricacion",
            description     = "Diferencia entre sensacion vulvar seca, humeda y lubricante.",
            durationMinutes = 7,
            storagePath     = "videos/obs_02_sensacion.mp4",
            thumbnailPath   = "thumbnails/obs_02.jpg",
            order           = 2
        ),
        VideoLesson(
            id              = "obs_03",
            category        = VideoCategory.OBSERVATION,
            title           = "La regla del Mayor Signo",
            description     = "Siempre registrar el signo mas fertil del dia. Ejemplos practicos.",
            durationMinutes = 9,
            storagePath     = "videos/obs_03_mayor_signo.mp4",
            thumbnailPath   = "thumbnails/obs_03.jpg",
            order           = 3
        ),

        // ── STAMPS ────────────────────────────────────────────────────────────
        VideoLesson(
            id              = "stamps_01",
            category        = VideoCategory.STAMPS,
            title           = "Las estampas del sistema",
            description     = "Roja, verde solida, blanca con bebe, verde con bebe: cuando aplica cada una.",
            durationMinutes = 11,
            storagePath     = "videos/stamps_01_estampas.mp4",
            thumbnailPath   = "thumbnails/stamps_01.jpg",
            order           = 1
        ),
        VideoLesson(
            id              = "stamps_02",
            category        = VideoCategory.STAMPS,
            title           = "Como construir el codigo oficial",
            description     = "Formato del codigo: sangrado, sensacion, consistencia, color, frecuencia.",
            durationMinutes = 13,
            storagePath     = "videos/stamps_02_codigo.mp4",
            thumbnailPath   = "thumbnails/stamps_02.jpg",
            order           = 2
        ),

        // ── FERTILITY ─────────────────────────────────────────────────────────
        VideoLesson(
            id              = "fert_01",
            category        = VideoCategory.FERTILITY,
            title           = "La fase fertil: cuando comienza y termina",
            description     = "Inicio con el primer dia de moco, fin en el dia 3 post-Pico.",
            durationMinutes = 10,
            storagePath     = "videos/fert_01_fase_fertil.mp4",
            thumbnailPath   = "thumbnails/fert_01.jpg",
            order           = 1,
            isForPartner    = true
        ),
        VideoLesson(
            id              = "fert_02",
            category        = VideoCategory.FERTILITY,
            title           = "La fase infertil y su importancia",
            description     = "Post-Pico, dias secos y el significado del tiempo infertil en la pareja.",
            durationMinutes = 8,
            storagePath     = "videos/fert_02_fase_infertil.mp4",
            thumbnailPath   = "thumbnails/fert_02.jpg",
            order           = 2,
            isForPartner    = true
        ),

        // ── PEAK ──────────────────────────────────────────────────────────────
        VideoLesson(
            id              = "peak_01",
            category        = VideoCategory.PEAK,
            title           = "Que es el Dia Pico",
            description     = "Definicion, como identificarlo, por que se reconoce retrospectivamente.",
            durationMinutes = 9,
            storagePath     = "videos/peak_01_dia_pico.mp4",
            thumbnailPath   = "thumbnails/peak_01.jpg",
            order           = 1,
            isForPartner    = true
        ),
        VideoLesson(
            id              = "peak_02",
            category        = VideoCategory.PEAK,
            title           = "Pico y ovulacion: la relacion hormonal",
            description     = "LH surge, ovulacion y como el moco refleja el pico hormonal.",
            durationMinutes = 11,
            storagePath     = "videos/peak_02_hormonal.mp4",
            thumbnailPath   = "thumbnails/peak_02.jpg",
            order           = 2
        ),
        VideoLesson(
            id              = "peak_03",
            category        = VideoCategory.PEAK,
            title           = "El Doble Pico",
            description     = "Cuando ocurre, como reconocerlo y las preguntas P+3.",
            durationMinutes = 8,
            storagePath     = "videos/peak_03_doble_pico.mp4",
            thumbnailPath   = "thumbnails/peak_03.jpg",
            order           = 3
        ),

        // ── POST PEAK ─────────────────────────────────────────────────────────
        VideoLesson(
            id              = "pp_01",
            category        = VideoCategory.POST_PEAK,
            title           = "La fase post-Pico normal",
            description     = "Progesterona, duracion normal (9-17 dias) y el ciclo lúteo.",
            durationMinutes = 10,
            storagePath     = "videos/pp_01_post_pico.mp4",
            thumbnailPath   = "thumbnails/pp_01.jpg",
            order           = 1
        ),
        VideoLesson(
            id              = "pp_02",
            category        = VideoCategory.POST_PEAK,
            title           = "Manchado premenstrual como biomarcador",
            description     = "Manchado cafe antes de la menstruacion: senial de progesterona baja.",
            durationMinutes = 9,
            storagePath     = "videos/pp_02_manchado.mp4",
            thumbnailPath   = "thumbnails/pp_02.jpg",
            order           = 2
        ),

        // ── SPICE ─────────────────────────────────────────────────────────────
        VideoLesson(
            id              = "spice_01",
            category        = VideoCategory.SPICE,
            title           = "Que es S-P-I-C-E",
            description     = "Las 5 dimensiones de la intimidad conyugal en el Creighton.",
            durationMinutes = 12,
            storagePath     = "videos/spice_01_intro.mp4",
            thumbnailPath   = "thumbnails/spice_01.jpg",
            order           = 1,
            isForPartner    = true
        ),
        VideoLesson(
            id              = "spice_02",
            category        = VideoCategory.SPICE,
            title           = "Comunicacion en la fase fertil",
            description     = "Como hablar en pareja sobre la fertilidad y la intimidad.",
            durationMinutes = 14,
            storagePath     = "videos/spice_02_comunicacion.mp4",
            thumbnailPath   = "thumbnails/spice_02.jpg",
            order           = 2,
            isForPartner    = true
        ),

        // ── SPECIAL CASES ─────────────────────────────────────────────────────
        VideoLesson(
            id              = "sp_01",
            category        = VideoCategory.SPECIAL,
            title           = "Inicio del uso: el BIP",
            description     = "El Beginning Infertile Pattern y como manejarlo al empezar.",
            durationMinutes = 10,
            storagePath     = "videos/sp_01_bip.mp4",
            thumbnailPath   = "thumbnails/sp_01.jpg",
            order           = 1
        ),
        VideoLesson(
            id              = "sp_02",
            category        = VideoCategory.SPECIAL,
            title           = "Lactancia y el ciclo Creighton",
            description     = "Como registrar durante la lactancia exclusiva y el retorno del ciclo.",
            durationMinutes = 11,
            storagePath     = "videos/sp_02_lactancia.mp4",
            thumbnailPath   = "thumbnails/sp_02.jpg",
            order           = 2
        ),
        VideoLesson(
            id              = "sp_03",
            category        = VideoCategory.SPECIAL,
            title           = "Pre-menopausia y el ciclo",
            description     = "Cambios en el ciclo durante la peri-menopausia y como adaptarse.",
            durationMinutes = 9,
            storagePath     = "videos/sp_03_menopausia.mp4",
            thumbnailPath   = "thumbnails/sp_03.jpg",
            order           = 3
        )
    )

    fun byCategory(category: VideoCategory): List<VideoLesson> =
        all.filter { it.category == category }.sortedBy { it.order }

    fun forPartner(): List<VideoLesson> =
        all.filter { it.isForPartner }.sortedBy { it.category.ordinal }

    fun forCategory(category: VideoCategory) =
        all.filter { it.category == category }.sortedBy { it.order }
}