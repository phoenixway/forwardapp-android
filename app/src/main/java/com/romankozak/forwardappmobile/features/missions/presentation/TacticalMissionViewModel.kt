package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.domain.usecase.AddTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.DeleteTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.GetTacticalMissionsUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.UpdateTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionStatus // Added import
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
    private val deleteTacticalMissionUseCase: DeleteTacticalMissionUseCase
) : ViewModel() {

    private val _missions = MutableStateFlow<List<TacticalMission>>(emptyList())
    val missions: StateFlow<List<TacticalMission>> = _missions.asStateFlow()

    init {
        loadMissions()
    }

    private fun loadMissions(projectId: String? = null) {
        getTacticalMissionsUseCase(projectId)
            .onEach { missions ->
                _missions.value = missions
            }
            .launchIn(viewModelScope)
    }

    fun addMission(mission: TacticalMission) {
        viewModelScope.launch {
            addTacticalMissionUseCase(mission)
        }
    }

    fun updateMission(mission: TacticalMission) {
        viewModelScope.launch {
            updateTacticalMissionUseCase(mission)
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
