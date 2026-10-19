package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsRegistry
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.data.workspace.WorkspacePresentationPreferencesRepository
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabActions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun workspaceSettingsIdentity(saved: SavedStateHandle): String =
    if (saved.contains("workspaceId")) saved.get<String>("workspaceId").orEmpty()
    else saved.get<String>("projectId").orEmpty() // Old route is admitted by canonical Workspace reads, not Context.

internal data class WorkspaceFeature(val key: String, val type: String, val title: String)
internal val workspaceFeatures = listOf(
    WorkspaceFeature("dashboard", "DASHBOARD", "Dashboard"), WorkspaceFeature("backlog", "BACKLOG", "Backlog"),
    WorkspaceFeature("inbox", "INBOX", "Inbox"), WorkspaceFeature("direction", "DIRECTION", "Direction"),
    WorkspaceFeature("connections", "CONNECTIONS", "Connections"), WorkspaceFeature("log", "EXECUTION_LOG", "Log"),
    WorkspaceFeature("inbox_sorting", "INBOX_SORTING", "Inbox sorting"), WorkspaceFeature("key_problems", "KEY_PROBLEMS", "Issues"),
)

data class WorkspaceOperationalSettingsState(
    val ready: Boolean = false,
    val available: Boolean = false,
    val enabled: Set<CapabilityId> = emptySet(),
    val showCheckboxes: Boolean = false,
    val reminderTime: Long? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val tags: List<String>? = null,
)

/** Screen orchestration only. Each action delegates to its independent existing authority. */
@HiltViewModel
class WorkspaceOperationalSettingsViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val workspaceDao: WorkspaceDao,
    orientationDao: OrientationDao,
    private val preferences: WorkspacePresentationPreferencesRepository,
    private val capabilities: CanonicalOrdinaryCapabilitySettings,
    private val reminders: ReminderRepository,
    private val registry: CapabilitySettingsRegistry,
    private val tagsRepository: CanonicalWorkspaceTagRepository,
    systemTags: SystemWorkspaceTagAuthority,
) : ViewModel(), RemindersTabActions {
    private val workspaceId = workspaceSettingsIdentity(saved)
    private val mutable = MutableStateFlow(WorkspaceOperationalSettingsState())
    val state = mutable.asStateFlow()
    val selectedTab = saved.getStateFlow("workspacePropertiesTab", "general")
    fun selectTab(key: String) { saved["workspacePropertiesTab"] = key }
    fun capabilityTabs() = registry.forCapabilities(state.value.enabled)

    init {
        if (workspaceId.isBlank()) mutable.value = state.value.copy(ready = true, error = "Workspace недоступний")
        else viewModelScope.launch {
            val tags = if (SystemContexts.isSystem(ContextId(workspaceId))) {
                systemTags.observeEffectiveOwners().map { owners -> owners.firstOrNull { it.id == workspaceId }?.tags }
            } else tagsRepository.observeLiveTagsByWorkspace().map { it[workspaceId].orEmpty() }
            combine(workspaceDao.observeById(workspaceId), orientationDao.observeWorkspaceCapabilities(workspaceId),
                preferences.observeCanonicalOrNull(workspaceId), reminders.getRemindersForEntityFlow(workspaceId), tags) { workspace, rows, prefs, alarms, tagList ->
                val enabledTypes = rows.filter { !it.isDeleted && it.state == "ACTIVE" }.mapTo(hashSetOf()) { it.capabilityType }
                state.value.copy(ready = true, available = workspace?.isDeleted == false,
                    enabled = workspaceFeatures.filter { it.type in enabledTypes }.mapTo(hashSetOf()) { CapabilityId(it.key) },
                    showCheckboxes = prefs?.showCheckboxes ?: false,
                    reminderTime = alarms.firstOrNull { it.entityType == "PROJECT" }?.reminderTime, tags = tagList)
            }.catch { error ->
                if (error is CancellationException) throw error
                mutable.value = state.value.copy(ready = true, available = false, error = "Не вдалося завантажити налаштування")
            }.collect { mutable.value = it }
        }
    }

    fun setCheckboxes(value: Boolean) = mutate { preferences.setShowCheckboxes(workspaceId, value) }
    fun addTag(value: String) {
        if (state.value.tags == null) return
        mutate { tagsRepository.replaceTags(workspaceId, tagsRepository.getTags(workspaceId) + value) }
    }
    fun removeTag(value: String) {
        if (state.value.tags == null) return
        mutate { tagsRepository.replaceTags(workspaceId, tagsRepository.getTags(workspaceId) - value) }
    }
    fun setCapability(key: String, enabled: Boolean) = mutate {
        require(workspaceFeatures.any { it.key == key }) { "Непідтримувана можливість" }
        capabilities.setEnabled(workspaceId, CapabilityId(key), enabled)
    }

    override fun onSetReminder(year: Int, month: Int, day: Int, hour: Int, minute: Int) = mutate {
        val time = Calendar.getInstance().apply { set(year, month, day, hour, minute, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        reminders.createReminder(workspaceId, "PROJECT", time) // Existing reminder transport discriminator; identity is Workspace.
    }
    override fun onClearReminder() = mutate { reminders.clearRemindersForEntityType(workspaceId, "PROJECT") }

    private fun mutate(command: suspend () -> Unit) {
        if (!state.value.ready || !state.value.available || state.value.busy) return
        mutable.value = state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                require(workspaceDao.getById(workspaceId)?.isDeleted == false) { "Workspace недоступний" }
                command()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { mutable.value = state.value.copy(error = error.message ?: "Не вдалося зберегти")
            } finally { mutable.value = state.value.copy(busy = false) }
        }
    }
}
