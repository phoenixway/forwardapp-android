package com.romankozak.forwardappmobile.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.Message
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private var TAG="AI_CHAT_DEBUG"

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val ollamaService: OllamaService,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _userInput = MutableStateFlow("")
    val userInput = _userInput.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage("Hello! How can I help you today?", isFromUser = false)
                )
            )
        }
    }


    fun onUserInputChange(text: String) {
        _userInput.value = text
    }

    fun sendMessage() {
        val messageText = _userInput.value.trim()
        if (messageText.isBlank() || _uiState.value.isLoading) return

        // Add user message to UI
        val userMessage = ChatMessage(messageText, isFromUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }
        _userInput.value = "" // Clear input field

        viewModelScope.launch {
            // Get settings
            val ollamaUrl = settingsRepo.ollamaUrlFlow.first()
            val smartModel = settingsRepo.ollamaSmartModelFlow.first()

            if (ollamaUrl.isBlank() || smartModel.isBlank()) {
                val errorMessage = ChatMessage(
                    text = "Ollama URL or Smart Model is not configured in settings.",
                    isFromUser = false,
                    isError = true
                )
                _uiState.update { it.copy(messages = it.messages + errorMessage, isLoading = false) }
                return@launch
            }

            // Prepare history for API
            val history = _uiState.value.messages.mapNotNull { msg ->
                if (!msg.isError) {
                    Message(role = if (msg.isFromUser) "user" else "assistant", content = msg.text)
                } else null
            }

            // Get response
            val result = ollamaService.generateChatResponse(ollamaUrl, smartModel, history)

            result.onSuccess { responseText ->
                val assistantMessage = ChatMessage(text = responseText, isFromUser = false)
                _uiState.update { it.copy(messages = it.messages + assistantMessage, isLoading = false) }
            }.onFailure { error ->
                val errorMessage = ChatMessage(
                    text = "Error: ${error.message}",
                    isFromUser = false,
                    isError = true
                )
                Log.e(TAG, "Error: ${error.message}")
                _uiState.update { it.copy(messages = it.messages + errorMessage, isLoading = false) }
            }
        }
    }
}