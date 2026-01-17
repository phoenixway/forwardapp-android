package com.romankozak.forwardappmobile.features.attachments.specific_types.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.ScriptRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ScriptListItem(
    val id: String,
    val name: String,
    val description: String?,
    val projectName: String?,
    val updatedAt: Long,
)

data class ScriptsLibraryUiState(
    val isFeatureEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary),
    val query: String = "",
    val items: List<ScriptListItem> = emptyList(),
    val totalCount: Int = 0,
    val matchedCount: Int = 0,
    val filter: ScriptsFilter = ScriptsFilter.ALL,
)

enum class ScriptsFilter { ALL, WITHOUT_PROJECT }

@HiltViewModel
class ScriptsLibraryViewModel @Inject constructor(
    scriptRepository: ScriptRepository,
    projectRepository: ProjectRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ScriptsFilter.ALL)

    val uiState: StateFlow<ScriptsLibraryUiState> =
        combine(
            scriptRepository.getAllScripts(),
            projectRepository.getAllProjectsFlow(),
            query,
            filter,
            settingsRepository.featureTogglesFlow,
        ) { scripts, projects, queryText, filterValue, featureToggles ->
            FeatureToggles.updateAll(featureToggles)
            val normalizedQuery = queryText.trim().lowercase()
            val projectNames = projects.associate { it.id to it.name }
            val sortedScripts = scripts.sortedByDescending { it.updatedAt }
            val filtered =
                if (normalizedQuery.isBlank()) {
                    sortedScripts
                } else {
                    sortedScripts.filter { script ->
                        val matchesQuery =
                            script.name.contains(normalizedQuery, ignoreCase = true) ||
                                script.description?.contains(normalizedQuery, ignoreCase = true) == true ||
                                script.projectId?.let { id ->
                                    projectNames[id]?.contains(normalizedQuery, ignoreCase = true)
                                } == true
                        val matchesFilter =
                            when (filterValue) {
                                ScriptsFilter.ALL -> true
                                ScriptsFilter.WITHOUT_PROJECT -> script.projectId == null
                            }
                        matchesQuery && matchesFilter
                    }
                }
            ScriptsLibraryUiState(
                isFeatureEnabled = featureToggles[FeatureFlag.ScriptsLibrary] ?: FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary),
                query = queryText,
                filter = filterValue,
                items =
                    filtered.map { script ->
                        ScriptListItem(
                            id = script.id,
                            name = script.name,
                            description = script.description,
                            projectName = script.projectId?.let { projectNames[it] },
                            updatedAt = script.updatedAt,
                        )
                    },
                totalCount = scripts.size,
                matchedCount = filtered.size,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScriptsLibraryUiState(),
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: ScriptsFilter) {
        filter.value = value
    }
}
