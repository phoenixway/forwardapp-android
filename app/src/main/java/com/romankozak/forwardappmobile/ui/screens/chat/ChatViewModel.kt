package com.romankozak.forwardappmobile.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "AI_CHAT_DEBUG"

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val translatedText: String? = null,
    val isTranslating: Boolean = false,
)

fun ChatMessageEntity.toChatMessage() =
    ChatMessage(
        id = this.id,
        text = this.text,
        isFromUser = this.isFromUser,
        isError = this.isError,
        timestamp = this.timestamp,
        isStreaming = this.isStreaming,
        translatedText = null,
        isTranslating = false,
    )

fun ChatMessage.toEntity() =
    ChatMessageEntity(
        id = this.id,
        text = this.text,
        isFromUser = this.isFromUser,
        isError = this.isError,
        timestamp = this.timestamp,
        isStreaming = this.isStreaming,
    )

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val systemPrompt: String = "",
    val roleTitle: String = "Assistant",
    val temperature: Float = 0.8f,
    val rolesHierarchy: List<RoleItem> = emptyList(),
    val smartModel: String = "",
    val availableModels: ModelsState = ModelsState.Loading,
)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val ollamaService: OllamaService,
        private val settingsRepo: SettingsRepository,
        private val chatRepo: ChatRepository,
        private val rolesRepo: RolesRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        private val _userInput = MutableStateFlow("")
        val userInput = _userInput.asStateFlow()

        init {
            viewModelScope.launch {
                val fiveFlows =
                    combine(
                        chatRepo.getChatHistory(),
                        settingsRepo.systemPromptFlow,
                        settingsRepo.roleTitleFlow,
                        settingsRepo.temperatureFlow,
                        rolesRepo.rolesHierarchyFlow,
                    ) { history, prompt, title, temp, roles ->
                        Triple(history, prompt, title) to (temp to roles)
                    }

                combine(fiveFlows, settingsRepo.ollamaSmartModelFlow) { fiveResults, model ->
                    val (history, prompt, title) = fiveResults.first
                    val (temp, roles) = fiveResults.second

                    _uiState.update { currentState ->
                        val existingTranslations =
                            currentState.messages
                                .filter { it.translatedText != null }
                                .associate { it.id to it.translatedText }

                        val newMessages =
                            history.map { entity ->
                                val chatMessage = entity.toChatMessage()
                                existingTranslations[chatMessage.id]?.let {
                                    chatMessage.copy(translatedText = it)
                                } ?: chatMessage
                            }
                        Log.d(TAG, "ViewModel State Updated: New message count = ${newMessages.size}, History size = ${history.size}")

                        currentState.copy(
                            messages = newMessages,
                            systemPrompt = prompt,
                            roleTitle = title,
                            temperature = temp,
                            rolesHierarchy = roles,
                            smartModel = model,
                        )
                    }
                }.collect()

                
            }
        }

        fun translateMessage(messageId: Long) {
            viewModelScope.launch {
                val message = _uiState.value.messages.find { it.id == messageId }
                if (message == null || message.text.isBlank()) return@launch

                _uiState.update { currentState ->
                    currentState.copy(
                        messages =
                            currentState.messages.map {
                                if (it.id == messageId) it.copy(isTranslating = true) else it
                            },
                    )
                }

                try {
                    val options =
                        TranslatorOptions
                            .Builder()
                            .setSourceLanguage(TranslateLanguage.ENGLISH)
                            .setTargetLanguage(TranslateLanguage.UKRAINIAN)
                            .build()
                    val englishUkrainianTranslator = Translation.getClient(options)

                    val conditions =
                        DownloadConditions
                            .Builder()
                            .requireWifi()
                            .build()
                    englishUkrainianTranslator.downloadModelIfNeeded(conditions).await()

                    val translatedText = englishUkrainianTranslator.translate(message.text).await()

                    _uiState.update { currentState ->
                        currentState.copy(
                            messages =
                                currentState.messages.map {
                                    if (it.id == messageId) {
                                        it.copy(translatedText = translatedText, isTranslating = false)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                    englishUkrainianTranslator.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Translation failed", e)
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages =
                                currentState.messages.map {
                                    if (it.id == messageId) {
                                        it.copy(
                                            translatedText = "Помилка перекладу: ${e.localizedMessage}",
                                            isTranslating = false,
                                        )
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                }
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
                result
                    .onSuccess { models ->
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

                val serviceIntent =
                    Intent(context, GenerationService::class.java).apply {
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

        fun updateSystemPromptAndTitle(
            newPrompt: String,
            newTitle: String,
        ) {
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

        fun exportChat(): String =
            _uiState.value.messages.joinToString("\n\n") { msg ->
                val prefix = if (msg.isFromUser) "[USER]" else "[ASSISTANT] - ${_uiState.value.roleTitle}"
                "$prefix\n${msg.text}"
            }

        fun startStreamingTest() {
            if (uiState.value.messages.any { it.isStreaming }) return

            viewModelScope.launch {
                val userMessage = ChatMessage(text = "Test message for scrolling.", isFromUser = true)
                chatRepo.addMessage(userMessage.toEntity())

                val assistantMessage = ChatMessage(text = "", isFromUser = false, isStreaming = true)
                val assistantMessageId = chatRepo.addMessage(assistantMessage.toEntity())

                val longText =
                    "Це дуже довгий тестовий текст, який емулює відповідь від великої мовної моделі. " +
                        "Кожне речення буде додаватися з невеликою затримкою, щоб ми могли чітко побачити, " +
                        "чи працює автоскрол коректно. Зараз ми перевіряємо, чи буде LazyColumn прокручуватися вниз " +
                        "разом із додаванням нового контенту. Якщо все налаштовано правильно, ви повинні бачити " +
                        "останнє згенероване слово внизу екрана. Текст повинен продовжувати з'являтися, " +
                        "і скрол не повинен зупинятися чи стрибати нагору. Ще трохи тексту для маси. " +
                        "Один, два, три, чотири, п'ять. Перевірка завершується."

                val words = longText.split(" ")
                var currentText = ""

                for (word in words) {
                    kotlinx.coroutines.delay(100)
                    currentText += "$word "
                    chatRepo.updateMessageText(assistantMessageId, currentText)
                }

                chatRepo.updateMessageStreamingStatus(assistantMessageId, isStreaming = false)
            }
        }

        fun sendMockMessage() {
            val messageText = _userInput.value.trim()
            if (messageText.isBlank() || uiState.value.messages.any { it.isStreaming }) return

            viewModelScope.launch {
                Log.d(TAG, "sendMockMessage: Method called. User input: '$messageText'")

                val userMessage = ChatMessage(text = messageText, isFromUser = true)
                chatRepo.addMessage(userMessage.toEntity())
                _userInput.value = ""
                Log.d(TAG, "sendMockMessage: User message entity added to repository.")

                kotlinx.coroutines.delay(50)
                val assistantMessage = ChatMessage(text = "", isFromUser = false, isStreaming = true)
                val assistantMessageId = chatRepo.addMessage(assistantMessage.toEntity())
                Log.d(TAG, "sendMockMessage: Assistant placeholder message added.")

                val mockResponse = "Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол.Це імітація відповіді. Кожне слово з'являється із затримкою, щоб перевірити анімацію та автоскрол."
                val words = mockResponse.split(" ")
                var currentText = ""

                for (word in words) {
                    kotlinx.coroutines.delay(150)
                    currentText += "$word "
                    chatRepo.updateMessageText(assistantMessageId, currentText.trim())
                }

                chatRepo.updateMessageStreamingStatus(assistantMessageId, isStreaming = false)
                Log.d(TAG, "sendMockMessage: Mock response finished.")
            }
        }
    }
