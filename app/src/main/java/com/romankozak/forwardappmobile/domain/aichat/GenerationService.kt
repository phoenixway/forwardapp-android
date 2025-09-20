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
    lateinit var ollamaService: OllamaService

    @Inject
    lateinit var chatRepo: ChatRepository

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        val assistantMessageId = intent?.getLongExtra("assistantMessageId", -1L) ?: -1L

        if (assistantMessageId == -1L) {
            Log.e(TAG, "Invalid assistantMessageId. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                val ollamaUrl = settingsRepo.ollamaUrlFlow.first()
                val smartModel = settingsRepo.ollamaSmartModelFlow.first()
                val systemPrompt = settingsRepo.systemPromptFlow.first()
                val temperature = settingsRepo.temperatureFlow.first()
                val historyEntities = chatRepo.getChatHistory().first()

                val systemMessage = Message(role = "system", content = systemPrompt)
                val history =
                    listOf(systemMessage) +
                        historyEntities
                            .filter { !it.isError && it.id != assistantMessageId }
                            .map { msg ->
                                Message(
                                    role = if (msg.isFromUser) "user" else "assistant",
                                    content = msg.text
                                )
                            }

                var fullResponse = ""
                ollamaService
                    .generateChatResponseStream(ollamaUrl, smartModel, history, temperature)
                    .collect { chunk ->
                        fullResponse += chunk
                        val currentMessage = chatRepo.getMessageById(assistantMessageId) 
                        currentMessage?.let {
                            chatRepo.updateMessage(it.copy(text = fullResponse, isStreaming = true))
                        }
                    }

                chatRepo.getMessageById(assistantMessageId)?.let {
                    chatRepo.updateMessage(it.copy(text = fullResponse, isStreaming = false))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during streaming generation", e)
                chatRepo.getMessageById(assistantMessageId)?.let {
                    chatRepo.updateMessage(
                        it.copy(
                            text = "Error: ${e.message ?: "Unknown error"}",
                            isError = true,
                            isStreaming = false,
                        ),
                    )
                }
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
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
            .setContentText("Generating response...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
