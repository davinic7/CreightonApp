package com.devdavinic.creightonapp.model

// =============================================================================
// MODULE 7 - SPECIAL CASES MODEL
// Based on Appendix A of the Creighton manual
// Covers: yellow stamps, breastfeeding, post-pill, premenopause,
//         anovulation, postpartum, unusual bleeding
// =============================================================================

// -----------------------------------------------------------------------------
// SPECIAL CATEGORY - the reproductive situation the user is in
// -----------------------------------------------------------------------------

enum class SpecialCategory(
    val label: String,
    val shortLabel: String,
    val description: String,
    val icon: String,
    val usesYellowStamps: Boolean = false
) {
    NORMAL(
        label       = "Ciclo normal",
        shortLabel  = "Normal",
        description = "Ciclo menstrual regular sin circunstancias especiales.",
        icon        = "cycle"
    ),
    BREASTFEEDING_TOTAL(
        label       = "Lactancia total",
        shortLabel  = "Lactancia total",
        description = "Todo el sustento nutricional del bebe viene del seno materno.",
        icon        = "baby",
        usesYellowStamps = true
    ),
    BREASTFEEDING_PARTIAL(
        label       = "Lactancia parcial",
        shortLabel  = "Lactancia parcial",
        description = "Se inicio la alimentacion complementaria al seno materno.",
        icon        = "baby",
        usesYellowStamps = true
    ),
    POSTPARTUM_NO_BREASTFEEDING(
        label       = "Post-parto sin lactancia",
        shortLabel  = "Post-parto",
        description = "Periodo post-parto sin lactancia materna.",
        icon        = "postpartum"
    ),
    POST_PILL(
        label       = "Despues de dejar la pildora",
        shortLabel  = "Post-pildora",
        description = "Periodo de transicion despues de dejar los anticonceptivos hormonales.",
        icon        = "pill",
        usesYellowStamps = true
    ),
    PREMENOPAUSE(
        label       = "Premenopausia",
        shortLabel  = "Premenopausia",
        description = "Periodo previo a la menopausia con ciclos irregulares.",
        icon        = "premenopause",
        usesYellowStamps = true
    ),
    ANOVULATION(
        label       = "Anovulacion",
        shortLabel  = "Anovulacion",
        description = "Ciclos sin ovulacion o con ovulacion irregular.",
        icon        = "anovulation",
        usesYellowStamps = true
    ),
    POSTPARTUM_ABORTION(
        label       = "Post-aborto / Embarazo ectopico",
        shortLabel  = "Post-aborto",
        description = "Periodo de recuperacion despues de un aborto espontaneo o embarazo ectopico.",
        icon        = "recovery"
    ),
    INFERTILITY(
        label       = "Infertilidad",
        shortLabel  = "Infertilidad",
        description = "Parejas con dificultad para lograr el embarazo.",
        icon        = "infertility"
    ),
    UNUSUAL_BLEEDING(
        label       = "Sangrado inusual",
        shortLabel  = "Sangrado inusual",
        description = "Sangrado que la mujer percibe diferente al flujo menstrual normal.",
        icon        = "bleeding",
        usesYellowStamps = false
    )
}

// -----------------------------------------------------------------------------
// SPECIAL CASE INSTRUCTION
// Each instruction maps to a specific rule from Appendix A
// -----------------------------------------------------------------------------

data class SpecialInstruction(
    val id: String,
    val title: String,
    val body: String,
    val isWarning: Boolean = false,
    val actionRequired: String? = null
)

// -----------------------------------------------------------------------------
// YELLOW STAMP INSTRUCTION
// Used in: breastfeeding, post-pill, premenopause, anovulation
// -----------------------------------------------------------------------------

data class YellowStampInfo(
    val title: String,
    val description: String,
    val phases: List<String>,       // when yellow stamps apply
    val discontinueRule: String     // when to stop using them
)

// =============================================================================
// SPECIAL CASES CONTENT REPOSITORY
// All instructions extracted from Appendix A of the Creighton manual
// =============================================================================

object SpecialCasesContent {

    // -------------------------------------------------------------------------
    // YELLOW STAMPS INFO
    // Manual: used only with specific professional guidance
    // -------------------------------------------------------------------------

    val yellowStampInfo = YellowStampInfo(
        title = "Estampas Amarillas",
        description = "Las estampas amarillas se usan cuando hay un flujo cervical continuo y el Profesional de FertilityCare puede ayudar a identificar el moco caracteristico para distinguirlo de otros flujos. Solo se usan bajo indicacion especifica del Profesional.",
        phases = listOf(
            "Fase pre-Pico: al final del dia, dias alternos",
            "Fase post-Pico (despues del 4to dia): al final del dia, dias alternos",
            "Fase post-Pico (despues del 4to dia): al final del dia, todos los dias",
            "Fase post-Pico (despues del 4to dia): cualquier hora del dia"
        ),
        discontinueRule = "Descontinuar el uso de estampas amarillas en pre-Pico en ciclos regulares cuando el ciclo de moco es menor a 9 dias."
    )

    // -------------------------------------------------------------------------
    // INSTRUCTIONS BY CATEGORY
    // -------------------------------------------------------------------------

    fun getInstructions(category: SpecialCategory): List<SpecialInstruction> {
        return when (category) {
            SpecialCategory.NORMAL -> emptyList()

            SpecialCategory.BREASTFEEDING_TOTAL -> listOf(
                SpecialInstruction(
                    id    = "BT1",
                    title = "Cuando comenzar a graficar",
                    body  = "Se debe comenzar a graficar cuando el sangrado disminuya, usualmente tres semanas post-parto."
                ),
                SpecialInstruction(
                    id    = "BT2",
                    title = "Primer seguimiento",
                    body  = "El primer seguimiento debe programarse a las 5 semanas post-parto. En ese momento podran revisarse dos semanas de graficacion."
                ),
                SpecialInstruction(
                    id    = "BT3",
                    title = "Primeras 8 semanas (56 dias)",
                    body  = "Mientras este en lactancia total, las primeras ocho semanas (56 dias a partir del nacimiento del bebe) se consideran infertiles.",
                    isWarning = false
                ),
                SpecialInstruction(
                    id    = "BT4",
                    title = "Si se entra despues de la octava semana",
                    body  = "Si se entra al programa despues de la octava semana post-parto, debe evitarse el contacto genital durante dos semanas (si hay total sequedad) o cuatro semanas (si hay moco).",
                    isWarning = true,
                    actionRequired = "Consultar al Profesional de FertilityCare para instrucciones personalizadas."
                ),
                SpecialInstruction(
                    id    = "BT5",
                    title = "Relacion intima genital",
                    body  = "La relacion intima genital debe ser siempre al final del dia durante el primer ciclo menstrual normal."
                ),
                SpecialInstruction(
                    id    = "BT6",
                    title = "Instrucciones basicas",
                    body  = "Durante este periodo aplican las instrucciones basicas de periodo pre-Pico."
                ),
                SpecialInstruction(
                    id    = "BT7",
                    title = "Retorno variable del moco tipo Pico",
                    body  = "Al inicio del destete puede experimentarse un retorno variable del moco tipo Pico. El CrMS permite navegar en este periodo sin grandes dificultades.",
                    isWarning = false
                )
            )

            SpecialCategory.BREASTFEEDING_PARTIAL -> listOf(
                SpecialInstruction(
                    id    = "BP1",
                    title = "Primeras 8 semanas NO son infertiles",
                    body  = "A diferencia de la lactancia total, en lactancia parcial las primeras ocho semanas despues del nacimiento del bebe NO se consideran automaticamente infertiles.",
                    isWarning = true
                ),
                SpecialInstruction(
                    id    = "BP2",
                    title = "Cuando comenzar",
                    body  = "La graficacion comienza cuando se inicia la alimentacion complementaria. El primer seguimiento debe ser a las 5 semanas post-parto."
                ),
                SpecialInstruction(
                    id    = "BP3",
                    title = "Instrucciones especiales",
                    body  = "Si la intencion es evitar el embarazo, la relacion intima genital debe ser siempre al final del dia durante el primer ciclo menstrual normal. Durante el primer ciclo se aplican instrucciones basicas pre-Pico."
                ),
                SpecialInstruction(
                    id    = "BP4",
                    title = "Retorno de la ovulacion",
                    body  = "La fertilidad puede regresar antes del retorno de la menstruacion. Es especialmente importante estar atenta al moco durante este periodo.",
                    isWarning = true,
                    actionRequired = "Registrar con atencion todos los dias, especialmente los signos de moco."
                )
            )

            SpecialCategory.POSTPARTUM_NO_BREASTFEEDING -> listOf(
                SpecialInstruction(
                    id    = "PN1",
                    title = "Ovulacion precoz posible",
                    body  = "La ovulacion mas precoz reportada despues del nacimiento, en ausencia de lactancia, ha sido a los 27 dias. No puede presumirse un tiempo automatico de infertilidad.",
                    isWarning = true
                ),
                SpecialInstruction(
                    id    = "PN2",
                    title = "Cuando comenzar a graficar",
                    body  = "La graficacion debe comenzar cuando disminuya el sangrado, generalmente 3 a 4 semanas despues del parto."
                ),
                SpecialInstruction(
                    id    = "PN3",
                    title = "Primeras 4 semanas",
                    body  = "La pareja debe evitar el contacto genital durante las primeras cuatro semanas de graficacion para que la mujer pueda desarrollar confianza en sus observaciones."
                ),
                SpecialInstruction(
                    id    = "PN4",
                    title = "Primer seguimiento",
                    body  = "El primer seguimiento debe programarse dos semanas despues de iniciar la graficacion."
                ),
                SpecialInstruction(
                    id    = "PN5",
                    title = "Instrucciones especiales",
                    body  = "Si la intencion es evitar el embarazo: relacion intima al final del dia durante el primer ciclo normal. Durante el primer ciclo se aplican instrucciones pre-Pico. Despues del primer ciclo, instrucciones pre y post-Pico."
                )
            )

            SpecialCategory.POST_PILL -> listOf(
                SpecialInstruction(
                    id    = "PP1",
                    title = "Dejar la pildora inmediatamente",
                    body  = "Dejar de tomar la pildora inmediatamente despues de la sesion introductoria. Puede esperarse sangrado unos dias despues de dejarla."
                ),
                SpecialInstruction(
                    id    = "PP2",
                    title = "Primer periodo post-pildora",
                    body  = "El intervalo entre este sangrado y la primera menstruacion normal puede ser mas largo que el ciclo usual. El primer periodo normal sera generalmente mas abundante que el periodo que se presentaba con la pildora."
                ),
                SpecialInstruction(
                    id    = "PP3",
                    title = "Primera ovulacion",
                    body  = "La primera ovulacion despues de dejar la pildora puede ser mas dolorosa que lo usual."
                ),
                SpecialInstruction(
                    id    = "PP4",
                    title = "Posible anovulacion temporal",
                    body  = "La mujer puede no ovular temporalmente (o hasta permanentemente) despues de dejar la pildora, por lo que no habra menstruacion durante algun tiempo. La anovulacion post-pildora persistente puede ser una causa de infertilidad.",
                    isWarning = true,
                    actionRequired = "Si dura mas de 6 meses, consultar al medico."
                ),
                SpecialInstruction(
                    id    = "PP5",
                    title = "Primer ciclo menstrual normal",
                    body  = "Si la intencion es evitar el embarazo: la relacion intima genital debe ser siempre al final de los dias infertiles durante el primer ciclo menstrual normal. Durante el primer ciclo se aplican instrucciones basicas pre-Pico."
                ),
                SpecialInstruction(
                    id    = "PP6",
                    title = "Despues del primer ciclo normal",
                    body  = "Despues del primer ciclo menstrual normal, se aplican las instrucciones basicas pre y post-Pico."
                ),
                SpecialInstruction(
                    id    = "PP7",
                    title = "Si ocurre anovulacion o flujo continuo",
                    body  = "Si ocurre anovulacion, o si se presenta un flujo continuo, se siguen las instrucciones de esos casos especiales.",
                    isWarning = true,
                    actionRequired = "Consultar con el Profesional de FertilityCare para instrucciones personalizadas."
                )
            )

            SpecialCategory.PREMENOPAUSE -> listOf(
                SpecialInstruction(
                    id    = "PM1",
                    title = "Patrones irregulares",
                    body  = "Los patrones de ciclo pueden ser irregulares y/o anovulatorios. Los patrones de moco pueden ser mas irregulares con mas parches de moco y retorno variable a moco tipo Pico."
                ),
                SpecialInstruction(
                    id    = "PM2",
                    title = "Fase pre-Pico mas corta",
                    body  = "La fase pre-Pico del ciclo puede ser mas corta que lo usual. Es necesario estar alerta para detectar ovulacion temprana, observando con atencion la presencia o ausencia de moco durante la menstruacion.",
                    isWarning = true
                ),
                SpecialInstruction(
                    id    = "PM3",
                    title = "Fase post-Pico variable",
                    body  = "La fase post-Pico puede ser de duracion irregular entre un ciclo y otro."
                ),
                SpecialInstruction(
                    id    = "PM4",
                    title = "Sangrado inusual frecuente",
                    body  = "Puede observarse con mayor frecuencia sangrado inusual, por lo que pueden aplicarse las instrucciones correspondientes.",
                    isWarning = true,
                    actionRequired = "Siempre que se presente sangrado inusual, flujo continuo, sangrado intermenstrual o flujo mal oliente, ver al medico."
                ),
                SpecialInstruction(
                    id    = "PM5",
                    title = "Llegada a la menopausia",
                    body  = "Se puede decir que se ha llegado a la menopausia cuando pasa un año sin menstruacion."
                ),
                SpecialInstruction(
                    id    = "PM6",
                    title = "Instrucciones",
                    body  = "Se aplican las instrucciones basicas del metodo. Las estampas amarillas pueden ser necesarias con asesoria del Profesional.",
                    actionRequired = "Consultar al Profesional de FertilityCare para instrucciones personalizadas."
                )
            )

            SpecialCategory.ANOVULATION -> listOf(
                SpecialInstruction(
                    id    = "AV1",
                    title = "Periodos largos de sequedad",
                    body  = "Pueden observarse periodos largos de sequedad ocasionalmente con un proceso de formacion de moco de tipo ovulatorio o un retorno variable de moco tipo Pico disperso durante el ciclo."
                ),
                SpecialInstruction(
                    id    = "AV2",
                    title = "Retorno variable de moco tipo Pico",
                    body  = "El moco tipo Pico puede ir y volver varias veces. Se origina en el intento del cuerpo por ovular y el moco va y viene como resultado de niveles crecientes y decrecientes de estrogeno. No debe considerarse algo anormal sino una variacion fisiologica normal.",
                    isWarning = false
                ),
                SpecialInstruction(
                    id    = "AV3",
                    title = "Instrucciones de final del dia",
                    body  = "En cualquiera de estas situaciones se deben aplicar las instrucciones basicas en el entendido de que se trata de una situacion de final del dia."
                ),
                SpecialInstruction(
                    id    = "AV4",
                    title = "Estampas amarillas posibles",
                    body  = "Si existe un flujo continuo de moco, por lo general puede establecerse un patron de fertilidad e infertilidad y pueden utilizarse estampas amarillas con ayuda del Profesional.",
                    actionRequired = "Consultar al Profesional de FertilityCare antes de usar estampas amarillas."
                )
            )

            SpecialCategory.POSTPARTUM_ABORTION -> listOf(
                SpecialInstruction(
                    id    = "PA1",
                    title = "Cuando comenzar a graficar",
                    body  = "La graficacion debe comenzar cuando disminuya el sangrado. Esto es por lo general una semana despues del incidente."
                ),
                SpecialInstruction(
                    id    = "PA2",
                    title = "Primeras 4 semanas",
                    body  = "La pareja debe evitar el contacto genital durante las primeras cuatro semanas de graficacion para que la mujer pueda desarrollar confianza en sus observaciones."
                ),
                SpecialInstruction(
                    id    = "PA3",
                    title = "La ovulacion puede regresar pronto",
                    body  = "La ovulacion puede regresar en dos semanas. La supresion de la ovulacion despues de la perdida es muy corta.",
                    isWarning = true
                ),
                SpecialInstruction(
                    id    = "PA4",
                    title = "Primer seguimiento",
                    body  = "El primer seguimiento debe programarse dos semanas despues de iniciar la graficacion."
                ),
                SpecialInstruction(
                    id    = "PA5",
                    title = "Apoyo emocional",
                    body  = "Cuando una mujer sufre un aborto, ya sea espontaneo o provocado, o un embarazo ectopico, se experimenta un proceso de duelo. El Profesional de FertilityCare esta consciente de ese proceso y proporcionara apoyo durante ese periodo.",
                    isWarning = false
                )
            )

            SpecialCategory.INFERTILITY -> listOf(
                SpecialInstruction(
                    id    = "IF1",
                    title = "Evitar todo contacto genital el primer ciclo",
                    body  = "La pareja infertil debe evitar todo contacto genital durante el primer ciclo menstrual completo. Confiar en el aspecto del moco es tan importante para la pareja con infertilidad como para la que desea usar el metodo para evitar el embarazo.",
                    isWarning = true
                ),
                SpecialInstruction(
                    id    = "IF2",
                    title = "Registrar la elasticidad del moco",
                    body  = "Registrar la elasticidad del moco (2, 4, 6 cm / 1 pulgada, 2 pulgadas, 3 pulgadas, etc.) para una evaluacion mas precisa."
                ),
                SpecialInstruction(
                    id    = "IF3",
                    title = "Usar dias de mayor calidad",
                    body  = "Utilizar los dias de mayor cantidad y calidad de moco y los dos primeros dias que le siguen. Estos seran los dias de mayor fertilidad durante el ciclo menstrual.",
                    actionRequired = "Consultar al Profesional para identificar los mejores dias."
                ),
                SpecialInstruction(
                    id    = "IF4",
                    title = "Ciclo de moco limitado",
                    body  = "Un ciclo de moco limitado en cantidad y duracion sugiere una situacion de mayor riesgo de infertilidad. Al observar el ciclo de moco se puede identificar el riesgo y reducir la oportunidad de que esto ocurra, con el manejo medico adecuado.",
                    isWarning = true,
                    actionRequired = "Llevar la grafica al medico especializado en NaProTECHNOLOGY."
                )
            )

            SpecialCategory.UNUSUAL_BLEEDING -> listOf(
                SpecialInstruction(
                    id    = "UB1",
                    title = "Que es el sangrado inusual",
                    body  = "Es un sangrado que la mujer percibe diferente del flujo menstrual normal (ordinario). Un flujo menstrual real sigue a un evento ovulatorio y es muy caracteristico.",
                    isWarning = true
                ),
                SpecialInstruction(
                    id    = "UB2",
                    title = "Agregar 3 dias fertiles",
                    body  = "Cuando hay un sangrado inusual, hay que agregar tres dias mas de fertilidad despues del ultimo dia de sangrado inusual."
                ),
                SpecialInstruction(
                    id    = "UB3",
                    title = "Registrar presencia de moco",
                    body  = "Debe registrarse si se observa o no cualquier flujo de moco durante el sangrado inusual. La razon por la que el sangrado inusual se considera fertil es por que ocasionalmente se asocia este sangrado con la ovulacion."
                ),
                SpecialInstruction(
                    id    = "UB4",
                    title = "Graficar como fertil",
                    body  = "Graficar como fertil por 3 ciclos. Graficar la presencia o ausencia de moco durante el sangrado. Despues de tres ciclos continuar graficando el sangrado pero seguir las instrucciones basicas con base en la presencia o ausencia de moco.",
                    isWarning = false
                ),
                SpecialInstruction(
                    id    = "UB5",
                    title = "Consultar al medico",
                    body  = "Siempre que se presente sangrado inusual, flujo continuo, sangrado intermenstrual o un flujo que sea mal oliente, se debe ver al medico.",
                    isWarning = true,
                    actionRequired = "Consultar al medico. El sangrado inusual puede representar una gran variedad de problemas de salud ginecologica."
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // SHORT DESCRIPTION FOR EACH CATEGORY
    // -------------------------------------------------------------------------

    fun getCategoryNote(category: SpecialCategory): String = when (category) {
        SpecialCategory.NORMAL ->
            "Estas en un ciclo menstrual regular. Las instrucciones basicas del metodo aplican."
        SpecialCategory.BREASTFEEDING_TOTAL ->
            "La lactancia total suprime fisiologicamente la ovulacion. Las primeras 8 semanas post-parto se consideran infertiles. Graficar es esencial para detectar el retorno de la fertilidad."
        SpecialCategory.BREASTFEEDING_PARTIAL ->
            "Al iniciar la alimentacion complementaria la supresion de la ovulacion disminuye. La fertilidad puede regresar antes de la menstruacion."
        SpecialCategory.POSTPARTUM_NO_BREASTFEEDING ->
            "Sin lactancia, la ovulacion puede regresar tan pronto como 27 dias post-parto. No hay periodo automatico de infertilidad."
        SpecialCategory.POST_PILL ->
            "Despues de dejar los anticonceptivos hormonales el ciclo puede ser irregular. El primer ciclo normal establece la linea de base para las instrucciones."
        SpecialCategory.PREMENOPAUSE ->
            "Los ciclos tienden a hacerse mas cortos e irregulares. Las instrucciones basicas aplican aunque con mayor atencion a los cambios de patron."
        SpecialCategory.ANOVULATION ->
            "Sin ovulacion no hay Dia Pico verdadero. El moco puede presentar un patron de retorno variable. Las instrucciones de final del dia aplican."
        SpecialCategory.POSTPARTUM_ABORTION ->
            "La ovulacion puede regresar en dos semanas. Graficar desde que disminuye el sangrado y evitar contacto genital las primeras 4 semanas."
        SpecialCategory.INFERTILITY ->
            "El CrMS es el metodo mas preciso para identificar el momento de mayor fertilidad. Usar los dias de mayor cantidad y calidad de moco."
        SpecialCategory.UNUSUAL_BLEEDING ->
            "El sangrado inusual se considera fertil. Agregar 3 dias de fertilidad despues del ultimo dia de sangrado inusual."
    }
}