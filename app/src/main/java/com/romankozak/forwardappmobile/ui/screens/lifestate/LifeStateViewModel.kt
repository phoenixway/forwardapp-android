package com.romankozak.forwardappmobile.ui.screens.lifestate

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.romankozak.forwardappmobile.domain.lifestate.AiAnalyzerService
import com.romankozak.forwardappmobile.domain.lifestate.LifeStateAnalysisWorker
import com.romankozak.forwardappmobile.domain.lifestate.model.AiAnalysis
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

data class LifeStateUiState(
    val isLoading: Boolean = false,
    val analysis: AiAnalysis? = null,
    val error: String? = null,
)

@HiltViewModel
class LifeStateViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val aiAnalyzerService: AiAnalyzerService,
) : ViewModel() {

    private val tag = "LifeStateViewModel"

    private val _uiState = MutableStateFlow(LifeStateUiState(isLoading = true))
    val uiState: StateFlow<LifeStateUiState> = _uiState.asStateFlow()

    companion object {
        private var cachedAnalysis: AiAnalysis? = null
        private const val LIFE_CHAT_TITLE = "Life Management"
        private const val LIFE_ANALYSIS_TAG = "life_analysis_unique"
    }

    private val cacheFile = File(appContext.filesDir, "life_state_analysis.json")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    init {
        loadAnalysis()
    }

    fun loadAnalysis(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                cachedAnalysis = null
                cacheFile.takeIf { it.exists() }?.delete()
            }
            if (!force && cachedAnalysis != null) {
                _uiState.value =
                    LifeStateUiState(
                        isLoading = false,
                        analysis = cachedAnalysis,
                        error = null,
                    )
                return@launch
            }
            if (!force) {
                val restored = restoreCachedAnalysis()
                if (restored != null) {
                    cachedAnalysis = restored
                    _uiState.value =
                        LifeStateUiState(
                            isLoading = false,
                            analysis = restored,
                            error = null,
                        )
                    return@launch
                }
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = aiAnalyzerService.analyzeLifeState()
                result.fold(
                    onSuccess = { analysis ->
                        cachedAnalysis = analysis
                        persistAnalysis(analysis)
                        _uiState.value = LifeStateUiState(isLoading = false, analysis = analysis, error = null)
                    },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Analysis failed",
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                Log.e(tag, "LifeState loadAnalysis failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Analysis failed",
                    )
                }
            }
        }
    }

    fun enqueueBackgroundAnalysis() {
        LifeStateAnalysisWorker.enqueue(appContext)
        observeBackgroundWork()
    }

    private fun observeBackgroundWork() {
        viewModelScope.launch {
            WorkManager.getInstance(appContext)
                .getWorkInfosByTagLiveData(LIFE_ANALYSIS_TAG)
                .asFlow()
                .collect { works ->
                    val finished = works.firstOrNull { it.state.isFinished }
                    if (finished != null && finished.state.isFinished) {
                        loadAnalysis(force = true)
                    }
                }
        }
    }

    private suspend fun persistAnalysis(analysis: AiAnalysis) {
        withContext(Dispatchers.IO) {
            runCatching {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(json.encodeToString(analysis))
            }.onFailure { error ->
                Log.w(tag, "Failed to persist analysis cache: ${error.message}", error)
            }
        }
    }

    private suspend fun restoreCachedAnalysis(): AiAnalysis? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!cacheFile.exists()) return@runCatching null
                val content = cacheFile.readText()
                json.decodeFromString<AiAnalysis>(content)
            }.onFailure { error ->
                Log.w(tag, "Failed to restore analysis cache: ${error.message}", error)
            }.getOrNull()
        }
}
