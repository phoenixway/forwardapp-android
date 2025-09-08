package com.romankozak.forwardappmobile.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import com.romankozak.forwardappmobile.data.repository.ChatRepository
import com.romankozak.forwardappmobile.data.repository.RolesRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.GenerationService
import com.romankozak.forwardappmobile.domain.Message
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.domain.RoleItem
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val systemPrompt: String = "",
    val roleTitle: String = "Assistant",
    val temperature: Float = 0.8f,
    val rolesHierarchy: List<RoleItem> = emptyList(),
    val smartModel: String = "",
    val availableModels: ModelsState = ModelsState.Loading
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val ollamaService: OllamaService,
    private val settingsRepo: SettingsRepository,
    private val chatRepo: ChatRepository,
    private val rolesRepo: RolesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _userInput = MutableStateFlow("")
    val userInput = _userInput.asStateFlow()

    init {
        viewModelScope.launch {
            // --- ПОЧАТОК ЗМІНИ: Виправлено combine для 6 потоків шляхом вкладення ---
            val fiveFlows = combine(
                chatRepo.getChatHistory(),
                settingsRepo.systemPromptFlow,
                settingsRepo.roleTitleFlow,
                settingsRepo.temperatureFlow,
                rolesRepo.rolesHierarchyFlow,
            ) { history, prompt, title, temp, roles ->
                // Групуємо результати перших 5 потоків
                Triple(history, prompt, title) to (temp to roles)
            }

            // Комбінуємо результат перших 5 з 6-м потоком
            combine(fiveFlows, settingsRepo.ollamaSmartModelFlow) { fiveResults, model ->
                val (history, prompt, title) = fiveResults.first
                val (temp, roles) = fiveResults.second

                _uiState.update { currentState ->
                    currentState.copy(
                        messages = history.map { it.toChatMessage() },
                        systemPrompt = prompt,
                        roleTitle = title,
                        temperature = temp,
                        rolesHierarchy = roles,
                        smartModel = model
                    )
                }
            }.collect()
            // --- КІНЕЦЬ ЗМІНИ ---
        }
    }

    fun loadAvailableModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(availableModels = ModelsState.Loading) }
            val ollamaUrl = settingsRepo.ollamaUrlFlow.first()
            if (ollamaUrl.isBlank()) {
                _uiState.update { it.copy(availableModels = ModelsState.Error("Ollama URL is not set")) }
                return@launch
            }

            val result = ollamaService.getAvailableModels(ollamaUrl)
            result.onSuccess { models ->
                _uiState.update { it.copy(availableModels = ModelsState.Success(models)) }
            }.onFailure { error ->
                _uiState.update { it.copy(availableModels = ModelsState.Error("Error: ${error.message}")) }
            }
        }
    }

    fun selectSmartModel(modelName: String) {
        viewModelScope.launch {
            val currentFastModel = settingsRepo.ollamaFastModelFlow.first()
            settingsRepo.saveOllamaModels(fastModel = currentFastModel, smartModel = modelName)
        }
    }

    fun onUserInputChange(text: String) {
        _userInput.value = text
    }

    fun sendMessage(regenerate: Boolean = false) {
        val messageText = _userInput.value.trim()
        if ((messageText.isBlank() && !regenerate) || uiState.value.messages.any { it.isStreaming }) return

        viewModelScope.launch {
            if (regenerate) {
                chatRepo.deleteLastAssistantMessage()
            } else {
                val userMessage = ChatMessage(text = messageText, isFromUser = true)
                chatRepo.addMessage(userMessage.toEntity())
                _userInput.value = ""
            }

            val assistantMessage = ChatMessage(text = "", isFromUser = false, isStreaming = true)
            val assistantMessageId = chatRepo.addMessage(assistantMessage.toEntity())

            val serviceIntent = Intent(context, GenerationService::class.java).apply {
                putExtra("assistantMessageId", assistantMessageId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
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

    fun updateSystemPromptAndTitle(newPrompt: String, newTitle: String) {
        viewModelScope.launch {
            settingsRepo.setSystemPrompt(newPrompt)
            settingsRepo.setRoleTitle(newTitle)
        }
    }

    fun updateTemperature(newTemperature: Float) {
        viewModelScope.launch {
            settingsRepo.setTemperature(newTemperature)
        }
    }

    fun exportChat(): String {
        return _uiState.value.messages.joinToString("\n\n") { msg ->
            val prefix = if (msg.isFromUser) "[USER]" else "[ASSISTANT] - ${_uiState.value.roleTitle}"
            "$prefix\n${msg.text}"
        }
    }
}