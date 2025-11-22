package com.romankozak.forwardappmobile.domain.aichat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.repository.ChatRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.aichat.Message
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import kotlinx.coroutines.flow.firstOrNull
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "GenerationService"
private const val NOTIFICATION_ID = 11434
private const val NOTIFICATION_CHANNEL_ID = "generation_channel"

@AndroidEntryPoint
class GenerationService : Service() {


    @Inject
    lateinit var chatRepo: ChatRepository

    @Inject
    lateinit var settingsRepo: SettingsRepository

    @Inject
    lateinit var ollamaService: OllamaService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val assistantMessageId = intent?.getLongExtra(EXTRA_ASSISTANT_MESSAGE_ID, -1L) ?: -1L
        val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, -1L) ?: -1L
        val systemPrompt = intent?.getStringExtra(EXTRA_SYSTEM_PROMPT)

        startForeground(NOTIFICATION_ID, createNotification(contentText = "Generating response…"))

        if (assistantMessageId == -1L || conversationId == -1L || systemPrompt.isNullOrBlank()) {
            Log.e(TAG, "Invalid assistantMessageId or conversationId. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                val url = resolveBaseUrl()
                val model = resolveModel()
                val temperature = settingsRepo.temperatureFlow.first()
                if (url.isNullOrBlank() || model.isNullOrBlank()) {
                    Log.e(TAG, "Server or model is not configured. url=$url model=$model")
                    markAssistantError(
                        conversationId = conversationId,
                        assistantMessageId = assistantMessageId,
                        message = "Error: Ollama server or model is not configured.",
                    )
                    stopSelf()
                    return@launch
                }

                val historyEntities = chatRepo.getChatHistory(conversationId).first()
                val historyMessages =
                    historyEntities
                        .filter { !it.isError && it.id != assistantMessageId }
                        .map { msg ->
                            Message(
                                role = if (msg.isFromUser) "user" else "assistant",
                                content = msg.text,
                            )
                        }
                val messages = listOf(Message(role = "system", content = systemPrompt)) + historyMessages

                Log.d(TAG, "--- Ollama Request ---")
                Log.d(TAG, "URL: $url")
                Log.d(TAG, "Model: $model")
                Log.d(TAG, "Temperature: $temperature")
                Log.d(TAG, "History Size: ${messages.size}")

                val responseBuilder = StringBuilder()
                ollamaService
                    .generateChatResponseStream(url, model, messages, temperature)
                    .collect { chunk ->
                        responseBuilder.append(chunk)
                        chatRepo.updateMessageContent(
                            messageId = assistantMessageId,
                            text = responseBuilder.toString(),
                            isStreaming = true,
                        )
                    }

                chatRepo.updateMessageContent(
                    messageId = assistantMessageId,
                    text = responseBuilder.toString(),
                    isStreaming = false,
                )

                notifyReady()
            } catch (e: Exception) {
                Log.e(TAG, "Error during streaming generation", e)
                chatRepo.updateMessageContent(
                    messageId = assistantMessageId,
                    text = "Error: ${e.message ?: "Unknown error"}",
                    isStreaming = false,
                    isError = true,
                )
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun notifyReady() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NOTIFICATION_ID,
            createNotification(contentText = "AI response is ready"),
        )
    }

    private fun createNotification(contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "AI Generation",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AI Assistant")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private suspend fun resolveBaseUrl(): String? {
        return settingsRepo.getOllamaUrl().firstOrNull()
    }

    private suspend fun resolveModel(): String? {
        val smart = settingsRepo.ollamaSmartModelFlow.first()
        val fast = settingsRepo.ollamaFastModelFlow.first()
        return smart.ifBlank { fast }
    }

    private suspend fun markAssistantError(
        conversationId: Long,
        assistantMessageId: Long,
        message: String,
    ) {
        val targetMessage = chatRepo.getChatHistory(conversationId).first().find { it.id == assistantMessageId }
        targetMessage?.let {
            chatRepo.updateMessageContent(
                messageId = assistantMessageId,
                text = message,
                isStreaming = false,
                isError = true,
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_ASSISTANT_MESSAGE_ID = "assistantMessageId"
        const val EXTRA_CONVERSATION_ID = "conversationId"
        const val EXTRA_SYSTEM_PROMPT = "systemPrompt"
    }
}
