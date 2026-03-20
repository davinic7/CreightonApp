package com.devdavinic.creightonapp.model

// =============================================================================
// CHAT MESSAGE MODEL - Module 4
// =============================================================================

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

enum class ChatRole { USER, ASSISTANT }

/** Suggested quick questions shown at the start of the conversation */
object QuickQuestions {
    val list = listOf(
        "Que significa el codigo que registre hoy?",
        "Estoy en fase fertil o infertil ahora?",
        "Como identifico el Dia Pico?",
        "Que es el moco tipo Pico?",
        "Que significa la cuenta post-Pico 1-2-3?",
        "Por que es importante registrar al final del dia?",
        "Mi fase post-Pico es normal?",
        "Que son las estampas y como se usan?",
        "Que es S-P-I-C-E en el Creighton MODEL?",
        "Cuando debo consultar al Profesional de FertilityCare?"
    )
}