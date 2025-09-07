package com.romankozak.forwardappmobile.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.domain.Message
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AI_CHAT_DEBUG"

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
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

        val userMessage = ChatMessage(messageText, isFromUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }
        _userInput.value = ""

        viewModelScope.launch {
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

            // Add system message to the beginning of the history
            val systemMessage = Message(
                role = "system",
                content = "You are a helpful assistant who answers concisely and accurately."
            )

            val history = listOf(systemMessage) + _uiState.value.messages.mapNotNull { msg ->
                if (!msg.isError && !msg.isStreaming) {
                    Message(role = if (msg.isFromUser) "user" else "assistant", content = msg.text)
                } else null
            }

            val initialAssistantMessage = ChatMessage(
                text = "",
                isFromUser = false,
                isStreaming = true
            )
            _uiState.update { it.copy(messages = it.messages + initialAssistantMessage) }

            try {
                var fullResponse = ""
                var chunkCount = 0
                ollamaService.generateChatResponseStream(ollamaUrl, smartModel, history)
                    .collect { chunk ->
                        chunkCount++
                        fullResponse += chunk
                        Log.d(TAG, "ViewModel received chunk #$chunkCount: '$chunk'")
                        Log.d(TAG, "Full response so far: '$fullResponse'")

                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            val lastMessageIndex = updatedMessages.lastIndex

                            if (lastMessageIndex >= 0 &&
                                !updatedMessages[lastMessageIndex].isFromUser &&
                                updatedMessages[lastMessageIndex].isStreaming
                            ) {
                                updatedMessages[lastMessageIndex] = updatedMessages[lastMessageIndex].copy(
                                    text = fullResponse
                                )
                                Log.d(TAG, "UI updated with message: '${fullResponse.take(50)}...'")
                            }

                            currentState.copy(messages = updatedMessages)
                        }
                    }

                Log.d(TAG, "Stream collection completed. Total chunks: $chunkCount")

                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.toMutableList()
                    val lastMessageIndex = updatedMessages.lastIndex

                    if (lastMessageIndex >= 0 &&
                        !updatedMessages[lastMessageIndex].isFromUser &&
                        updatedMessages[lastMessageIndex].isStreaming
                    ) {
                        updatedMessages[lastMessageIndex] = updatedMessages[lastMessageIndex].copy(
                            isStreaming = false
                        )
                        Log.d(TAG, "Message marked as completed")
                    }

                    currentState.copy(messages = updatedMessages, isLoading = false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during streaming: ${e.message}", e)
                _uiState.update { currentState ->
                    val messagesWithoutStreaming = currentState.messages.filter { !it.isStreaming }
                    val errorMessage = ChatMessage(
                        text = "Error: ${e.message ?: "Unknown error"}",
                        isFromUser = false,
                        isError = true
                    )
                    currentState.copy(messages = messagesWithoutStreaming + errorMessage, isLoading = false)
                }
            }
        }
    }
}