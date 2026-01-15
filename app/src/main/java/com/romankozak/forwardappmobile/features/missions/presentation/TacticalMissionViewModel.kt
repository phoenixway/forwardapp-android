package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.domain.usecase.AddTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.DeleteTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.GetTacticalMissionsUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.UpdateTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionStatus // Added import
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentLibraryQueryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TacticalMissionViewModel @Inject constructor(
    private val getTacticalMissionsUseCase: GetTacticalMissionsUseCase,
    private val addTacticalMissionUseCase: AddTacticalMissionUseCase,
    private val updateTacticalMissionUseCase: UpdateTacticalMissionUseCase,
    private val deleteTacticalMissionUseCase: DeleteTacticalMissionUseCase,
    private val missionRepository: com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository,
    projectRepository: ProjectRepository,
    attachmentRepository: AttachmentRepository,
) : ViewModel() {

    private val _missions = MutableStateFlow<List<TacticalMission>>(emptyList())
    val missions: StateFlow<List<TacticalMission>> = _missions.asStateFlow()

    private val _projectOptions = MutableStateFlow<List<ProjectOption>>(emptyList())
    val projectOptions: StateFlow<List<ProjectOption>> = _projectOptions.asStateFlow()

    private val _attachmentOptions = MutableStateFlow<List<AttachmentOption>>(emptyList())
    val attachmentOptions: StateFlow<List<AttachmentOption>> = _attachmentOptions.asStateFlow()

    init {
        loadMissions()
        projectRepository.getAllProjectsFlow()
            .onEach { projects ->
                _projectOptions.value = projects.map { ProjectOption(it.id, it.name) }
            }
            .launchIn(viewModelScope)
        attachmentRepository.getAttachmentLibraryItems()
            .onEach { results ->
                _attachmentOptions.value = results.mapNotNull { it.toAttachmentOption() }
            }
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
        val newMission = TacticalMission(
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
                val updatedMission = existingMission.copy(
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
        val updatedStatus = if (mission.status == MissionStatus.COMPLETED) {
            MissionStatus.PENDING
        } else {
            MissionStatus.COMPLETED
        }
        val updatedMission = mission.copy(status = updatedStatus)
        updateMission(updatedMission)
    }
}

data class ProjectOption(val id: String, val name: String)

data class AttachmentOption(val id: String, val name: String)

private fun AttachmentLibraryQueryResult.toAttachmentOption(): AttachmentOption? {
    val id = this.id ?: return null
    val label = this.noteName
        ?: this.checklistName
        ?: this.linkDisplayName
        ?: this.linkTarget
        ?: "Attachment"
    return AttachmentOption(id, label)
}
