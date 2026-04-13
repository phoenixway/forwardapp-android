package com.romankozak.forwardappmobile.domain.ner

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton

data class Entity(
    val label: String,
    val start: Int,
    val end: Int,
    val text: String,
)

sealed class NerState {
    object NotInitialized : NerState()

    data class Downloading(
        val progress: Int,
    ) : NerState()

    object Ready : NerState()

    data class Error(
        val message: String,
    ) : NerState()
}

sealed class PredictionState {
    object Idle : PredictionState()

    object Processing : PredictionState()

    data class Error(
        val message: String,
    ) : PredictionState()
}

@Singleton
class NerManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settingsRepository: SettingsRepository,
    ) {
        private val _nerState = MutableStateFlow<NerState>(NerState.NotInitialized)
        val nerState = _nerState.asStateFlow()

        private val _predictionState = MutableStateFlow<PredictionState>(PredictionState.Idle)
        val predictionState = _predictionState.asStateFlow()

        private var nerProcessor: UkDtNerProcessor? = null
        private var currentModelUri = ""
        private var currentTokenizerUri = ""
        private var currentLabelsUri = ""

        private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        init {
            CoroutineScope(Dispatchers.IO).launch {
                combine(
                    settingsRepository.nerModelUriFlow,
                    settingsRepository.nerTokenizerUriFlow,
                    settingsRepository.nerLabelsUriFlow,
                ) { modelUri, tokenizerUri, labelsUri ->
                    Triple(modelUri, tokenizerUri, labelsUri)
                }.distinctUntilChanged().collect { (modelUri, tokenizerUri, labelsUri) ->
                    currentModelUri = modelUri
                    currentTokenizerUri = tokenizerUri
                    currentLabelsUri = labelsUri

                    if (modelUri.isNotBlank() && tokenizerUri.isNotBlank() && labelsUri.isNotBlank()) {
                        initialize(modelUri, tokenizerUri, labelsUri)
                    } else {
                        _nerState.value = NerState.NotInitialized
                        nerProcessor = null
                    }
                    Log.d("NerManager", "Model URI: $currentModelUri")
                    Log.d("NerManager", "Tokenizer URI: $currentTokenizerUri")
                    Log.d("NerManager", "Labels URI: $currentLabelsUri")
                }
            }
        }

        private fun initialize(
            modelUri: String,
            tokenizerUri: String,
            labelsUri: String,
        ) {
            val processor = UkDtNerProcessor(context, modelUri, tokenizerUri, labelsUri)
            nerProcessor = processor
            _nerState.value = NerState.Downloading(0)
            processor.initAsync(
                object : UkDtNerProcessor.ProgressListener {
                    override fun onProgress(percent: Int) {
                        _nerState.value = NerState.Downloading(percent)
                    }

                    override fun onComplete(
                        success: Boolean,
                        errorMessage: String?,
                    ) {
                        _nerState.value = if (success) NerState.Ready else NerState.Error(errorMessage ?: "Unknown error")
                    }
                },
            )
        }

        fun reinitialize() {
            if (currentModelUri.isNotBlank() && currentTokenizerUri.isNotBlank() && currentLabelsUri.isNotBlank()) {
                Log.d("NerManager", "Re-initializing NER model...")
                initialize(currentModelUri, currentTokenizerUri, currentLabelsUri)
            } else {
                Log.w("NerManager", "Re-initialization requested but not all file URIs are set.")
            }
        }

        fun predictAsync(
            text: String,
            callback: (List<Entity>?) -> Unit,
        ) {
            Log.d("NerManager", "predictAsync called with text: '$text', state: ${_nerState.value}")

            if (_nerState.value !is NerState.Ready) {
                Log.w("NerManager", "NER not ready, current state: ${_nerState.value}")
                callback(null)
                return
            }

            _predictionState.value = PredictionState.Processing

            managerScope.launch {
                try {
                    val result =
                        withTimeout(15000L) {
                            nerProcessor?.predict(text)
                        }
                    withContext(Dispatchers.Main) {
                        _predictionState.value = PredictionState.Idle
                        callback(result)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e("NerManager", "Prediction timeout")
                    withContext(Dispatchers.Main) {
                        _predictionState.value = PredictionState.Error("Prediction timeout")
                        callback(null)
                    }
                } catch (e: Exception) {
                    Log.e("NerManager", "Prediction failed", e)
                    withContext(Dispatchers.Main) {
                        _predictionState.value = PredictionState.Error(e.message ?: "Prediction failed")
                        callback(null)
                    }
                }
            }
        }
    }

class BertTokenizer private constructor(
    private val vocabMap: Map<String, Int>,
    private val maxLength: Int = 512,
) {
    companion object {
        fun fromJsonFile(tokenizerFile: File): BertTokenizer {
            val jsonText = tokenizerFile.readText()
            val json = JSONObject(jsonText)

            val vocabMap =
                when {
                    json.has("model") && json.getJSONObject("model").has("vocab") -> {
                        val vocab = json.getJSONObject("model").getJSONObject("vocab")
                        mutableMapOf<String, Int>().apply {
                            vocab.keys().forEach { key ->
                                put(key, vocab.getInt(key))
                            }
                        }
                    }
                    json.has("vocab") -> {
                        val vocab = json.getJSONObject("vocab")
                        mutableMapOf<String, Int>().apply {
                            vocab.keys().forEach { key ->
                                put(key, vocab.getInt(key))
                            }
                        }
                    }
                    else -> {
                        mutableMapOf<String, Int>().apply {
                            json.keys().forEach { key ->
                                put(key, json.getInt(key))
                            }
                        }
                    }
                }

            return BertTokenizer(vocabMap)
        }
    }

    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val unkToken = "[UNK]"
    private val padToken = "[PAD]"

    fun encode(text: String): EncodingResult {
        val tokens = mutableListOf<String>()
        val tokenIds = mutableListOf<Long>()
        val charSpans = mutableListOf<Pair<Int, Int>>()

        tokens.add(clsToken)
        tokenIds.add(getTokenId(clsToken))
        charSpans.add(0 to 0)

        var currentPos = 0
        val words = text.split(Regex("\\s+"))

        for (word in words) {
            if (word.isBlank()) continue

            val wordStart = text.indexOf(word, currentPos)
            val wordEnd = wordStart + word.length

            val subTokens =
                if (word.length > 10) {
                    val chunks = word.chunked(6)
                    chunks.mapIndexed { index, chunk ->
                        if (index == 0) chunk else "##$chunk"
                    }
                } else {
                    listOf(word)
                }

            subTokens.forEach { subToken ->
                tokens.add(subToken)
                tokenIds.add(getTokenId(subToken))
                charSpans.add(wordStart to wordEnd)
            }

            currentPos = wordEnd
        }

        tokens.add(sepToken)
        tokenIds.add(getTokenId(sepToken))
        charSpans.add(text.length to text.length)

        while (tokenIds.size < maxLength) {
            tokens.add(padToken)
            tokenIds.add(getTokenId(padToken))
            charSpans.add(text.length to text.length)
        }

        if (tokenIds.size > maxLength) {
            tokens.subList(maxLength - 1, tokens.size).clear()
            tokenIds.subList(maxLength - 1, tokenIds.size).clear()
            charSpans.subList(maxLength - 1, charSpans.size).clear()

            tokens[maxLength - 1] = sepToken
            tokenIds[maxLength - 1] = getTokenId(sepToken)
        }

        val attentionMask =
            LongArray(tokenIds.size) {
                if (tokens[it] == padToken) 0L else 1L
            }

        return EncodingResult(
            tokens = tokens.toTypedArray(),
            ids = tokenIds.toLongArray(),
            attentionMask = attentionMask,
            charSpans = charSpans.toTypedArray(),
        )
    }

    private fun getTokenId(token: String): Long = vocabMap[token]?.toLong() ?: vocabMap[unkToken]?.toLong() ?: 100L
}

data class EncodingResult(
    val tokens: Array<String>,
    val ids: LongArray,
    val attentionMask: LongArray,
    val charSpans: Array<Pair<Int, Int>>,
)

private data class NerInputTensors(
    val inputIdsTensor: OnnxTensor,
    val attentionTensor: OnnxTensor,
    val tokenTypeIdsTensor: OnnxTensor,
) {
    fun toInputMap(): Map<String, OnnxTensor> =
        mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionTensor,
            "token_type_ids" to tokenTypeIdsTensor,
        )

    fun close() {
        inputIdsTensor.close()
        attentionTensor.close()
        tokenTypeIdsTensor.close()
    }
}

private class UkDtNerProcessor(
    private val context: Context,
    private val modelUri: String,
    private val tokenizerUri: String,
    private val labelsUri: String,
) {
    private val TAG = "UkDtNerProcessor"
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var session: OrtSession
    private lateinit var tokenizer: BertTokenizer
    private lateinit var labels: List<String>

    interface ProgressListener {
        fun onProgress(percent: Int)

        fun onComplete(
            success: Boolean,
            errorMessage: String? = null,
        )
    }

    fun initAsync(listener: ProgressListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheDir = File(context.filesDir, "ner_cache")
                if (!cacheDir.exists()) cacheDir.mkdir()

                listener.onProgress(10)
                val modelFile = copyFileFromUriToCache(modelUri, File(cacheDir, "model.onnx"))
                listener.onProgress(40)
                val tokenizerFile = copyFileFromUriToCache(tokenizerUri, File(cacheDir, "tokenizer.json"))
                listener.onProgress(70)
                val labelsFile = copyFileFromUriToCache(labelsUri, File(cacheDir, "labels.json"))
                listener.onProgress(90)

                session = env.createSession(modelFile.readBytes())
                tokenizer = BertTokenizer.fromJsonFile(tokenizerFile)

                val jsonArray = JSONArray(labelsFile.readText())
                labels = (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }

                withContext(Dispatchers.Main) {
                    listener.onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initialization error:", e)
                withContext(Dispatchers.Main) {
                    listener.onComplete(false, e.message ?: "Exception with no message")
                }
            }
        }
    }

    private suspend fun copyFileFromUriToCache(
        uriString: String,
        destinationFile: File,
    ): File =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uriString.toUri())?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw Exception("Failed to open input stream for URI: $uriString")
            destinationFile
        }

    fun predict(text: String): List<Entity> {
        if (!canPredict(text)) return emptyList()

        return try {
            val encoding = tokenizer.encode(text)
            val tensors = createInputTensors(encoding)
            try {
                val predIds = runInference(tensors)
                extractEntities(text = text, encoding = encoding, predIds = predIds)
            } finally {
                tensors.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during prediction", e)
            emptyList()
        }
    }

    private fun canPredict(text: String): Boolean =
        ::session.isInitialized &&
            ::tokenizer.isInitialized &&
            text.isNotBlank()

    private fun createInputTensors(encoding: EncodingResult): NerInputTensors {
        val shape = longArrayOf(1, encoding.ids.size.toLong())
        return NerInputTensors(
            inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.ids), shape),
            attentionTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.attentionMask), shape),
            tokenTypeIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(encoding.ids.size)), shape),
        )
    }

    private fun runInference(tensors: NerInputTensors): List<Int> {
        val logits =
            session.run(tensors.toInputMap()).use { results ->
                @Suppress("UNCHECKED_CAST")
                results[0].value as Array<Array<FloatArray>>
            }
        return logits[0].map { row ->
            row.indices.maxByOrNull { row[it] } ?: 0
        }
    }

    private fun extractEntities(
        text: String,
        encoding: EncodingResult,
        predIds: List<Int>,
    ): List<Entity> {
        val entities = mutableListOf<Entity>()
        var index = 0

        while (index < predIds.size && index < encoding.tokens.size) {
            if (encoding.tokens[index].isSpecialToken()) {
                index++
                continue
            }

            val nextIndex =
                collectEntityAtIndex(
                    text = text,
                    encoding = encoding,
                    predIds = predIds,
                    index = index,
                    entities = entities,
                )
            index = nextIndex ?: index + 1
        }

        return entities
    }

    private fun String.isSpecialToken(): Boolean =
        this == "[CLS]" || this == "[SEP]" || this == "[PAD]"

    private fun collectEntityAtIndex(
        text: String,
        encoding: EncodingResult,
        predIds: List<Int>,
        index: Int,
        entities: MutableList<Entity>,
    ): Int? {
        val labelIndex = predIds.getOrNull(index) ?: return null
        val label = labels.getOrNull(labelIndex) ?: return null
        if (!label.startsWith("B-")) return null

        val kind = label.substring(2)
        var endIndex = index
        var cursor = index + 1
        while (cursor < predIds.size && isInsideEntity(predIds, cursor, kind)) {
            endIndex = cursor
            cursor++
        }

        buildEntityFromSpan(
            text = text,
            encoding = encoding,
            startIndex = index,
            endIndex = endIndex,
            kind = kind,
        )?.let(entities::add)
        return cursor
    }

    private fun isInsideEntity(
        predIds: List<Int>,
        index: Int,
        kind: String,
    ): Boolean {
        val predictedLabelIndex = predIds.getOrNull(index) ?: return false
        val predictedLabel = labels.getOrNull(predictedLabelIndex) ?: return false
        return predictedLabel == "I-$kind"
    }

    private fun buildEntityFromSpan(
        text: String,
        encoding: EncodingResult,
        startIndex: Int,
        endIndex: Int,
        kind: String,
    ): Entity? {
        if (startIndex !in encoding.charSpans.indices || endIndex !in encoding.charSpans.indices) return null
        val (startChar, _) = encoding.charSpans[startIndex]
        val (_, endCharRaw) = encoding.charSpans[endIndex]
        val start = startChar.coerceIn(0, text.length)
        val end = endCharRaw.coerceIn(start, text.length)
        if (end <= start) return null
        return Entity(kind, start, end, text.substring(start, end))
    }
}
