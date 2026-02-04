
package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class ArtifactHandler(
    private val contextRepository: ContextRepository,
    private val stateManager: ContextStateManager,
    private val scope: CoroutineScope,
    private val projectId: String
) {
    fun onEditArtifact(artifact: ContextArtifact) {
        stateManager.updateState { it.copy(artifactToEdit = artifact) }
    }

    fun onDismissArtifactEditor() {
        stateManager.updateState { it.copy(artifactToEdit = null) }
    }

    fun onSaveArtifact(content: String) {
        scope.launch {
            val currentArtifact = stateManager.uiState.value.artifactToEdit
            if (currentArtifact == null) {
                // Створення нового
                contextRepository.updateContextArtifact(
                    ContextArtifact(
                        id = UUID.randomUUID().toString(),
                        contextId = projectId,
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            } else {
                // Оновлення існуючого
                contextRepository.updateContextArtifact(
                    currentArtifact.copy(
                        content = content, 
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            onDismissArtifactEditor()
        }
    }

    fun onAutoSaveArtifact(content: String) {
        scope.launch {
            val currentArtifact = stateManager.uiState.value.artifactToEdit ?: return@launch
            contextRepository.updateContextArtifact(
                currentArtifact.copy(
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
