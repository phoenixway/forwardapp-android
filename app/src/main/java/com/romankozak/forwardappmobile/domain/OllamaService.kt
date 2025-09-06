package com.romankozak.forwardappmobile.domain

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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

            Log.d("OllamaServiceChat", "Sending chat request to model $model with ${messages.size} messages: ${messages.joinToString("\n") { "${it.role}: ${it.content}" }}")

            val responseBody = api.generateChat(request)
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

            val json = Json { ignoreUnknownKeys = true }

            try {
                val errorResponse = json.decodeFromString<OllamaErrorResponse>(response)
                return Result.failure(IllegalStateException("Ollama API error: ${errorResponse.error}"))
            } catch (e: Exception) {
                // Not an error response, proceed with parsing
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

    suspend fun generateChatResponseStream(baseUrl: String, model: String, messages: List<Message>): Flow<String> = flow {
        if (baseUrl.isBlank() || model.isBlank()) {
            throw IllegalArgumentException("URL or model is not configured")
        }

        val api = buildRetrofitApi(baseUrl)

        try {
            Log.d(TAG, "Trying /api/chat endpoint for streaming...")
            Log.d(TAG, "Chat request: ${messages.joinToString("\n") { "${it.role}: ${it.content}" }}")
            val chatRequest = OllamaChatRequest(
                model = model,
                messages = messages,
                stream = true
            )

            val responseBody = api.generateChat(chatRequest)
            processStreamingResponse(responseBody, isGenerateEndpoint = false)

        } catch (e: Exception) {
            Log.w(TAG, "/api/chat failed, trying /api/generate: ${e.message}", e)

            val messagesText = messages.joinToString("\n") { "${it.role}: ${it.content}" }
            val prompt = "Continue this conversation:\n$messagesText\nassistant:"

            Log.d(TAG, "Generate request prompt: $prompt")
            val generateRequest = OllamaCompletionRequest(
                model = model,
                prompt = prompt,
                stream = true
            )

            val responseBody = api.generateCompletionStream(generateRequest)
            processStreamingResponse(responseBody, isGenerateEndpoint = true)
        }
    }.catch { e ->
        Log.e(TAG, "Error during streaming: ${e.message}", e)
        throw e // Rethrow to be handled by the ViewModel
    }.flowOn(Dispatchers.IO) // Ensure emissions occur on Dispatchers.IO

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.processStreamingResponse(
        responseBody: ResponseBody,
        isGenerateEndpoint: Boolean
    ) {
        val json = Json { ignoreUnknownKeys = true }

        responseBody.byteStream().bufferedReader().use { reader ->
            var lineCount = 0
            var shouldContinue = true

            while (shouldContinue) {
                val line = reader.readLine() ?: break
                lineCount++
                Log.d(TAG, "Stream line #$lineCount: '$line'")

                if (line.isBlank()) continue

                // Parse the line as JSON
                val jsonElement = try {
                    json.parseToJsonElement(line)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse line as JSON: '$line', error: ${e.message}")
                    continue
                }

                // Check for error response first
                try {
                    val errorResponse = json.decodeFromString<OllamaErrorResponse>(line)
                    throw IllegalStateException("Ollama API error: ${errorResponse.error}")
                } catch (e: Exception) {
                    // Not an error response, proceed with parsing
                }

                if (isGenerateEndpoint) {
                    // Parse as OllamaResponse for /api/generate
                    val ollamaResponse = try {
                        json.decodeFromString<OllamaResponse>(line)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse line as OllamaResponse: '$line', error: ${e.message}")
                        continue
                    }

                    val content = ollamaResponse.response
                    if (content.isNotEmpty()) {
                        Log.d(TAG, "Emitting chunk: '$content'")
                        emit(content)
                    } else {
                        Log.d(TAG, "Skipping empty content for /api/generate: '$content'")
                    }

                    if (ollamaResponse.done) {
                        Log.d(TAG, "Stream completed (done=true)")
                        shouldContinue = false
                    }
                } else {
                    // Parse as OllamaChatResponse for /api/chat
                    val ollamaResponse = try {
                        json.decodeFromString<OllamaChatResponse>(line)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse line as OllamaChatResponse: '$line', error: ${e.message}")
                        continue
                    }

                    val content = ollamaResponse.message.content
                    if (content.isNotEmpty()) {
                        Log.d(TAG, "Emitting chunk: '$content'")
                        emit(content)
                    } else {
                        Log.d(TAG, "Skipping empty content for /api/chat: '$content'")
                    }

                    // Check for "done" flag
                    val done = jsonElement.jsonObject["done"]?.jsonPrimitive?.boolean ?: false
                    if (done) {
                        Log.d(TAG, "Stream completed (done=true)")
                        shouldContinue = false
                    }
                }
            }

            Log.d(TAG, "Stream reading completed. Total lines processed: $lineCount")
        }
    }
}