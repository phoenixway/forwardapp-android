package com.romankozak.forwardappmobile.ui

sealed class ModelsState {
    object Loading : ModelsState()

    data class Success(
        val models: List<String>,
    ) : ModelsState()

    data class Error(
        val message: String,
    ) : ModelsState()
}
