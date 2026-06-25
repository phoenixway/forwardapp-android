package com.romankozak.forwardappmobile.domain.aichat

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val TAG = "AI_CHAT_DEBUG"
const val DEFAULT_OLLAMA_NUM_PREDICT = -1
const val DEFAULT_OLLAMA_NUM_CTX = 8192

data class OllamaRuntimeOptions(
    val numCtx: Int = DEFAULT_OLLAMA_NUM_CTX,
    val numPredict: Int = DEFAULT_OLLAMA_NUM_PREDICT,
    val numBatch: Int = 0,
    val numGpu: Int = -1,
    val numThread: Int = 0,
)

private data class ParsedStreamLine(
    val content: String,
    val done: Boolean,
    val doneReason: String? = null,
)

@Singleton
class OllamaService
    @Inject
    constructor() {
        private fun buildRetrofitApi(baseUrl: String): OllamaApi {
            val okHttpClient =
                OkHttpClient
                    .Builder()
                    .readTimeout(5, TimeUnit.MINUTES)
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .build()

            val json = Json { ignoreUnknownKeys = true }

            return Retrofit
                .Builder()
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

        suspend fun streamChatResponses(
            baseUrl: String,
            model: String,
            messages: List<Message>,
            temperature: Float,
            runtimeOptions: OllamaRuntimeOptions = OllamaRuntimeOptions(),
        ): Flow<String> =
            flow {
                if (baseUrl.isBlank() || model.isBlank()) {
                    throw IllegalArgumentException("URL or model is not configured")
                }

                val api = buildRetrofitApi(baseUrl)
                val requestOptions =
                    OllamaOptions(
                        numPredict = runtimeOptions.numPredict,
                        numCtx = runtimeOptions.numCtx.takeIf { it > 0 },
                        numBatch = runtimeOptions.numBatch.takeIf { it > 0 },
                        numGpu = runtimeOptions.numGpu.takeIf { it >= 0 },
                        numThread = runtimeOptions.numThread.takeIf { it > 0 },
                        temperature = temperature,
                    )

                try {
                    Log.d(TAG, "Trying /api/chat endpoint for streaming with temperature: $temperature...")
                    Log.d(TAG, "Chat request messages: ${messages.joinToString("\n") { "${it.role}: ${it.content}" }}")
                    val chatRequest =
                        OllamaChatRequest(
                            model = model,
                            messages = messages,
                            stream = true,
                            options = requestOptions,
                        )

                    val responseBody = api.generateChat(chatRequest)
                    processStreamingResponse(responseBody, isGenerateEndpoint = false)
                } catch (e: Exception) {
                    if (e is HttpException && e.code() == 404) {
                        Log.w(TAG, "/api/chat failed (404), trying /api/generate as fallback: ${e.message}", e)

                        val messagesText = messages.joinToString("\n") { "${it.role}: ${it.content}" }
                        val prompt = "Respond to the user's query with information relevant to the conversation:\n$messagesText\nassistant:"

                        Log.d(TAG, "Generate request prompt: $prompt")
                        val generateRequest =
                            OllamaCompletionRequest(
                                model = model,
                                prompt = prompt,
                                stream = true,
                                options = requestOptions,
                            )

                        val responseBody = api.generateCompletionStream(generateRequest)
                        processStreamingResponse(responseBody, isGenerateEndpoint = true)
                    } else {
                        throw e
                    }
                }
            }.catch { e ->
                Log.e(TAG, "Error during streaming: ${e.message}", e)
                throw e
            }.flowOn(Dispatchers.IO)

        suspend fun generateChatResponseStream(
            baseUrl: String,
            model: String,
            messages: List<Message>,
            temperature: Float,
            runtimeOptions: OllamaRuntimeOptions = OllamaRuntimeOptions(),
        ): Flow<String> = streamChatResponses(baseUrl, model, messages, temperature, runtimeOptions)

        private suspend fun FlowCollector<String>.processStreamingResponse(
            responseBody: ResponseBody,
            isGenerateEndpoint: Boolean,
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

                    val parsedLine =
                        parseStreamingLine(
                            json = json,
                            line = line,
                            isGenerateEndpoint = isGenerateEndpoint,
                        ) ?: continue
                    if (parsedLine.content.isNotEmpty()) emit(parsedLine.content)
                    if (parsedLine.done) {
                        Log.d(TAG, "Ollama stream done. reason=${parsedLine.doneReason ?: "unknown"}")
                        contextExhaustionNotice(parsedLine.doneReason)?.let { emit(it) }
                    }
                    shouldContinue = !parsedLine.done
                }
                Log.d(TAG, "Stream reading completed. Total lines processed: $lineCount")
            }
        }

        private fun parseStreamingLine(
            json: Json,
            line: String,
            isGenerateEndpoint: Boolean,
        ): ParsedStreamLine? {
            val jsonElement =
                parseStreamJsonElement(json, line) ?: return null
            throwIfErrorResponse(json, line)
            return if (isGenerateEndpoint) {
                parseGenerateStreamLine(json, line)
            } else {
                parseChatStreamLine(json, line, jsonElement.jsonObject["done"]?.jsonPrimitive?.boolean ?: false)
            }
        }

        private fun parseStreamJsonElement(
            json: Json,
            line: String,
        ) = try {
            json.parseToJsonElement(line)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse line as JSON: '$line', error: ${e.message}")
            null
        }

        private fun throwIfErrorResponse(
            json: Json,
            line: String,
        ) {
            try {
                val errorResponse = json.decodeFromString<OllamaErrorResponse>(line)
                throw IllegalStateException("Ollama API error: ${errorResponse.error}")
            } catch (_: Exception) {
            }
        }

        private fun parseGenerateStreamLine(
            json: Json,
            line: String,
        ): ParsedStreamLine? {
            val ollamaResponse =
                try {
                    json.decodeFromString<OllamaResponse>(line)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse line as OllamaResponse: '$line', error: ${e.message}")
                    return null
                }
            return ParsedStreamLine(
                content = ollamaResponse.response,
                done = ollamaResponse.done,
                doneReason = ollamaResponse.doneReason,
            )
        }

        private fun parseChatStreamLine(
            json: Json,
            line: String,
            done: Boolean,
        ): ParsedStreamLine? {
            val ollamaResponse =
                try {
                    json.decodeFromString<OllamaChatResponse>(line)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse line as OllamaChatResponse: '$line', error: ${e.message}")
                    return null
                }
            return ParsedStreamLine(
                content = ollamaResponse.message.content,
                done = ollamaResponse.done || done,
                doneReason = ollamaResponse.doneReason,
            )
        }

        private fun contextExhaustionNotice(doneReason: String?): String? {
            val normalizedReason = doneReason?.trim()?.lowercase().orEmpty()
            val isContextOrLengthLimit =
                normalizedReason == "length" ||
                    "context" in normalizedReason ||
                    "limit" in normalizedReason
            if (!isContextOrLengthLimit) return null
            return "\n\n[Генерацію зупинено: Ollama вичерпала контекстне вікно або ліміт довжини відповіді.]"
        }
    }
