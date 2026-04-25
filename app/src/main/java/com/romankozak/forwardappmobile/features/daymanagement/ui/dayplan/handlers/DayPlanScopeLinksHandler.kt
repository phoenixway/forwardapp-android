package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.handlers

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TodayTabScopeLinksHandler(
    private val settingsRepository: SettingsRepository,
    private val isScopeLinksSheetVisible: MutableStateFlow<Boolean>,
    private val scope: CoroutineScope,
) {
    private val linksMutationMutex = Mutex()

    fun addPlanProjectLink(projectId: String) {
        scope.launch(Dispatchers.IO) {
            linksMutationMutex.withLock {
                val current = settingsRepository.todayLinkedProjectIdsFlow.first()
                settingsRepository.setTodayLinkedProjectIds((current + projectId).toSet())
            }
        }
    }

    fun removePlanProjectLink(projectId: String) {
        scope.launch(Dispatchers.IO) {
            linksMutationMutex.withLock {
                val current = settingsRepository.todayLinkedProjectIdsFlow.first()
                settingsRepository.setTodayLinkedProjectIds(current.filterNot { it == projectId }.toSet())
            }
        }
    }

    fun addPlanAttachmentLink(attachmentId: String) {
        scope.launch(Dispatchers.IO) {
            linksMutationMutex.withLock {
                val current = settingsRepository.todayLinkedAttachmentIdsFlow.first()
                settingsRepository.setTodayLinkedAttachmentIds((current + attachmentId).toSet())
            }
        }
    }

    fun removePlanAttachmentLink(attachmentId: String) {
        scope.launch(Dispatchers.IO) {
            linksMutationMutex.withLock {
                val current = settingsRepository.todayLinkedAttachmentIdsFlow.first()
                settingsRepository.setTodayLinkedAttachmentIds(current.filterNot { it == attachmentId }.toSet())
            }
        }
    }

    fun setScopeContextsExpanded(expanded: Boolean) {
        scope.launch {
            settingsRepository.setDayScopeContextsExpanded(expanded)
        }
    }

    fun setScopeAttachmentsExpanded(expanded: Boolean) {
        scope.launch {
            settingsRepository.setDayScopeAttachmentsExpanded(expanded)
        }
    }

    fun toggleScopeLinksSheet() {
        isScopeLinksSheetVisible.value = !isScopeLinksSheetVisible.value
    }

    fun dismissScopeLinksSheet() {
        isScopeLinksSheetVisible.value = false
    }
}
