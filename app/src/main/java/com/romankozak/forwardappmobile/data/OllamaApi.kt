// File: app/src/main/java/com/romankozak/forwardappmobile/data/OllamaApi.kt

package com.romankozak.forwardappmobile.data // ✨ ВИПРАВЛЕНО: Назва пакета

import kotlinx.serialization.SerialName // ✨ ДОДАНО: Імпорт для анотації
import kotlinx.serialization.Serializable // ✨ ДОДАНО: Правильний імпорт
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// --- Універсальні моделі ---
@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class OllamaResponse(
    val response: String,
    val done: Boolean,
    @SerialName("done_reason") val doneReason: String? = null
)

@Serializable
data class ModelDetails(val name: String)

@Serializable
data class OllamaTagsResponse(val models: List<ModelDetails>)

@Serializable
data class OllamaChatResponse(val message: Message)

// --- Модель для опцій генерації ---
@Serializable
data class OllamaOptions(
    @SerialName("num_predict") val numPredict: Int // ✨ ВИПРАВЛЕНО: Стиль і анотація
)

// --- Моделі запитів ---
@Serializable
data class OllamaCompletionRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaErrorResponse(
    val error: String
)


@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false
)

interface OllamaApi {
    @POST("/api/generate")

    suspend fun generateCompletion(@Body request: OllamaCompletionRequest): OllamaResponse

    @POST("/api/chat")
    suspend fun generateChat(@Body request: OllamaChatRequest): OllamaChatResponse

    @POST("/api/generate")
    suspend fun testGenerateAsChat(@Body request: OllamaChatRequest): OllamaResponse

    @POST("/api/generate")
    suspend fun generateCompletionRaw(@Body request: OllamaCompletionRequest): ResponseBody


    @GET("/api/tags")
    suspend fun getTags(): OllamaTagsResponse
}