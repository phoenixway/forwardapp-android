package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ViewModelScoped
class ContextSelectionCoordinator
    @Inject
    constructor() {
        private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
        val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

        fun retainExistingContextIds(existingIds: Set<String>) {
            _selectedIds.update { selectedIds -> selectedIds.intersect(existingIds) }
        }

        fun handleContextClick(contextId: String): Boolean {
            if (_selectedIds.value.isEmpty()) return false
            toggle(contextId)
            return true
        }

        fun start(contextId: String) {
            _selectedIds.update { selectedIds -> selectedIds + contextId }
        }

        fun toggle(contextId: String) {
            _selectedIds.update { selectedIds ->
                if (contextId in selectedIds) {
                    selectedIds - contextId
                } else {
                    selectedIds + contextId
                }
            }
        }

        fun clear() {
            _selectedIds.value = emptySet()
        }

        fun takeSelection(): Set<String> {
            val selectedIds = _selectedIds.value
            clear()
            return selectedIds
        }
    }
