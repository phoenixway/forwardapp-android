package com.romankozak.forwardappmobile.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

@Serializable
data class Message(
    val role: String,
    val content: String,
)

@Serializable
data class OllamaResponse(
    val response: String,
    val done: Boolean,
    @SerialName("done_reason") val doneReason: String? = null,
)

@Serializable
data class ModelDetails(
    val name: String,
)

@Serializable
data class OllamaTagsResponse(
    val models: List<ModelDetails>,
)

@Serializable
data class OllamaChatResponse(
    val message: Message,
)

@Serializable
data class OllamaOptions(
    @SerialName("num_predict") val numPredict: Int? = null,
    @SerialName("temperature") val temperature: Float? = null, 
)

@Serializable
data class OllamaCompletionRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val options: OllamaOptions? = null,
)

@Serializable
data class OllamaErrorResponse(
    val error: String,
)

@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val options: OllamaOptions? = null, 
)

interface OllamaApi {
    @POST("/api/generate")
    suspend fun generateCompletion(
        @Body request: OllamaCompletionRequest,
    ): OllamaResponse

    @POST("/api/generate")
    suspend fun generateCompletionRaw(
        @Body request: OllamaCompletionRequest,
    ): ResponseBody

    @GET("/api/tags")
    suspend fun getTags(): OllamaTagsResponse

    @Streaming
    @POST("/api/chat")
    suspend fun generateChat(
        @Body request: OllamaChatRequest,
    ): ResponseBody

    @Streaming
    @POST("/api/generate")
    suspend fun generateCompletionStream(
        @Body request: OllamaCompletionRequest,
    ): ResponseBody
}
