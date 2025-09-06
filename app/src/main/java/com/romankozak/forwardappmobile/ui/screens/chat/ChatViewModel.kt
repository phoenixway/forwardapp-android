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

private var TAG="AI_CHAT_DEBUG"

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false // Додаємо прапорець для стрімінгу
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

        // 1. Додаємо повідомлення користувача до UI
        val userMessage = ChatMessage(messageText, isFromUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }
        _userInput.value = "" // Очищуємо поле вводу

        viewModelScope.launch {
            // Отримуємо налаштування з репозиторію
            val ollamaUrl = settingsRepo.ollamaUrlFlow.first()
            val smartModel = settingsRepo.ollamaSmartModelFlow.first()

            // Перевіряємо, чи налаштовані URL та модель
            if (ollamaUrl.isBlank() || smartModel.isBlank()) {
                val errorMessage = ChatMessage(
                    text = "Ollama URL or Smart Model is not configured in settings.",
                    isFromUser = false,
                    isError = true
                )
                _uiState.update { it.copy(messages = it.messages + errorMessage, isLoading = false) }
                return@launch
            }

            // Готуємо історію чату для API (виключаємо повідомлення з помилками та стрімінгові)
            val history = _uiState.value.messages.mapNotNull { msg ->
                if (!msg.isError && !msg.isStreaming) {
                    Message(role = if (msg.isFromUser) "user" else "assistant", content = msg.text)
                } else null
            }

            // 2. Додаємо початкове порожнє повідомлення асистента для стрімінгу
            val initialAssistantMessage = ChatMessage(
                text = "",
                isFromUser = false,
                isStreaming = true
            )
            _uiState.update { it.copy(messages = it.messages + initialAssistantMessage) }

            try {
                // 3. Запускаємо потік і оновлюємо UI з кожним отриманим чанком тексту
                var fullResponse = ""
                var chunkCount = 0
                ollamaService.generateChatResponseStream(ollamaUrl, smartModel, history)
                    .collect { chunk ->
                        chunkCount++
                        fullResponse += chunk
                        Log.d(TAG, "ViewModel received chunk #$chunkCount: '$chunk'")
                        Log.d(TAG, "Full response so far: '$fullResponse'")

                        // Оновлюємо UI, замінюючи останнє повідомлення асистента новим текстом
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            val lastMessageIndex = updatedMessages.lastIndex

                            // Перевіряємо, що останнє повідомлення дійсно від асистента і стрімінгове
                            if (lastMessageIndex >= 0 &&
                                !updatedMessages[lastMessageIndex].isFromUser &&
                                updatedMessages[lastMessageIndex].isStreaming) {

                                updatedMessages[lastMessageIndex] = updatedMessages[lastMessageIndex].copy(
                                    text = fullResponse
                                )

                                Log.d(TAG, "UI updated with message: '${fullResponse.take(50)}...'")
                            }

                            currentState.copy(messages = updatedMessages)
                        }
                    }

                Log.d(TAG, "Stream collection completed. Total chunks: $chunkCount")

                // 4. Після завершення стріму позначаємо повідомлення як завершене
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.toMutableList()
                    val lastMessageIndex = updatedMessages.lastIndex

                    if (lastMessageIndex >= 0 &&
                        !updatedMessages[lastMessageIndex].isFromUser &&
                        updatedMessages[lastMessageIndex].isStreaming) {

                        updatedMessages[lastMessageIndex] = updatedMessages[lastMessageIndex].copy(
                            isStreaming = false
                        )

                        Log.d(TAG, "Message marked as completed")
                    }

                    currentState.copy(messages = updatedMessages)
                }

            } catch (e: Exception) {
                // Обробка помилок під час потокової передачі
                Log.e(TAG, "Error during streaming: ${e.message}", e)

                // Видаляємо порожнє стрімінгове повідомлення і додаємо повідомлення про помилку
                _uiState.update { currentState ->
                    val messagesWithoutStreaming = currentState.messages.filter { !it.isStreaming }
                    val errorMessage = ChatMessage(
                        text = "Error: ${e.message}",
                        isFromUser = false,
                        isError = true
                    )
                    currentState.copy(messages = messagesWithoutStreaming + errorMessage)
                }
            } finally {
                // 5. Після завершення потоку вимикаємо індикатор завантаження
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}