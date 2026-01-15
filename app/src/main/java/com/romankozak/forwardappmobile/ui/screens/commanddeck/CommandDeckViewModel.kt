package com.romankozak.forwardappmobile.ui.screens.commanddeck

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.domain.lifecontext.SubmitContextInputUseCase
import com.romankozak.forwardappmobile.domain.lifecontext.StartContextTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CommandDeckViewModel @Inject constructor(
    private val application: Application,
    private val submitContextInputUseCase: SubmitContextInputUseCase,
    private val startContextTrackingUseCase: StartContextTrackingUseCase,
) : ViewModel() {

    private val sharedPreferences = application.getSharedPreferences("command_deck_prefs", Context.MODE_PRIVATE)

    private val _isContextInputVisible = MutableStateFlow(false)
    val isContextInputVisible: StateFlow<Boolean> = _isContextInputVisible.asStateFlow()

    private val _contextInputText = MutableStateFlow("")
    val contextInputText: StateFlow<String> = _contextInputText.asStateFlow()

    fun isCategoryExpanded(categoryTitle: String): Boolean {
        return sharedPreferences.getBoolean(categoryTitle, false)
    }

    fun setCategoryExpanded(categoryTitle: String, isExpanded: Boolean) {
        sharedPreferences.edit().putBoolean(categoryTitle, isExpanded).apply()
    }

    fun openContextInput() {
        _isContextInputVisible.value = true
    }

    fun closeContextInput() {
        _isContextInputVisible.value = false
    }

    fun onContextInputChange(text: String) {
        _contextInputText.value = text
    }

    fun clearContextInput() {
        _contextInputText.value = ""
    }

    fun submitContextInput() {
        val text = _contextInputText.value.trim()
        if (text.isEmpty()) {
            closeContextInput()
            return
        }
        viewModelScope.launch {
            submitContextInputUseCase(text)
            clearContextInput()
            closeContextInput()
        }
    }

    fun startContextTracking() {
        val text = _contextInputText.value.trim()
        if (text.isEmpty()) {
            closeContextInput()
            return
        }
        viewModelScope.launch {
            startContextTrackingUseCase(text)
            clearContextInput()
            closeContextInput()
        }
    }
}
