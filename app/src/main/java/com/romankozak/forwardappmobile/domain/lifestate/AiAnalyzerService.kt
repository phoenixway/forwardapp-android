package com.romankozak.forwardappmobile.domain.lifestate

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedProjectKeys
import com.romankozak.forwardappmobile.data.database.models.ReservedSystemAppKeys
import com.romankozak.forwardappmobile.data.repository.ActivityRecordRepository
import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
import com.romankozak.forwardappmobile.domain.lifestate.model.AiAnalysis
import com.romankozak.forwardappmobile.domain.lifestate.model.AiOpportunity
import com.romankozak.forwardappmobile.domain.lifestate.model.AiRecommendation
import com.romankozak.forwardappmobile.domain.lifestate.model.AiRisk
import com.romankozak.forwardappmobile.domain.lifestate.model.AiSignals
import com.romankozak.forwardappmobile.domain.lifestate.model.LifeStatePromptPayload
import com.romankozak.forwardappmobile.domain.lifestate.model.LifeStateTrackerEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@Singleton
class AiAnalyzerService @Inject constructor(
    private val trackerRepository: ActivityRecordRepository,
    private val systemAppRepository: SystemAppRepository,
    private val llmApi: LlmApi,
) {
    private val tag = "AI_LIFE_STATE"

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            prettyPrint = true
            // Trailing commas are not supported in this Json config
        }

    suspend fun analyzeLifeState(): Result<AiAnalysis> {
        val trackerEntries =
            trackerRepository
                .getRecentRecords(daysBack = 3)
                .take(100) // захист від надто великих запитів
                .map { it.toLifeStateTrackerEntry() }
        val systemNoteText = ensureSystemNoteText()

        val payload =
            LifeStatePromptPayload(
                trackerEntries = trackerEntries,
                systemAppNoteText = systemNoteText,
            )

        val userPrompt = json.encodeToString(payload)
        Log.d(tag, "Sending life-state request. entries=${trackerEntries.size}, noteChars=${systemNoteText.length}")
        val rawResponse =
            try {
                // RTX 4060 8GB може тягнути 8B+, тож збільшуємо таймаут до 120с
                withTimeout(120_000) { llmApi.runAnalysis(LIFE_STATE_SYSTEM_PROMPT, userPrompt) }
            } catch (e: TimeoutCancellationException) {
                Log.e(tag, "LLM request exceeded 120s timeout", e)
                return Result.failure(IllegalStateException("LLM request exceeded timeout", e))
            }

        return rawResponse.mapCatching { response ->
            Log.d(tag, "LLM response received, chars=${response.length}")
            Log.d(tag, "LLM response preview=${response.take(500)}")
            parseAnalysis(response)
        }.onFailure { error ->
            Log.e(tag, "Life-state analysis failed: ${error.message}", error)
        }
    }

    private suspend fun ensureSystemNoteText(): String {
        val existingNote = systemAppRepository.getSystemNote(ReservedSystemAppKeys.MY_LIFE_CURRENT_STATE)
        if (existingNote != null) {
            return existingNote.content ?: ""
        }

        val systemApp =
            systemAppRepository.ensureNoteApp(
                systemKey = ReservedSystemAppKeys.MY_LIFE_CURRENT_STATE,
                projectSystemKey = ReservedProjectKeys.STRATEGIC,
                documentName = "my-life-current-state",
            )
        val note = systemApp.noteDocumentId?.let { systemAppRepository.getSystemNote(ReservedSystemAppKeys.MY_LIFE_CURRENT_STATE) }
        return note?.content ?: ""
    }

    private fun parseAnalysis(response: String): AiAnalysis {
        val cleaned = normalizeResponse(response)
        try {
            return json.decodeFromString(cleaned)
        } catch (e: SerializationException) {
            // Фолбек: розбираємо вручну, допускаючи спрощені масиви рядків
            return parseLenientAnalysis(cleaned, e)
        }
    }

    private fun normalizeResponse(raw: String): String {
        // 1) прибираємо markdown fences і будь-який текст до першої '{' та після останньої '}'
        val trimmed =
            raw
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

        val balanced = extractBalancedObject(trimmed)
        val extracted = balanced ?: run {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
        }

        // 2) вирізаємо можливі розриви після JSON (часто LLM додає пояснення після масиву)
        val closingIndex = extracted.lastIndexOf("}")
        val clipped = if (closingIndex >= 0) extracted.substring(0, closingIndex + 1) else extracted
        // 3) додаємо лапки до неекранованих ключів виду `{key: value}` → `{"key": value}`
        val keyRegex = Regex("([\\{,]\\s*)([A-Za-z0-9_]+)\\s*:")
        val quotedKeys = keyRegex.replace(clipped) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val key = matchResult.groupValues[2]
            "$prefix\"$key\":"
        }
        // 4) прибираємо BOM та контрол-символи
        val sanitized = quotedKeys.filter { it >= ' ' }
        return sanitized
    }

    private fun extractBalancedObject(text: String): String? {
        var inString = false
        var escape = false
        var depth = 0
        var startIndex = -1
        var endIndex = -1
        text.forEachIndexed { idx, ch ->
            if (escape) {
                escape = false
            } else if (ch == '\\') {
                escape = true
            } else if (ch == '"') {
                inString = !inString
            } else if (!inString) {
                if (ch == '{') {
                    if (depth == 0) startIndex = idx
                    depth++
                } else if (ch == '}') {
                    depth--
                    if (depth == 0 && startIndex >= 0) {
                        endIndex = idx
                        return@forEachIndexed
                    }
                }
            }
        }
        return if (startIndex >= 0 && endIndex > startIndex) text.substring(startIndex, endIndex + 1) else null
    }

    private fun parseLenientAnalysis(cleaned: String, cause: SerializationException): AiAnalysis {
        return try {
            val root = json.parseToJsonElement(cleaned).jsonObject
            AiAnalysis(
                summary = root["summary"]?.jsonPrimitive?.contentOrNull ?: "",
                keyProcesses = root["key_processes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                signals =
                    AiSignals(
                        positive =
                            root["signals"]?.jsonObject
                                ?.get("positive")
                                ?.jsonArray
                                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                                ?: emptyList(),
                        negative =
                            root["signals"]?.jsonObject
                                ?.get("negative")
                                ?.jsonArray
                                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                                ?: emptyList(),
                    ),
                risks =
                    root["risks"]?.jsonArray
                        ?.mapNotNull { element: JsonElement ->
                            when {
                                element is JsonObject ->
                                    runCatching { json.decodeFromJsonElement<AiRisk>(element) }.getOrNull()
                                element.jsonPrimitive.contentOrNull != null ->
                                    AiRisk(name = element.jsonPrimitive.content, description = "")
                                else -> null
                            }
                        }
                        ?: emptyList(),
                opportunities =
                    root["opportunities"]?.jsonArray
                        ?.mapNotNull { element: JsonElement ->
                            when {
                                element is JsonObject ->
                                    runCatching { json.decodeFromJsonElement<AiOpportunity>(element) }.getOrNull()
                                element.jsonPrimitive.contentOrNull != null ->
                                    AiOpportunity(name = element.jsonPrimitive.content, description = "")
                                else -> null
                            }
                        }
                        ?: emptyList(),
                recommendations =
                    root["recommendations"]?.jsonArray
                        ?.mapNotNull { element: JsonElement ->
                            when {
                                element is JsonObject ->
                                    runCatching { json.decodeFromJsonElement<AiRecommendation>(element) }.getOrNull()
                                element.jsonPrimitive.contentOrNull != null ->
                                    AiRecommendation(title = element.jsonPrimitive.content, message = element.jsonPrimitive.content)
                                else -> null
                            }
                        }
                        ?: emptyList(),
            )
        } catch (fallbackError: Exception) {
            val preview = cleaned.take(400)
            throw IllegalStateException(
                "Failed to parse LLM JSON response. Preview (cleaned): $preview",
                cause.also { it.addSuppressed(fallbackError) },
            )
        }
    }
}

private fun ActivityRecord.toLifeStateTrackerEntry(): LifeStateTrackerEntry =
    LifeStateTrackerEntry(
        id = id,
        timestampStart = startTime ?: createdAt,
        timestampEnd = endTime,
        category = targetType ?: projectId ?: goalId,
        label = text,
        notes = null,
        energy = null,
        stress = null,
        importance = null,
        satisfaction = null,
        projectId = projectId,
        goalId = goalId,
        targetId = targetId,
        targetType = targetType,
    )
