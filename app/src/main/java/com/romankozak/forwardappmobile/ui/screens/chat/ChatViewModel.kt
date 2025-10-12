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
import com.romankozak.forwardappmobile.data.database.models.ConversationEntity
import com.romankozak.forwardappmobile.data.repository.ChatRepository
import com.romankozak.forwardappmobile.data.repository.RolesRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.aichat.GenerationService
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import com.romankozak.forwardappmobile.domain.aichat.RoleItem
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "AI_CHAT_DEBUG"

data class ChatUiState(
    val currentConversation: ConversationEntity? = null,
    val drawerItems: List<DrawerItem> = emptyList(),
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
class ChatViewModel @Inject constructor(
    // private val ollamaService: OllamaService,
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
            chatRepo.getDrawerItems().collectLatest { drawerItems ->
                _uiState.update { it.copy(drawerItems = drawerItems) }
                if (uiState.value.currentConversation == null) {
                    val firstConversation = drawerItems.firstNotNullOfOrNull { item ->
                        when (item) {
                            is DrawerItem.Conversation -> item.conversationWithLastMessage.conversation
                            is DrawerItem.Folder -> item.conversations.firstOrNull()?.conversation
                        }
                    }
                    firstConversation?.let { setCurrentConversation(it.id) } ?: startNewChat()
                }
            }
        }

        viewModelScope.launch {
            uiState.map { it.currentConversation?.id }.distinctUntilChanged().collectLatest { conversationId ->
                if (conversationId != null) {
                    chatRepo.getChatHistory(conversationId).collectLatest { history ->
                        _uiState.update { currentState ->
                            val newMessages = history.map { it.toChatMessage(conversationId) }
                            currentState.copy(messages = newMessages)
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                settingsRepo.systemPromptFlow,
                settingsRepo.roleTitleFlow,
                settingsRepo.temperatureFlow,
                rolesRepo.rolesHierarchyFlow,
                settingsRepo.ollamaSmartModelFlow
            ) { prompt, title, temp, roles, smartModel ->
                _uiState.update {
                    it.copy(
                        systemPrompt = prompt,
                        roleTitle = title,
                        temperature = temp,
                        rolesHierarchy = roles,
                        smartModel = smartModel
                    )
                }
            }.collect{}
        }
        // loadAvailableModels()
    }

    fun setCurrentConversation(conversationId: Long) {
        viewModelScope.launch {
            val conversation = chatRepo.getConversationById(conversationId)
            _uiState.update { it.copy(currentConversation = conversation) }
        }
    }

    fun translateMessage(messageId: Long) {
        viewModelScope.launch {
            val message = _uiState.value.messages.find { it.id == messageId }
            if (message == null || message.text.isBlank()) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages.map {
                        if (it.id == messageId) it.copy(isTranslating = true) else it
                    },
                )
            }

            try {
                val options =
                    TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.UKRAINIAN)
                        .build()
                val englishUkrainianTranslator = Translation.getClient(options)

                val conditions = DownloadConditions.Builder().requireWifi().build()
                englishUkrainianTranslator.downloadModelIfNeeded(conditions).await()

                val translatedText = englishUkrainianTranslator.translate(message.text).await()

                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages.map {
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
                        messages = currentState.messages.map {
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
        // viewModelScope.launch {
        //     _uiState.update { it.copy(availableModels = ModelsState.Loading) }
        //     val url = settingsRepo.getOllamaUrl().first()
        //     if (url.isNullOrBlank()) {
        //         _uiState.update { it.copy(availableModels = ModelsState.Error("Server URL is not set")) }
        //         return@launch
        //     }
        //
        //     val result = ollamaService.getAvailableModels(url)
        //     result.onSuccess { models ->
        //         _uiState.update { it.copy(availableModels = ModelsState.Success(models)) }
        //     }.onFailure { error ->
        //         _uiState.update { it.copy(availableModels = ModelsState.Error("Error: ${error.message}")) }
        //     }
        // }
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

    fun sendMessage() {
        // val messageText = _userInput.value.trim()
        // val conversation = _uiState.value.currentConversation
        // if (messageText.isBlank() || uiState.value.messages.any { it.isStreaming } || conversation == null) return

        // viewModelScope.launch {
        //     val userMessage = ChatMessage(conversationId = conversation.id, text = messageText, isFromUser = true)
        //     chatRepo.addMessage(userMessage.toEntity())
        //     _userInput.value = ""

        //     val assistantMessage = ChatMessage(conversationId = conversation.id, text = "", isFromUser = false, isStreaming = true)
        //     val assistantMessageId = chatRepo.addMessage(assistantMessage.toEntity())

        //     val serviceIntent =
        //         Intent(context, GenerationService::class.java).apply {
        //             putExtra("assistantMessageId", assistantMessageId)
        //             putExtra("conversationId", conversation.id)
        //         }

        //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //         context.startForegroundService(serviceIntent)
        //     } else {
        //         context.startService(serviceIntent)
        //     }
        // }
    }

    fun regenerateLastResponse() {
        val conversation = _uiState.value.currentConversation
        if (conversation == null) return

        viewModelScope.launch {
            chatRepo.deleteLastAssistantMessage(conversation.id)
            val lastUserMessage = _uiState.value.messages.filter { it.isFromUser }.lastOrNull()
            if (lastUserMessage != null) {
                _userInput.value = lastUserMessage.text
                sendMessage()
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch {
            val streamingMessage = _uiState.value.messages.find { it.isStreaming }
            if (streamingMessage != null) {
                chatRepo.addMessage(
                    streamingMessage.copy(
                        text = streamingMessage.text + "\n\n[Generation stopped by user]",
                        isStreaming = false,
                        isError = true,
                    ).toEntity()
                )
            }

            val serviceIntent = Intent(context, GenerationService::class.java)
            context.stopService(serviceIntent)
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            val newConversationId = chatRepo.createConversation("New Chat")
            setCurrentConversation(newConversationId)
        }
    }

    fun updateConversationTitle(newTitle: String) {
        viewModelScope.launch {
            _uiState.value.currentConversation?.let {
                chatRepo.updateConversation(it.copy(title = newTitle))
            }
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            chatRepo.deleteConversation(conversationId)
        }
    }

    fun updateSystemPromptAndTitle(
        newPrompt: String,
        newTitle: String,
    ) {
        viewModelScope.launch {
            settingsRepo.setSystemPrompt(newPrompt)
            settingsRepo.setRoleTitle(newTitle)
            updateConversationTitle(newTitle)
        }
    }

    fun updateTemperature(newTemperature: Float) {
        viewModelScope.launch {
            settingsRepo.setTemperature(newTemperature)
        }
    }

    fun exportChat(): String {
        val roleTitle = _uiState.value.roleTitle
        return _uiState.value.messages.joinToString("\n\n") { msg ->
            val prefix = if (msg.isFromUser) "[USER]" else "[ASSISTANT] - $roleTitle"
            "$prefix\n${msg.text}"
        }
    }
}
