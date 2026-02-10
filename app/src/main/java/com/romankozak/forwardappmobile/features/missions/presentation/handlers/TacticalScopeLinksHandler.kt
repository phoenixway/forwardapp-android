package com.romankozak.forwardappmobile.features.missions.presentation.handlers

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TacticalScopeLinksHandler(
    private val settingsRepository: SettingsRepository,
    private val boardLinkedProjectIds: StateFlow<List<String>>,
    private val boardLinkedAttachmentIds: StateFlow<List<String>>,
    private val isScopeLinksSheetVisible: MutableStateFlow<Boolean>,
    private val scope: CoroutineScope,
) {
    fun addBoardProjectLink(projectId: String) {
        val updated = (boardLinkedProjectIds.value + projectId).distinct().toSet()
        scope.launch { settingsRepository.setTacticalLinkedProjectIds(updated) }
    }

    fun removeBoardProjectLink(projectId: String) {
        val updated = boardLinkedProjectIds.value.filterNot { it == projectId }.toSet()
        scope.launch { settingsRepository.setTacticalLinkedProjectIds(updated) }
    }

    fun addBoardAttachmentLink(attachmentId: String) {
        val updated = (boardLinkedAttachmentIds.value + attachmentId).distinct().toSet()
        scope.launch { settingsRepository.setTacticalLinkedAttachmentIds(updated) }
    }

    fun removeBoardAttachmentLink(attachmentId: String) {
        val updated = boardLinkedAttachmentIds.value.filterNot { it == attachmentId }.toSet()
        scope.launch { settingsRepository.setTacticalLinkedAttachmentIds(updated) }
    }

    fun setScopeContextsExpanded(expanded: Boolean) {
        scope.launch { settingsRepository.setTacticalScopeContextsExpanded(expanded) }
    }

    fun setScopeAttachmentsExpanded(expanded: Boolean) {
        scope.launch { settingsRepository.setTacticalScopeAttachmentsExpanded(expanded) }
    }

    fun toggleScopeLinksSheet() {
        isScopeLinksSheetVisible.value = !isScopeLinksSheetVisible.value
    }

    fun dismissScopeLinksSheet() {
        isScopeLinksSheetVisible.value = false
    }
}
