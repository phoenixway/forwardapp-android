package com.romankozak.forwardappmobile.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import com.romankozak.forwardappmobile.data.repository.ChatRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.Message
import com.romankozak.forwardappmobile.domain.OllamaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AI_CHAT_DEBUG"

// Data class для UI-рівня
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

// Extension-функції для мапінгу
fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id = this.id,
    text = this.text,
    isFromUser = this.isFromUser,
    isError = this.isError,
    timestamp = this.timestamp,
    isStreaming = this.isStreaming
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = this.id,
    text = this.text,
    isFromUser = this.isFromUser,
    isError = this.isError,
    timestamp = this.timestamp,
    isStreaming = this.isStreaming
)


data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val systemPrompt: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val ollamaService: OllamaService,
    private val settingsRepo: SettingsRepository,
    private val chatRepo: ChatRepository, // INFO: Додано кінцеву кому
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _userInput = MutableStateFlow("")
    val userInput = _userInput.asStateFlow()

    init {
        viewModelScope.launch {
            // Комбінуємо історію чату та системний промпт
            combine(
                chatRepo.getChatHistory(),
                // ERROR: ВИПРАВЛЕНО - Припускаємо, що settingsRepo має 'systemPromptFlow'
                settingsRepo.systemPromptFlow,
            ) { history, prompt ->
                ChatUiState(
                    messages = history.map { it.toChatMessage() },
                    systemPrompt = prompt, // INFO: Додано кінцеву кому
                )
            }.collect { combinedState ->
                // З цим виправленням тип `combinedState` тепер правильно визначається як `ChatUiState`
                _uiState.value = _uiState.value.copy(
                    // ERROR: ВИПРАВЛЕНО - Поля 'messages' та 'systemPrompt' тепер доступні
                    messages = combinedState.messages,
                    systemPrompt = combinedState.systemPrompt, // INFO: Додано кінцеву кому
                )
            }
        }
    }

    fun onUserInputChange(text: String) {
        _userInput.value = text
    }

    fun sendMessage(regenerate: Boolean = false) {
        val messageText = _userInput.value.trim()
        if ((messageText.isBlank() && !regenerate) || _uiState.value.isLoading) return

        viewModelScope.launch {
            if (!regenerate) {
                val userMessage = ChatMessage(text = messageText, isFromUser = true)
                chatRepo.addMessage(userMessage.toEntity())
                _userInput.value = ""
            } else {
                // Видаляємо останню відповідь асистента перед регенерацією
                chatRepo.deleteLastAssistantMessage()
            }

            _uiState.update { it.copy(isLoading = true) }

            val ollamaUrl = settingsRepo.ollamaUrlFlow.first()
            val smartModel = settingsRepo.ollamaSmartModelFlow.first()

            if (ollamaUrl.isBlank() || smartModel.isBlank()) {
                val errorMessage = ChatMessage(
                    text = "Ollama URL or Smart Model is not configured in settings.",
                    isFromUser = false,
                    isError = true
                )
                chatRepo.addMessage(errorMessage.toEntity())
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val systemMessage = Message(role = "system", content = _uiState.value.systemPrompt)

            val history = listOf(systemMessage) + _uiState.value.messages
                .filter { !it.isError && !it.isStreaming }
                .map { msg ->
                    Message(role = if (msg.isFromUser) "user" else "assistant", content = msg.text)
                }

            val assistantMessage = ChatMessage(text = "", isFromUser = false, isStreaming = true)
            val assistantMessageId = chatRepo.addMessage(assistantMessage.toEntity())

            try {
                var fullResponse = ""
                ollamaService.generateChatResponseStream(ollamaUrl, smartModel, history)
                    .collect { chunk ->
                        fullResponse += chunk
                        val updatedMessage = ChatMessage(
                            id = assistantMessageId,
                            text = fullResponse,
                            isFromUser = false,
                            isStreaming = true
                        )
                        chatRepo.updateMessage(updatedMessage.toEntity())
                    }

                // Завершення стрімінгу
                val finalMessage = ChatMessage(
                    id = assistantMessageId,
                    text = fullResponse,
                    isFromUser = false,
                    isStreaming = false
                )
                chatRepo.updateMessage(finalMessage.toEntity())

            } catch (e: Exception) {
                Log.e(TAG, "Error during streaming: ${e.message}", e)
                val errorMessage = ChatMessage(
                    id = assistantMessageId,
                    text = "Error: ${e.message ?: "Unknown error"}",
                    isFromUser = false,
                    isError = true
                )
                chatRepo.updateMessage(errorMessage.toEntity())
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun regenerateLastResponse() {
        if (_uiState.value.messages.any { it.isFromUser }) {
            sendMessage(regenerate = true)
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            chatRepo.clearChat()
        }
    }

    fun updateSystemPrompt(newPrompt: String) {
        viewModelScope.launch {
            settingsRepo.setSystemPrompt(newPrompt)
        }
    }

    fun exportChat(): String {
        return _uiState.value.messages.joinToString("\n\n") { msg ->
            val prefix = if (msg.isFromUser) "[USER]" else "[ASSISTANT]"
            "$prefix\n${msg.text}"
        }
    }
}