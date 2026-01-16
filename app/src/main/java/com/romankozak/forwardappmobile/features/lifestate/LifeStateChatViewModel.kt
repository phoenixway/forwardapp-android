package com.romankozak.forwardappmobile.features.lifestate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ChatRepository
import com.romankozak.forwardappmobile.domain.aichat.GenerationService
import com.romankozak.forwardappmobile.domain.lifestate.model.AiAnalysis
import com.romankozak.forwardappmobile.features.ai.chat.ChatMessage
import com.romankozak.forwardappmobile.features.ai.chat.toChatMessage
import com.romankozak.forwardappmobile.features.ai.chat.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LifeStateChatUiState(
    val conversationId: Long? = null,
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LifeStateChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val tag = "LifeStateChat"
    private val lifeChatTitle = "Life Management"

    private val _uiState = MutableStateFlow(LifeStateChatUiState())
    val uiState: StateFlow<LifeStateChatUiState> = _uiState.asStateFlow()

    fun attachContext(analysis: AiAnalysis) {
        if (_uiState.value.conversationId != null) return
        viewModelScope.launch {
            val existing = chatRepo.getConversationByTitle(lifeChatTitle)
            val conversationId = existing?.id ?: chatRepo.createConversation(lifeChatTitle)
            _uiState.update { it.copy(conversationId = conversationId) }

            val hasHistory = chatRepo.getChatHistory(conversationId).first().isNotEmpty()
            if (!hasHistory) {
                val introText =
                    buildString {
                        appendLine("Current state (source language may vary):")
                        appendLine(analysis.summary)
                        if (analysis.recommendations.isNotEmpty()) {
                            appendLine()
                            appendLine("Recommendations (24-48h):")
                            analysis.recommendations.take(3).forEach { rec ->
                                appendLine("- ${rec.title}: ${rec.message}")
                            }
                        }
                    }
                chatRepo.addMessage(
                    ChatMessage(
                        conversationId = conversationId,
                        text = introText,
                        isFromUser = false,
                    ).toEntity(),
                )
            }

            chatRepo.getChatHistory(conversationId).collectLatest { history ->
                val mapped = history.map { it.toChatMessage(conversationId) }
                _uiState.update { state -> state.copy(messages = mapped) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(userInput = text) }
    }

    fun sendMessage(contextAnalysis: AiAnalysis, promptOverride: String? = null) {
        val conversationId = _uiState.value.conversationId ?: return
        val text =
            promptOverride?.takeIf { it.isNotBlank() }
                ?: _uiState.value.userInput.takeIf { it.isNotBlank() }
                ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            try {
                val userMsg =
                    ChatMessage(
                        conversationId = conversationId,
                        text = text,
                        isFromUser = true,
                    )
                chatRepo.addMessage(userMsg.toEntity())
                _uiState.update { it.copy(userInput = "") }

            } catch (e: Exception) {
                Log.e(tag, "Chat send failed: ${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
            viewModelScope.launch {
                val assistantDraftId =
                    chatRepo
                        .addMessage(
                            ChatMessage(
                                conversationId = conversationId,
                                text = "",
                                isFromUser = false,
                                isStreaming = true,
                            ).toEntity(),
                        )
                val systemPrompt = buildLifeCoachSystemPrompt(contextAnalysis)
                startBackgroundGeneration(conversationId, assistantDraftId, systemPrompt)
            }
        }
    }

    fun regenerate(contextAnalysis: AiAnalysis) {
        val conversationId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            chatRepo.deleteLastAssistantMessage(conversationId)
            val history = chatRepo.getChatHistory(conversationId).first()
            val lastUserPrompt = history.lastOrNull { it.isFromUser }?.text
            if (lastUserPrompt.isNullOrBlank()) return@launch
            sendMessage(contextAnalysis, promptOverride = lastUserPrompt)
        }
    }

    fun regenerateFromMessage(message: ChatMessage, contextAnalysis: AiAnalysis) {
        if (_uiState.value.isSending) return
        if (!message.isFromUser) return
        val conversationId = _uiState.value.conversationId ?: return
        if (conversationId != message.conversationId) return
        sendMessage(contextAnalysis, promptOverride = message.text)
    }

    fun sendQuickPrompt(prompt: String, contextAnalysis: AiAnalysis) {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(userInput = prompt) }
        sendMessage(contextAnalysis, promptOverride = prompt)
    }

    private fun buildLifeCoachSystemPrompt(contextAnalysis: AiAnalysis): String =
        buildString {
            appendLine("You are an AI life-coach for ForwardApp. Use the context below, be concise and action-oriented.")
            appendLine("Always respond in English only, even if the user writes in another language. Do not switch languages.")
            appendLine("If context or user input is not English, first interpret it and then answer strictly in English.")
            appendLine("=== Current analysis ===")
            appendLine(contextAnalysis.summary)
            if (contextAnalysis.recommendations.isNotEmpty()) {
                appendLine("Recommendations for the next 24-48h:")
                contextAnalysis.recommendations.take(5).forEach { rec ->
                    appendLine("- ${rec.title}: ${rec.message}")
                }
            }
            appendLine("IMPORTANT: Reply only in English. Keep responses concise and action-oriented.")
        }

    private fun startBackgroundGeneration(
        conversationId: Long,
        assistantMessageId: Long,
        systemPrompt: String,
    ) {
        val intent =
            Intent(context, GenerationService::class.java).apply {
                putExtra(GenerationService.EXTRA_CONVERSATION_ID, conversationId)
                putExtra(GenerationService.EXTRA_ASSISTANT_MESSAGE_ID, assistantMessageId)
                putExtra(GenerationService.EXTRA_SYSTEM_PROMPT, systemPrompt)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
