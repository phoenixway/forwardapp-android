package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CoreLevelUiState(
    val projects: List<Context> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CoreLevelViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
    ) : ViewModel() {
        val uiState: StateFlow<CoreLevelUiState> =
            contextRepository.getAllContextsFlow()
                .map { projects ->
                    val coreProjects =
                        projects.filter {
                            it.tags?.contains("main-beacons") == true || it.tags?.contains("core") == true
                        }
                    CoreLevelUiState(projects = coreProjects)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = CoreLevelUiState(isLoading = true),
                )
    }
