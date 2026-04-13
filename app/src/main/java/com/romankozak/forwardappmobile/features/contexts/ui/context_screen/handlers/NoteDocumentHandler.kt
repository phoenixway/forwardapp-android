@file:Suppress("PackageNaming")

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

private const val UTF_8 = "UTF-8"

class NoteDocumentHandler
    @Inject
    constructor(
        private val noteDocumentRepository: NoteDocumentRepository,
        private val settingsRepository: SettingsRepository,
        private val stateManager: ContextStateManager,
        private val resultListener: ResultListener,
        private val scope: CoroutineScope,
    ) {
        interface ResultListener {
            fun showSnackbar(message: String)

            fun openUri(uri: String)
        }

        fun onShowCreateNoteDocumentDialog() {
            stateManager.updateState { it.copy(showCreateNoteDocumentDialog = true) }
        }

        fun onDismissNoteDocumentEditor() {
            stateManager.updateState { it.copy(showNoteDocumentEditor = false) }
        }

        fun onCreateChecklist(@Suppress("UNUSED_PARAMETER") projectId: String) = Unit

        fun createObsidianNote(noteName: String) {
            scope.launch {
                val vaultName = settingsRepository.obsidianVaultNameFlow.first()
                if (vaultName.isBlank()) {
                    resultListener.showSnackbar("Obsidian vault name is not configured.")
                    return@launch
                }
                val encodedNoteName = URLEncoder.encode(noteName, UTF_8)
                val uri = "obsidian://new?vault=$vaultName&name=$encodedNoteName"
                resultListener.openUri(uri)
            }
        }

        fun onSaveNoteDocument(
            contextId: String,
            name: String,
            content: String,
        ) {
            scope.launch {
                val existingDocument = noteDocumentRepository.findDocumentByName(name)
                if (existingDocument != null) {
                    noteDocumentRepository.updateDocument(existingDocument.copy(content = content))
                } else {
                    noteDocumentRepository.createDocument(name, contextId, content)
                }
                onDismissNoteDocumentEditor()
                resultListener.showSnackbar("Note document saved.")
            }
        }
    }
