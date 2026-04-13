package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusContextsViewModel
    @Inject
    constructor(
        private val focusContextRepository: FocusContextRepository,
        private val contextRepository: ContextRepository,
        private val activityRepository: ActivityRepository,
    ) : ViewModel() {
        data class FocusedContextUi(
            val contextId: String,
            val name: String,
            val startedAt: Long,
        )

        val focusedContexts: StateFlow<List<FocusedContextUi>> =
            combine(
                focusContextRepository.observeActiveFocusContexts(),
                contextRepository.getAllContextsFlow(),
            ) { focused, contexts ->
                val nameById = contexts.associateBy({ it.id }, { it.name })
                focused.mapNotNull { row ->
                    val name = nameById[row.contextId] ?: return@mapNotNull null
                    FocusedContextUi(
                        contextId = row.contextId,
                        name = name,
                        startedAt = row.startedAt,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        fun startTracking(contextId: String) {
            if (contextId.isBlank()) return
            viewModelScope.launch {
                activityRepository.startContextActivity(contextId)
            }
        }

        fun unfocus(contextId: String) {
            if (contextId.isBlank()) return
            viewModelScope.launch {
                focusContextRepository.unfocusContext(contextId)
            }
        }

        fun updateFocusedContextsOrder(orderedContextIds: List<String>) {
            if (orderedContextIds.isEmpty()) return
            viewModelScope.launch {
                focusContextRepository.updateActiveFocusOrder(orderedContextIds)
            }
        }
    }
