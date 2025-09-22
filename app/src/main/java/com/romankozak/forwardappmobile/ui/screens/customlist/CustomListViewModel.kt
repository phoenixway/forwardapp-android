package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    init {
        val listId = savedStateHandle.get<String>("listId")
        // TODO: Load list items
    }
}
