package com.romankozak.forwardappmobile.features.missions.presentation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tacticsWorkspaceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tactics_workspace_state",
)

data class TacticsWorkspaceState(
    val selectedMode: TacticsWorkspaceMode = TacticsWorkspaceMode.STREAMS,
    val selectedMissionStreamId: String = GENERAL_MISSION_STREAM_ID,
    val selectedPlanningContextId: String? = null,
    val recentMissionStreamIds: List<String> = listOf(GENERAL_MISSION_STREAM_ID),
    val iterationDurationDays: Int? = null,
)

@Singleton
class TacticsWorkspaceStateRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val RECENT_STREAM_LIMIT = 30
            private const val LIST_SEPARATOR = "\n"
            private val selectedModeKey = stringPreferencesKey("selected_mode")
            private val selectedMissionStreamIdKey = stringPreferencesKey("selected_mission_stream_id")
            private val selectedPlanningContextIdKey = stringPreferencesKey("selected_planning_context_id")
            private val recentMissionStreamIdsKey = stringPreferencesKey("recent_mission_stream_ids")
            private val iterationDurationDaysKey = intPreferencesKey("iteration_duration_days")
        }

        val state: Flow<TacticsWorkspaceState> =
            context.tacticsWorkspaceDataStore.data.map { prefs ->
                TacticsWorkspaceState(
                    selectedMode = prefs[selectedModeKey].toWorkspaceMode(),
                    selectedMissionStreamId = prefs[selectedMissionStreamIdKey] ?: GENERAL_MISSION_STREAM_ID,
                    selectedPlanningContextId = prefs[selectedPlanningContextIdKey]?.ifBlank { null },
                    recentMissionStreamIds =
                        prefs[recentMissionStreamIdsKey]
                            .toIdList()
                            .ifEmpty { listOf(GENERAL_MISSION_STREAM_ID) },
                    iterationDurationDays = prefs[iterationDurationDaysKey]?.takeIf { it > 0 },
                )
            }

        suspend fun setSelectedMode(mode: TacticsWorkspaceMode) {
            context.tacticsWorkspaceDataStore.edit { prefs ->
                prefs[selectedModeKey] = mode.name
            }
        }

        suspend fun setSelectedMissionStream(streamId: String) {
            context.tacticsWorkspaceDataStore.edit { prefs ->
                prefs[selectedMissionStreamIdKey] = streamId
                prefs[recentMissionStreamIdsKey] =
                    prefs[recentMissionStreamIdsKey]
                        .toIdList()
                        .withRecentFirst(streamId)
                        .joinToString(LIST_SEPARATOR)
            }
        }

        suspend fun setSelectedPlanningContext(contextId: String?) {
            context.tacticsWorkspaceDataStore.edit { prefs ->
                if (contextId == null) {
                    prefs.remove(selectedPlanningContextIdKey)
                } else {
                    prefs[selectedPlanningContextIdKey] = contextId
                }
            }
        }

        suspend fun setIterationDurationDays(days: Int?) {
            context.tacticsWorkspaceDataStore.edit { prefs ->
                if (days == null || days <= 0) {
                    prefs.remove(iterationDurationDaysKey)
                } else {
                    prefs[iterationDurationDaysKey] = days
                }
            }
        }

        private fun String?.toWorkspaceMode(): TacticsWorkspaceMode =
            this
                ?.let { stored -> TacticsWorkspaceMode.entries.firstOrNull { it.name == stored } }
                ?: TacticsWorkspaceMode.STREAMS

        private fun String?.toIdList(): List<String> =
            this
                ?.split(LIST_SEPARATOR)
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.distinct()
                .orEmpty()

        private fun List<String>.withRecentFirst(streamId: String): List<String> =
            (listOf(streamId) + filterNot { it == streamId })
                .take(RECENT_STREAM_LIMIT)
    }
