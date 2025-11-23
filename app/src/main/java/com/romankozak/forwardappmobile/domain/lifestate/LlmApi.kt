package com.romankozak.forwardappmobile.domain.lifestate

import android.util.Log
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.aichat.Message
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import javax.inject.Inject
import javax.inject.Singleton
import java.net.SocketException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

interface LlmApi {
    suspend fun runAnalysis(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float? = null,
    ): Result<String>
}

@Singleton
class OllamaLlmApi @Inject constructor(
    private val ollamaService: OllamaService,
    private val settingsRepository: SettingsRepository,
) : LlmApi {
    private val tag = "AI_CHAT_OLLAMA"

    override suspend fun runAnalysis(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float?,
    ): Result<String> {
        val manualIp = runCatching { settingsRepository.manualServerIpFlow.first() }.getOrNull().orEmpty()
        val port = runCatching { settingsRepository.ollamaPortFlow.first() }.getOrDefault(11434)
        val manualUrl =
            manualIp.takeIf { it.isNotBlank() }?.let { ip ->
                if (ip.startsWith("http")) ip else "http://$ip:$port"
            }
        val autoUrl =
            runCatching {
                // Автопошук може чекати безкінечно, тому обмежуємо до 5с
                kotlinx.coroutines.withTimeout(5_000) { settingsRepository.getOllamaUrl().first() }
            }.getOrElse { error ->
                Log.w(tag, "[LifeState LLM] Auto-discovery timed out: ${error.message}")
                null
        }
        val baseUrl = manualUrl ?: autoUrl
        if (baseUrl.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Ollama URL is not configured"))
        }

        val smartModel = settingsRepository.ollamaSmartModelFlow.first()
        val fastModel = settingsRepository.ollamaFastModelFlow.first()
        val model = smartModel.ifBlank { fastModel }
        if (model.isBlank()) {
            return Result.failure(IllegalStateException("Ollama model is not set"))
        }
        val tempValue = temperature ?: settingsRepository.temperatureFlow.first()
        val messages =
            listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt),
            )

        return try {
            val responseBuilder = StringBuilder()
            Log.d(tag, "[LifeState LLM] call url=$baseUrl model=$model temp=$tempValue")
            withTimeout(110_000) {
                ollamaService
                    .generateChatResponseStream(baseUrl, model, messages, tempValue)
                    .collect { chunk: String -> responseBuilder.append(chunk) }
            }
            Log.d(tag, "[LifeState LLM] response chars=${responseBuilder.length}")
            Result.success(responseBuilder.toString())
        } catch (e: TimeoutCancellationException) {
            Log.e(tag, "[LifeState LLM] Request timed out", e)
            Result.failure(IllegalStateException("LLM request timed out", e))
        } catch (e: SocketException) {
            Log.e(tag, "[LifeState LLM] Connection aborted", e)
            Result.failure(IllegalStateException("LLM interrupted the connection, try again", e))
        } catch (e: Exception) {
            Log.e(tag, "[LifeState LLM] Request failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
