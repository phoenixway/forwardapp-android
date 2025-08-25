package com.romankozak.forwardappmobile.domain

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.romankozak.forwardappmobile.data.OllamaApi
import com.romankozak.forwardappmobile.data.OllamaCompletionRequest
import com.romankozak.forwardappmobile.data.OllamaErrorResponse
import com.romankozak.forwardappmobile.data.OllamaOptions
import com.romankozak.forwardappmobile.data.OllamaResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType


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
}