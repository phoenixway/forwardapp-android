package com.romankozak.forwardappmobile.features.context_lab

import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.context.Context
import com.romankozak.forwardappmobile.core.context.ContextController
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextState
import com.romankozak.forwardappmobile.core.context.ViewSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// features/context_lab/ContextLabViewModel.kt

class ContextLabViewModel @Inject constructor(
    private val labController: ContextLabController,
    private val systemController: ContextController, //
    private val registry: CapabilityRegistry // [cite: 1]
) : ViewModel() {

    // Список усіх доступних дескрипторів для відображення в UI
    val allCapabilities = registry.all() // [cite: 1, 2]

    // Поточний стан лабораторії
    private val _uiState = MutableStateFlow(labController.getAllContexts())
    val uiState = _uiState.asStateFlow()

    fun onToggleCapability(contextId: ContextId, capId: CapabilityId) { // [cite: 2, 5]
        labController.toggleCapability(contextId, capId)
        _uiState.value = labController.getAllContexts()

        // Якщо це поточний активний контекст — оновлюємо систему
        val active = labController.getActiveContext()
        if (active?.id == contextId) {
            updateSystemState(active)
        }
    }

    private fun updateSystemState(context: Context) {
        systemController.update { currentState -> //
            // Перетворюємо наш експериментальний контекст у системний ContextState
            object : ContextState { //
                override val id = context.id //[cite: 5]
                override val features = CapabilitySet(context.config.activeCapabilities)
                override val views = ViewSet(context.config.activeViews, context.config.currentView)
            }
        }
    }
}