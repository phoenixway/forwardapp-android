package com.romankozak.forwardappmobile.domain

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

private val TAG = "AI_CHAT_DEBUG"

@Singleton
class OllamaService @Inject constructor() {

    private fun buildRetrofitApi(baseUrl: String): OllamaApi {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(1, TimeUnit.MINUTES)
            .build()

        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OllamaApi::class.java)
    }

    suspend fun getAvailableModels(baseUrl: String): Result<List<String>> {
        if (baseUrl.isBlank()) return Result.failure(IllegalArgumentException("Base URL is empty"))
        return try {
            val api = buildRetrofitApi(baseUrl)
            val response = api.getTags()
            Result.success(response.models.map { it.name })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateChatResponse(baseUrl: String, model: String, messages: List<Message>): Result<String> {
        if (baseUrl.isBlank() || model.isBlank()) {
            return Result.failure(IllegalArgumentException("URL or model is not configured"))
        }

        return try {
            val api = buildRetrofitApi(baseUrl)
            val request = OllamaChatRequest(
                model = model,
                messages = messages,
                stream = false
            )

            Log.d("OllamaServiceChat", "Sending chat request to model $model with ${messages.size} messages.")

            // Отримуємо необроблений ResponseBody
            val responseBody = api.generateChat(request)

            // Парсимо JSON-об'єкти рядок за рядком
            val json = Json { ignoreUnknownKeys = true }

            val fullResponseContent = responseBody.string()
                .lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<OllamaChatResponse>(line).message.content
                    } catch (e: Exception) {
                        Log.e("OllamaServiceChat", "Failed to parse line: $line", e)
                        null
                    }
                }
                .joinToString("")

            if (fullResponseContent.isBlank()) {
                val errorResponse = json.decodeFromString<OllamaErrorResponse>(responseBody.string())
                return Result.failure(IllegalStateException("Ollama API error: ${errorResponse.error}"))
            }

            Log.d("OllamaServiceChat", "Received response: '$fullResponseContent'")

            Result.success(fullResponseContent.trim())
        } catch (e: Exception) {
            Log.e("OllamaServiceChat", "Error generating chat response: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateTitle(baseUrl: String, model: String, fullText: String): Result<String> {
        if (baseUrl.isBlank() || model.isBlank()) {
            return Result.failure(IllegalArgumentException("URL or model is not configured"))
        }

        // Clean text from unwanted characters
        val cleanText = fullText.trim()
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\"", "'")
            .replace("\t", " ")

        val prompt = "Generate a very short, concise title (3-7 words, max 80 characters) for the following text. " +
                "Respond with ONLY the title itself and absolutely nothing else. " +
                "TEXT: \"$cleanText\" " +
                "TITLE:"

        return try {
            val api = buildRetrofitApi(baseUrl)

            // Use data class instead of manual JSON creation
            val request = OllamaCompletionRequest(
                model = model,
                prompt = prompt,
                stream = false,
                options = OllamaOptions(numPredict = 20),
            )

            Log.d("OllamaService", "Sending request: model=$model, prompt length=${prompt.length}")

            val responseBody = api.generateCompletionRaw(request)
            val response = responseBody.string()

            Log.d("OllamaService", "Raw response: $response")

            // Parse JSON objects line by line
            val json = Json { ignoreUnknownKeys = true }

            // Check if response contains an error
            try {
                val errorResponse = json.decodeFromString<OllamaErrorResponse>(response)
                return Result.failure(IllegalStateException("Ollama API error: ${errorResponse.error}"))
            } catch (e: Exception) {
                // If failed to parse as error, continue processing as successful response
            }

            val responses = response.lines()
                .filter { it.trim().isNotEmpty() }
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<OllamaResponse>(line)
                    } catch (e: Exception) {
                        Log.e("OllamaService", "Failed to parse line: $line", e)
                        null
                    }
                }

            Log.d("OllamaService", "Parsed ${responses.size} responses")
            responses.forEach { resp ->
                Log.d("OllamaService", "Response part: '${resp.response}', done: ${resp.done}")
            }

            // Збираємо повний текст з усіх частин відповіді
            val fullResponse = responses.joinToString("") { it.response }
            Log.d("OllamaService", "Full combined response: '$fullResponse'")

            val cleanResponse = fullResponse
                .trim()
                .removeSurrounding("\"")
                .take(80)

            Log.d("OllamaService", "Final clean response: '$cleanResponse'")

            if (cleanResponse.isBlank()) {
                Log.w("OllamaService", "Generated title is empty!")
                return Result.failure(IllegalStateException("Generated title is empty"))
            }

            Result.success(cleanResponse)
        } catch (e: Exception) {
            Log.e("OllamaService", "Error generating title: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateChatResponseStream(baseUrl: String, model: String, messages: List<Message>): kotlinx.coroutines.flow.Flow<String> = flow {
        if (baseUrl.isBlank() || model.isBlank()) {
            throw IllegalArgumentException("URL or model is not configured")
        }

        try {
            val api = buildRetrofitApi(baseUrl)

            // Спочатку спробуємо через /api/chat
            try {
                Log.d(TAG, "Trying /api/chat endpoint for streaming...")
                val chatRequest = OllamaChatRequest(
                    model = model,
                    messages = messages,
                    stream = true
                )

                Log.d(TAG, "Chat request: $chatRequest")
                val responseBody = api.generateChat(chatRequest)

                processStreamingResponse(responseBody, isGenerateEndpoint = false)

            } catch (e: Exception) {
                Log.w(TAG, "/api/chat failed, trying /api/generate: ${e.message}")

                // Якщо /api/chat не працює, спробуємо /api/generate
                val messagesText = messages.joinToString("\n") { "${it.role}: ${it.content}" }
                val prompt = "Continue this conversation:\n$messagesText\nassistant:"

                val generateRequest = OllamaCompletionRequest(
                    model = model,
                    prompt = prompt,
                    stream = true
                )

                Log.d(TAG, "Generate request: $generateRequest")
                val responseBody = api.generateCompletionStream(generateRequest)

                processStreamingResponse(responseBody, isGenerateEndpoint = true)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during streaming: ${e.message}", e)
            throw e
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.processStreamingResponse(
        responseBody: ResponseBody,
        isGenerateEndpoint: Boolean
    ) {
        val json = Json { ignoreUnknownKeys = true }

        // КРИТИЧНО ВАЖЛИВО: Читаємо стрім рядок за рядком в реальному часі
        withContext(Dispatchers.IO) {
            responseBody.byteStream().bufferedReader().use { reader ->
                var lineCount = 0
                var shouldContinue = true

                while (shouldContinue) {
                    val line = reader.readLine()
                    if (line == null) {
                        break
                    }

                    lineCount++
                    Log.d(TAG, "Stream line #$lineCount: '$line'")

                    if (line.isNotBlank()) {
                        try {
                            if (isGenerateEndpoint) {
                                // Парсимо як OllamaResponse для /api/generate
                                val ollamaResponse = json.decodeFromString<OllamaResponse>(line)
                                val content = ollamaResponse.response

                                Log.d(TAG, "Generate endpoint - parsed content: '$content'")

                                if (content.isNotEmpty()) {
                                    Log.d(TAG, "Emitting chunk: '$content'")
                                    emit(content)
                                }

                                if (ollamaResponse.done) {
                                    Log.d(TAG, "Stream completed (done=true)")
                                    shouldContinue = false
                                }
                            } else {
                                // Парсимо як OllamaChatResponse для /api/chat
                                val ollamaResponse = json.decodeFromString<OllamaChatResponse>(line)
                                val content = ollamaResponse.message.content

                                Log.d(TAG, "Chat endpoint - parsed content: '$content'")

                                if (content.isNotEmpty()) {
                                    Log.d(TAG, "Emitting chunk: '$content'")
                                    emit(content)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse streaming line: '$line'", e)

                            // Спробуємо як помилку
                            try {
                                val errorResponse = json.decodeFromString<OllamaErrorResponse>(line)
                                throw IllegalStateException("Ollama API error: ${errorResponse.error}")
                            } catch (e2: Exception) {
                                Log.w(TAG, "Ignoring unparseable line: '$line'")
                            }
                        }
                    }
                }

                Log.d(TAG, "Stream reading completed. Total lines processed: $lineCount")
            }
        }
    }
}