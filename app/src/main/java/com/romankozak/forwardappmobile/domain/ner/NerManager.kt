package com.romankozak.forwardappmobile.domain.ner

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.Uri
import android.util.Log
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton

data class Entity(val label: String, val start: Int, val end: Int, val text: String)

sealed class NerState {
    object NotInitialized : NerState()
    data class Downloading(val progress: Int) : NerState()
    object Ready : NerState()
    data class Error(val message: String) : NerState()
}

@Singleton
class NerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val _nerState = MutableStateFlow<NerState>(NerState.NotInitialized)
    val nerState = _nerState.asStateFlow()

    private var nerProcessor: UkDtNerProcessor? = null
    private var currentModelUri = ""
    private var currentTokenizerUri = ""
    private var currentLabelsUri = ""

    init {
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                settingsRepository.nerModelUriFlow,
                settingsRepository.nerTokenizerUriFlow,
                settingsRepository.nerLabelsUriFlow
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
            }
        }
    }

    private fun initialize(modelUri: String, tokenizerUri: String, labelsUri: String) {
        val processor = UkDtNerProcessor(context, modelUri, tokenizerUri, labelsUri)
        nerProcessor = processor
        _nerState.value = NerState.Downloading(0)
        processor.initAsync(object : UkDtNerProcessor.ProgressListener {
            override fun onProgress(percent: Int) {
                _nerState.value = NerState.Downloading(percent)
            }
            override fun onComplete(success: Boolean) {
                _nerState.value = if (success) NerState.Ready else NerState.Error("Failed to load NER model")
            }
        })
    }

    fun reinitialize() {
        if (currentModelUri.isNotBlank() && currentTokenizerUri.isNotBlank() && currentLabelsUri.isNotBlank()) {
            Log.d("NerManager", "Re-initializing NER model...")
            initialize(currentModelUri, currentTokenizerUri, currentLabelsUri)
        } else {
            Log.w("NerManager", "Re-initialization requested but not all file URIs are set.")
        }
    }

    fun predict(text: String): List<Entity>? {
        if (_nerState.value !is NerState.Ready) {
            Log.w("NerManager", "Predict called but NER is not ready. State: ${nerState.value}")
            return null
        }
        return nerProcessor?.predict(text)
    }
}


private class UkDtNerProcessor(
    private val context: Context,
    private val modelUri: String,
    private val tokenizerUri: String,
    private val labelsUri: String
) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var session: OrtSession
    private lateinit var tokenizer: HuggingFaceTokenizer
    private lateinit var labels: List<String>

    interface ProgressListener {
        fun onProgress(percent: Int)
        fun onComplete(success: Boolean)
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
                tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile.toPath())

                val jsonArray = JSONArray(labelsFile.readText())
                labels = (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }

                withContext(Dispatchers.Main) {
                    listener.onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("UkDtNerProcessor", "File copy/init Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    listener.onComplete(false)
                }
            }
        }
    }

    private suspend fun copyFileFromUriToCache(uriString: String, destinationFile: File): File {
        return withContext(Dispatchers.IO) {
            Log.d("UkDtNerProcessor", "Copying ${Uri.parse(uriString).lastPathSegment} to ${destinationFile.name}")
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("UkDtNerProcessor", "Copying complete for ${destinationFile.name}")
            destinationFile
        }
    }

    fun predict(text: String): List<Entity> {



        if (!::session.isInitialized || !::tokenizer.isInitialized) return emptyList()





        val encoding = tokenizer.encode(text)



        val inputIds = encoding.getIds()



        val attentionMask = encoding.getAttentionMask()



        val spans = encoding.getCharTokenSpans()  // Ось правильне джерело offset-ів





        val shape = longArrayOf(1, inputIds.size.toLong())



        val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape)



        val attnTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)



        val inputs = mapOf("input_ids" to inputIdsTensor, "attention_mask" to attnTensor)





        val logits = session.run(inputs).use { results ->



            @Suppress("UNCHECKED_CAST")



            results[0].value as Array<Array<FloatArray>>



        }





        val predIds = logits[0].map { row ->



            row.indices.maxByOrNull { row[it] } ?: 0



        }





        val entities = mutableListOf<Entity>()



        var i = 0



        while (i < predIds.size) {



            if (encoding.getTokens()[i] == "[CLS]" || encoding.getTokens()[i] == "[SEP]") {



                i++; continue



            }



            val label = labels[predIds[i]]



            if (label.startsWith("B-")) {



                val kind = label.substring(2)



                val startTok = i



                var endTok = i



                i++



                while (i < predIds.size && labels[predIds[i]] == "I-$kind") {



                    endTok = i



                    i++



                }



                val s = spans[startTok].getStart()



                val e = spans[endTok].getEnd()



                val spanText = text.substring(



                    s.toInt().coerceIn(0, text.length),



                    e.toInt().coerceIn(0, text.length)



                )



                entities.add(Entity(kind, s.toInt(), e.toInt(), spanText))



            } else {



                i++



            }



        }



        return entities



    }

}