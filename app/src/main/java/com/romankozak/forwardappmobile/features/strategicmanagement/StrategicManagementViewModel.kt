package com.romankozak.forwardappmobile.features.strategicmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STRATEGIC_TAG = "strategic"

@HiltViewModel
class StrategicManagementViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
    ) : ViewModel() {
        val uiState: StateFlow<StrategicManagementUiState> =
            contextRepository.getAllContextsFlow()
                .map { projects ->
                    val strategic =
                        projects.filter {
                            it.tags?.contains(STRATEGIC_TAG) == true
                        }
                    StrategicManagementUiState(dashboardProjects = strategic)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = StrategicManagementUiState(isLoading = true),
                )

        private val _currentTab = MutableStateFlow(StrategicManagementTab.DASHBOARD)
        val currentTab = _currentTab.asStateFlow()

        fun onTabSelected(tab: StrategicManagementTab) {
            _currentTab.value = tab
        }

        fun addStrategicLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, addTag = STRATEGIC_TAG)
            }
        }

        fun removeStrategicLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, removeTags = setOf(STRATEGIC_TAG))
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
