package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ArtifactHandler @Inject constructor(
    private val contextRepository: ContextRepository,
    private val stateManager: ContextStateManager,
    private val scope: CoroutineScope
) {
    fun onEditArtifact(artifact: ContextArtifact) {
        stateManager.updateState { it.copy(artifactToEdit = artifact) }
    }

    fun onDismissArtifactEditor() {
        stateManager.updateState { it.copy(artifactToEdit = null) }
    }

    fun onSaveArtifact(projectId: String, content: String) {
        scope.launch {
            val current = stateManager.uiState.value.artifactToEdit
            val artifact = current?.copy(content = content, updatedAt = System.currentTimeMillis())
                ?: ContextArtifact(
                    id = UUID.randomUUID().toString(),
                    contextId = projectId,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            contextRepository.updateContextArtifact(artifact)
            onDismissArtifactEditor()
        }
    }

    fun onAutoSaveArtifact(content: String) {
        val current = stateManager.uiState.value.artifactToEdit ?: return
        scope.launch {
            contextRepository.updateContextArtifact(current.copy(content = content, updatedAt = System.currentTimeMillis()))
        }
    }
}
