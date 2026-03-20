package com.devdavinic.creightonapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devdavinic.creightonapp.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// =============================================================================
// AI CHAT VIEWMODEL - Module 4
// Calls the Anthropic API with full cycle context injected in the system prompt
// =============================================================================

class AiChatViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _contextSummary = MutableStateFlow("Cargando contexto...")
    val contextSummary: StateFlow<String> = _contextSummary.asStateFlow()

    init {
        // Update context summary whenever analysis changes
        viewModelScope.launch {
            mainViewModel.cycleAnalysis.collect { analysis ->
                _contextSummary.value = AiContextBuilder.buildContextSummary(analysis)
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            id      = UUID.randomUUID().toString(),
            role    = ChatRole.USER,
            content = userText.trim()
        )

        // Add user message + loading placeholder
        val loadingMessage = ChatMessage(
            id        = UUID.randomUUID().toString(),
            role      = ChatRole.ASSISTANT,
            content   = "",
            isLoading = true
        )
        _messages.value = _messages.value + userMessage + loadingMessage
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val analysis      = mainViewModel.cycleAnalysis.value
                val recentRecords = mainViewModel.currentCycleRecords.value

                val systemPrompt = AiContextBuilder.buildSystemPrompt(analysis, recentRecords)
                val reply        = callClaudeApi(systemPrompt, _messages.value.dropLast(1))

                // Replace loading placeholder with real response
                val assistantMessage = ChatMessage(
                    id      = loadingMessage.id,
                    role    = ChatRole.ASSISTANT,
                    content = reply
                )
                _messages.value = _messages.value.dropLast(1) + assistantMessage

            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id      = loadingMessage.id,
                    role    = ChatRole.ASSISTANT,
                    content = "Ocurrio un error al conectar con la asistente. Verifica tu conexion e intenta de nuevo.",
                    isError = true
                )
                _messages.value = _messages.value.dropLast(1) + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    // =========================================================================
    // ANTHROPIC API CALL
    // =========================================================================

    private suspend fun callClaudeApi(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

        val url        = URL("https://api.anthropic.com/v1/messages")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod        = "POST"
                doOutput             = true
                connectTimeout       = 30_000
                readTimeout          = 60_000
                setRequestProperty("Content-Type",      "application/json")
                setRequestProperty("anthropic-version", "2023-06-01")
                // API key is injected via BuildConfig — set in local.properties
                setRequestProperty("x-api-key", com.devdavinic.creightonapp.BuildConfig.ANTHROPIC_API_KEY)
            }

            // Build messages array from conversation history
            val messagesArray = JSONArray()
            conversationHistory
                .filter { !it.isLoading && !it.isError }
                .forEach { msg ->
                    val msgObj = JSONObject()
                    msgObj.put("role",    if (msg.role == ChatRole.USER) "user" else "assistant")
                    msgObj.put("content", msg.content)
                    messagesArray.put(msgObj)
                }

            val body = JSONObject().apply {
                put("model",      "claude-sonnet-4-20250514")
                put("max_tokens", 1024)
                put("system",     systemPrompt)
                put("messages",   messagesArray)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream       = if (responseCode == 200) connection.inputStream
            else connection.errorStream

            val responseText = stream.bufferedReader(Charsets.UTF_8).readText()

            if (responseCode != 200) {
                throw Exception("API error $responseCode: $responseText")
            }

            val responseJson = JSONObject(responseText)
            val contentArray = responseJson.getJSONArray("content")
            val firstContent = contentArray.getJSONObject(0)
            firstContent.getString("text")

        } finally {
            connection.disconnect()
        }
    }
}

// =============================================================================
// FACTORY
// =============================================================================

class AiChatViewModelFactory(
    private val mainViewModel: MainViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiChatViewModel(mainViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}