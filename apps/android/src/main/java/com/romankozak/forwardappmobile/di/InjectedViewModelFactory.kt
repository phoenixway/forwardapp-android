package com.romankozak.forwardappmobile.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import me.tatarka.inject.annotations.Inject
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel

@Inject
class InjectedViewModelFactory(
    private val projectScreenViewModel: () -> ProjectScreenViewModel
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {

        if (modelClass == ProjectScreenViewModel::class.java) {
            val savedStateHandle: SavedStateHandle = extras.createSavedStateHandle()

            val vm = projectScreenViewModel()
            vm.savedStateHandle = savedStateHandle

            return vm as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}