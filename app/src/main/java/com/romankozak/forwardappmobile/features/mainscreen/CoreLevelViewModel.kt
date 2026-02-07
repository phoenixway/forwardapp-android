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
import kotlinx.coroutines.launch
import javax.inject.Inject

private val CORE_TAGS = setOf("core", "main-beacons")

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

        fun addCoreLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, addTag = "core")
            }
        }

        fun removeCoreLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, removeTags = CORE_TAGS)
            }
        }

        private suspend fun updateTags(
            contextId: String,
            addTag: String? = null,
            removeTags: Set<String> = emptySet(),
        ) {
            val context = contextRepository.getContextById(contextId) ?: return
            val current = context.tags.orEmpty()
            val next =
                current
                    .filterNot { it in removeTags }
                    .toMutableList()
            if (addTag != null && addTag !in next) {
                next.add(addTag)
            }
            if (next != current) {
                contextRepository.updateContext(context.copy(tags = next))
            }
        }
    }
