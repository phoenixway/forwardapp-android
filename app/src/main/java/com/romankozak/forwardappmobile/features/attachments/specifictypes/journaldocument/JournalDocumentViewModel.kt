package com.romankozak.forwardappmobile.features.attachments.specifictypes.journaldocument

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalDocumentUiState(
    val document: NoteDocumentEntity? = null,
    val isLoading: Boolean = true,
)

sealed interface JournalDocumentEvent {
    data object NavigateBack : JournalDocumentEvent

    data class ShowMessage(val message: String) : JournalDocumentEvent
}

@HiltViewModel
class JournalDocumentViewModel
    @Inject
    constructor(
        private val application: Application,
        private val noteDocumentRepository: NoteDocumentRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val documentId: String? = savedStateHandle["documentId"]

        private val _uiState = MutableStateFlow(JournalDocumentUiState())
        val uiState: StateFlow<JournalDocumentUiState> = _uiState.asStateFlow()

        private val _events = Channel<JournalDocumentEvent>()
        val events = _events.receiveAsFlow()

        init {
            val id = documentId
            if (id.isNullOrBlank()) {
                viewModelScope.launch { _events.send(JournalDocumentEvent.NavigateBack) }
            } else {
                viewModelScope.launch {
                    noteDocumentRepository.getDocumentByIdFlow(id).collect { document ->
                        if (document == null) {
                            _events.send(JournalDocumentEvent.NavigateBack)
                        } else {
                            _uiState.update {
                                it.copy(
                                    document = document,
                                    isLoading = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        fun addEntry(text: String) {
            val document = _uiState.value.document ?: return
            val normalized = text.trim()
            if (normalized.isBlank()) return
            val appendedContent =
                document.content
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "$it\n$normalized" }
                    ?: normalized
            saveDocument(document.copy(content = appendedContent, lastCursorPosition = appendedContent.length))
        }

        fun updateLine(lineIndex: Int, updatedText: String) {
            mutateLines { lines ->
                val safeIndex = lineIndex.takeIf { it in lines.indices } ?: return@mutateLines lines
                lines.toMutableList().apply { this[safeIndex] = updatedText }
            }
        }

        fun deleteLine(lineIndex: Int) {
            mutateLines { lines ->
                val safeIndex = lineIndex.takeIf { it in lines.indices } ?: return@mutateLines lines
                lines.toMutableList().apply { removeAt(safeIndex) }
            }
        }

        fun reorderLines(updatedLines: List<String>) {
            val document = _uiState.value.document ?: return
            val normalizedContent = updatedLines.joinToString("\n")
            saveDocument(document.copy(content = normalizedContent, lastCursorPosition = normalizedContent.length))
        }

        fun renameDocument(newName: String) {
            val document = _uiState.value.document ?: return
            val normalizedName = newName.trim()
            if (normalizedName.isBlank() || normalizedName == document.name) return
            saveDocument(document.copy(name = normalizedName))
            viewModelScope.launch {
                _events.send(JournalDocumentEvent.ShowMessage("Назву документа оновлено"))
            }
        }

        fun clearDocument() {
            val document = _uiState.value.document ?: return
            saveDocument(document.copy(content = "", lastCursorPosition = 0))
            viewModelScope.launch {
                _events.send(JournalDocumentEvent.ShowMessage("Журнал очищено"))
            }
        }

        fun exportDocument() {
            val document = _uiState.value.document ?: return
            val text = document.content.orEmpty()
            val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("journal_document", text))
            viewModelScope.launch {
                _events.send(JournalDocumentEvent.ShowMessage("Журнал скопійовано"))
            }
        }

        private fun mutateLines(transform: (List<String>) -> List<String>) {
            val document = _uiState.value.document ?: return
            val nextLines = transform(document.content.orEmpty().lines())
            val nextContent = nextLines.joinToString("\n")
            saveDocument(document.copy(content = nextContent, lastCursorPosition = nextContent.length))
        }

        private fun saveDocument(document: NoteDocumentEntity) {
            viewModelScope.launch {
                noteDocumentRepository.updateDocument(document)
            }
        }
    }
