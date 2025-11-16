package com.romankozak.forwardappmobile.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
import me.tatarka.inject.annotations.Inject

@Inject
class InjectedViewModelFactory(
    private val projectScreenViewModel: (SavedStateHandle) -> ProjectScreenViewModel,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass == ProjectScreenViewModel::class.java) {
            val savedStateHandle = extras.createSavedStateHandle()
            return projectScreenViewModel(savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
