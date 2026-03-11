package com.romankozak.forwardappmobile.features.attachments.specifictypes.notedocument

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.ui.common.editor.NoteTitleExtractor
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDocumentEditorViewModel
    @Inject
    constructor(
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val contextRepository: ContextRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val universalEditorViewModel = UniversalEditorViewModel(application)
        private var listId: String? = null

        val linkSuggestions =
            combine(
                noteDocumentRepository.getAllDocumentsAsFlow(),
                musicNoteRepository.getAllMusicNotesAsFlow(),
                checklistRepository.getAllChecklistsAsFlow(),
                contextRepository.getAllContextsFlow(),
            ) { docs, musicNotes, checklists, contexts ->
                (docs.map { doc -> "doc:${doc.id}|${doc.name.ifBlank { "Untitled" }}" } +
                    musicNotes.map { note -> "music:${note.id}|${note.name.ifBlank { "Untitled" }}" } +
                    checklists.map { checklist -> "checklist:${checklist.id}|${checklist.name.ifBlank { "Untitled" }}" } +
                    contexts.map { ctx -> "ctx:${ctx.id}|${ctx.name.ifBlank { "Untitled" }}" })
                    .filter { token -> token.substringAfter('|', "").isNotBlank() }
                    .distinct()
            }
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        val contextSuggestions =
            contextRepository.getAllContextsFlow()
                .map { contexts ->
                    contexts.map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        fun loadDocument(id: String) {
            listId = id
            viewModelScope.launch {
                noteDocumentRepository.getDocumentById(id)?.let { document ->
                    recentItemsRepository.logNoteDocumentAccess(document)
                    Log.d("CursorDebug", "Loaded document with lastCursorPosition: ${document.lastCursorPosition}")
                    // Встановлюємо projectId для "Show Location"
                    universalEditorViewModel.setProjectId(document.contextId)
                    universalEditorViewModel.setInitialContent(
                        document.content ?: "",
                        document.lastCursorPosition,
                    )
                }
            }
        }

        fun saveDocument(
            content: String,
            cursorPosition: Int,
        ) {
            Log.d("CursorDebug", "saveDocument called with cursorPosition: $cursorPosition")
            listId?.let {
                viewModelScope.launch {
                    noteDocumentRepository.getDocumentById(it)?.let { document ->
                        val name = NoteTitleExtractor.extractOrNull(content)?.take(100) ?: document.name
                        Log.d("NoteTitleExtractor", "saveDocument extracted title='$name'")
                        val updatedDocument =
                            document.copy(
                                name = name,
                                content = content,
                                updatedAt = System.currentTimeMillis(),
                                lastCursorPosition = cursorPosition,
                            )
                        noteDocumentRepository.updateDocument(updatedDocument)
                    }
                }
            }
        }

        suspend fun findDocumentIdByName(name: String): String? = noteDocumentRepository.findDocumentByName(name)?.id

        suspend fun findMusicNoteIdByName(name: String): String? = musicNoteRepository.findByName(name)?.id

        suspend fun findChecklistIdByName(name: String): String? = checklistRepository.findByName(name)?.id

        suspend fun findContextIdByName(name: String): String? =
            contextRepository
                .getAllContextsFlow()
                .first()
                .firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.id
    }
