package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.features.missions.domain.usecase.AddTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.DeleteTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.GetTacticalMissionsUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.UpdateTacticalMissionUseCase
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TacticalMissionViewModel
    @Inject
    constructor(
        private val getTacticalMissionsUseCase: GetTacticalMissionsUseCase,
        private val addTacticalMissionUseCase: AddTacticalMissionUseCase,
        private val updateTacticalMissionUseCase: UpdateTacticalMissionUseCase,
        private val deleteTacticalMissionUseCase: DeleteTacticalMissionUseCase,
        private val missionRepository: MissionRepository,
        private val contextRepository: ContextRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val _missions = MutableStateFlow<List<TacticalMission>>(emptyList())
        val missions: StateFlow<List<TacticalMission>> = _missions.asStateFlow()

        private val _projectOptions = MutableStateFlow<List<ProjectOption>>(emptyList())
        val projectOptions: StateFlow<List<ProjectOption>> = _projectOptions.asStateFlow()

        private val _attachmentOptions = MutableStateFlow<List<AttachmentOption>>(emptyList())
        val attachmentOptions: StateFlow<List<AttachmentOption>> = _attachmentOptions.asStateFlow()

        private val _isAddMissionDialogOpen = MutableStateFlow(false)
        val isAddMissionDialogOpen: StateFlow<Boolean> = _isAddMissionDialogOpen.asStateFlow()

        private val _boardLinkedProjectIds = MutableStateFlow<List<String>>(emptyList())
        val boardLinkedProjectIds: StateFlow<List<String>> = _boardLinkedProjectIds.asStateFlow()

        private val _boardLinkedAttachmentIds = MutableStateFlow<List<String>>(emptyList())
        val boardLinkedAttachmentIds: StateFlow<List<String>> = _boardLinkedAttachmentIds.asStateFlow()

        init {
            loadMissions()

            // Завантаження доступних проектів для вибору
            contextRepository.getAllContextsFlow()
                .onEach { projects ->
                    _projectOptions.value = projects.map { ProjectOption(it.id, it.name) }
                }
                .launchIn(viewModelScope)

            // Завантаження вкладень (нотатки, чеклисти, лінки) для прив'язки до місії
            // Якщо приходить Flow<List<AttachmentLibraryQueryResult>>
            attachmentsRepository.getAttachmentLibraryItems()
                .onEach { results ->
                    _attachmentOptions.value = results.mapNotNull { it.toAttachmentOption() }
                    // Переконайтеся, що toAttachmentOption() визначено для AttachmentLibraryQueryResult
                }
                .launchIn(viewModelScope)

            settingsRepository.tacticalLinkedProjectIdsFlow
                .onEach { ids -> _boardLinkedProjectIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.tacticalLinkedAttachmentIdsFlow
                .onEach { ids -> _boardLinkedAttachmentIds.value = ids.toList() }
                .launchIn(viewModelScope)
        }

        private fun loadMissions(projectId: String? = null) {
            getTacticalMissionsUseCase(projectId)
                .onEach { missions ->
                    _missions.value = missions
                }
                .launchIn(viewModelScope)
        }

        fun addMission(
            title: String,
            description: String,
            deadline: Long,
            projectLinks: List<String>,
            attachmentLinks: List<String>,
        ) {
            val newMission =
                TacticalMission(
                    title = title,
                    description = description,
                    deadline = deadline,
                    projectId = null,
                    linkedProjectIds = projectLinks,
                    linkedAttachmentIds = attachmentLinks,
                )
            addMission(newMission)
        }

        fun addMission(mission: TacticalMission) {
            viewModelScope.launch {
                val id = addTacticalMissionUseCase(mission)
                // Прив'язуємо вкладення до створеної місії
                missionRepository.setAttachments(id, mission.linkedAttachmentIds ?: emptyList())
            }
        }

        fun updateMission(
            id: Long,
            title: String,
            description: String?,
            deadline: Long,
            projectLinks: List<String>,
            attachmentLinks: List<String>,
        ) {
            viewModelScope.launch {
                val existingMission = _missions.value.find { it.id == id }
                if (existingMission != null) {
                    val updatedMission =
                        existingMission.copy(
                            title = title,
                            description = description,
                            deadline = deadline,
                            linkedProjectIds = projectLinks,
                            linkedAttachmentIds = attachmentLinks,
                        )
                    updateTacticalMissionUseCase(updatedMission)
                    missionRepository.setAttachments(id, attachmentLinks)
                }
            }
        }

        fun updateMission(mission: TacticalMission) {
            viewModelScope.launch {
                updateTacticalMissionUseCase(mission)
                missionRepository.setAttachments(mission.id, mission.linkedAttachmentIds ?: emptyList())
            }
        }

        fun deleteMission(missionId: Long) {
            viewModelScope.launch {
                deleteTacticalMissionUseCase(missionId)
            }
        }

        fun toggleMissionCompleted(mission: TacticalMission) {
            val updatedStatus =
                if (mission.status == MissionStatus.COMPLETED) {
                    MissionStatus.PENDING
                } else {
                    MissionStatus.COMPLETED
                }
            val updatedMission = mission.copy(status = updatedStatus)
            viewModelScope.launch {
                updateTacticalMissionUseCase(updatedMission)
            }
        }

        fun openAddMissionDialog() {
            _isAddMissionDialogOpen.value = true
        }

        fun dismissAddMissionDialog() {
            _isAddMissionDialogOpen.value = false
        }

        fun addBoardProjectLink(projectId: String) {
            val updated = (_boardLinkedProjectIds.value + projectId).distinct().toSet()
            viewModelScope.launch { settingsRepository.setTacticalLinkedProjectIds(updated) }
        }

        fun removeBoardProjectLink(projectId: String) {
            val updated = _boardLinkedProjectIds.value.filterNot { it == projectId }.toSet()
            viewModelScope.launch { settingsRepository.setTacticalLinkedProjectIds(updated) }
        }

        fun addBoardAttachmentLink(attachmentId: String) {
            val updated = (_boardLinkedAttachmentIds.value + attachmentId).distinct().toSet()
            viewModelScope.launch { settingsRepository.setTacticalLinkedAttachmentIds(updated) }
        }

        fun removeBoardAttachmentLink(attachmentId: String) {
            val updated = _boardLinkedAttachmentIds.value.filterNot { it == attachmentId }.toSet()
            viewModelScope.launch { settingsRepository.setTacticalLinkedAttachmentIds(updated) }
        }
    }

data class ProjectOption(val id: String, val name: String)

data class AttachmentOption(val id: String, val name: String)

// Оновлене розширення для роботи з результатом запиту бібліотеки
private fun AttachmentLibraryQueryResult.toAttachmentOption(): AttachmentOption {
    val label =
        noteName
            ?: checklistName
            ?: contextName
            ?: linkDisplayName?.let { json ->
                try {
                    // Якщо це посилання, намагаємось дістати ім'я з JSON
                    com.google.gson.Gson().fromJson(json, RelatedLink::class.java).displayName
                } catch (e: Exception) {
                    null
                }
            }
            ?: "Attachment ${id.takeLast(4)}" // Fallback

    return AttachmentOption(id = id, name = label)
}
