package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StrategicArcUiState(
    val projects: List<Context> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class StrategicArcViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
    ) : ViewModel() {
        val uiState: StateFlow<StrategicArcUiState> =
            contextRepository.getAllContextsFlow()
                .map { projects ->
                    val arcProjects =
                        projects.filter {
                            it.tags?.contains("arc") == true
                        }
                    StrategicArcUiState(projects = arcProjects)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = StrategicArcUiState(isLoading = true),
                )
    }
