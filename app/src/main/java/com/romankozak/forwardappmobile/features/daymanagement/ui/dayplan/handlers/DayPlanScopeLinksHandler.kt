package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.handlers

import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DayPlanScopeLinksHandler(
    private val dayManagementRepository: DayManagementRepository,
    private val settingsRepository: SettingsRepository,
    private val planIdFlow: StateFlow<String?>,
    private val isScopeLinksSheetVisible: MutableStateFlow<Boolean>,
    private val scope: CoroutineScope,
) {
    fun addPlanProjectLink(projectId: String) {
        val planId = planIdFlow.value ?: return
        scope.launch(Dispatchers.IO) {
            val current = dayManagementRepository.getPlanById(planId) ?: return@launch
            dayManagementRepository.updatePlanLinks(
                planId = planId,
                linkedProjectIds = (current.linkedProjectIds.orEmpty() + projectId).distinct(),
                linkedAttachmentIds = current.linkedAttachmentIds.orEmpty(),
            )
        }
    }

    fun removePlanProjectLink(projectId: String) {
        val planId = planIdFlow.value ?: return
        scope.launch(Dispatchers.IO) {
            val current = dayManagementRepository.getPlanById(planId) ?: return@launch
            dayManagementRepository.updatePlanLinks(
                planId = planId,
                linkedProjectIds = current.linkedProjectIds.orEmpty().filterNot { it == projectId },
                linkedAttachmentIds = current.linkedAttachmentIds.orEmpty(),
            )
        }
    }

    fun addPlanAttachmentLink(attachmentId: String) {
        val planId = planIdFlow.value ?: return
        scope.launch(Dispatchers.IO) {
            val current = dayManagementRepository.getPlanById(planId) ?: return@launch
            dayManagementRepository.updatePlanLinks(
                planId = planId,
                linkedProjectIds = current.linkedProjectIds.orEmpty(),
                linkedAttachmentIds = (current.linkedAttachmentIds.orEmpty() + attachmentId).distinct(),
            )
        }
    }

    fun removePlanAttachmentLink(attachmentId: String) {
        val planId = planIdFlow.value ?: return
        scope.launch(Dispatchers.IO) {
            val current = dayManagementRepository.getPlanById(planId) ?: return@launch
            dayManagementRepository.updatePlanLinks(
                planId = planId,
                linkedProjectIds = current.linkedProjectIds.orEmpty(),
                linkedAttachmentIds = current.linkedAttachmentIds.orEmpty().filterNot { it == attachmentId },
            )
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
