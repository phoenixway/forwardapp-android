package com.romankozak.forwardappmobile.features.attachments.specifictypes.musicnote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicNoteUiState(
    val id: String = "",
    val name: String = "",
    val content: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class MusicNoteViewModel
    @Inject
    constructor(
        private val musicNoteRepository: MusicNoteRepository,
        private val recentItemsRepository: RecentItemsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val musicNoteId: String = savedStateHandle["musicNoteId"] ?: ""

        private val _uiState = MutableStateFlow(MusicNoteUiState())
        val uiState: StateFlow<MusicNoteUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                if (musicNoteId.isBlank()) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val note = musicNoteRepository.getById(musicNoteId)
                if (note == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                recentItemsRepository.logMusicNoteAccess(note)
                _uiState.update {
                    it.copy(
                        id = note.id,
                        name = note.name,
                        content = note.content,
                        isLoading = false,
                    )
                }
            }
        }

        fun onNameChange(value: String) {
            _uiState.update { it.copy(name = value) }
        }

        fun onContentChange(value: String) {
            _uiState.update { it.copy(content = value) }
        }

        fun save() {
            val current = _uiState.value
            if (current.id.isBlank()) return
            viewModelScope.launch {
                val existing = musicNoteRepository.getById(current.id) ?: return@launch
                val updated =
                    existing.copy(
                        name = current.name.trim().ifBlank { "Music note" },
                        content = current.content,
                    )
                musicNoteRepository.update(updated)
                recentItemsRepository.logMusicNoteAccess(updated)
            }
        }
    }
