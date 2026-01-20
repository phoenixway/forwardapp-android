package com.romankozak.forwardappmobile.features.context_lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.features.context_lab.domain.SwitchContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ContextLabViewModel @Inject constructor(
    private val labController: ContextLabController,
    private val switchContextUseCase: SwitchContextUseCase,
    private val registry: CapabilityRegistry
) : ViewModel() {

    // Всі зареєстровані в системі можливості (для списку чекбоксів)
    val allCapabilities = registry.all()

    // Стан списку контекстів у лабораторії
    private val _uiState = MutableStateFlow(labController.getAllContexts())
    val uiState = _uiState.asStateFlow()

    // Поточний активний контекст (для візуальної позначки)
    private val _activeContextId = MutableStateFlow(labController.getActiveContext()?.id)
    val activeContextId = _activeContextId.asStateFlow()

    fun onToggleCapability(contextId: ContextId, capId: CapabilityId) {
        labController.toggleCapability(contextId, capId)
        _uiState.value = labController.getAllContexts()
    }

    fun onActivateContext(contextId: ContextId) {
        switchContextUseCase.execute(contextId)
        _activeContextId.value = contextId
    }
}