package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsRegistry
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabActions
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ContextSettingsViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val reminderRepository: ReminderRepository,
        private val savedStateHandle: SavedStateHandle,
        private val structurePresetDao: StructurePresetDao,
        private val contextStructureRepository: ContextStructureRepository,
        private val structurePresetService: StructurePresetService,
        private val capabilitySettingsRegistry: CapabilitySettingsRegistry,
    ) : ViewModel(), EvaluationTabActions, RemindersTabActions {
        private val projectId: String? = savedStateHandle["projectId"]

        private val _uiState = MutableStateFlow(ContextSettingsUiState())
        val uiState: StateFlow<ContextSettingsUiState> = _uiState.asStateFlow()

        private val _events = Channel<ContextSettingsEvent>()
        val events = _events.receiveAsFlow()

        fun getAvailableCapabilitySettingsTabs(enabledCapabilityIds: Set<CapabilityId>): List<CapabilitySettingsEntry> =
            capabilitySettingsRegistry.forCapabilities(enabledCapabilityIds)

        init {
            viewModelScope.launch {
                if (projectId != null) {
                    loadExistingProject(projectId)
                    reminderRepository.getRemindersForEntityFlow(projectId).collect { reminders ->
                        _uiState.update { it.copy(reminderTime = reminders.firstOrNull()?.reminderTime) }
                    }
                }
            }

            viewModelScope.launch {
                contextStructureRepository.ensureReservedBaseRolePresets()
                structurePresetDao.getAll().collect { presets ->
                    _uiState.update { it.copy(availablePresets = presets) }
                }
            }
        }

        /**
         * Завантаження існуючого проекту з повною синхронізацією конфігурації
         */
        private suspend fun loadExistingProject(projectId: String) {
            // 1. Отримуємо основні дані проекту та його структуру
            val project = contextRepository.getContextById(projectId)
            val structure = contextStructureRepository.getStructureByContext(projectId)

            if (project != null) {
                // 2. Резолвимо назву пресета (ролі) для відображення в UI
                val presetLabel =
                    structure?.basePresetCode?.let { code ->
                        structurePresetDao.getByCode(code)?.label
                    } ?: "Стандартний (Default)"

                // 3. Формуємо мапу фіч, використовуючи CapabilityGate для перевірки реального стану
                val allKnownCapabilities = ContextRoleRegistry.getAllKnownCapabilities()
                val structureFeatures = allKnownCapabilities.associate { capId ->
                    val key = capId.raw.replace("_", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    key to isEnabledForConfig(capId, structure)
                }

                // 4. Оновлюємо стан UI одним атомарним блоком
                _uiState.update { state ->
                    state.copy(
                        contextId = project.id,
                        // Метадані проекту
                        title = state.title.copy(project.name),
                        description = state.description.copy(project.description ?: ""),
                        tags = project.tags ?: emptyList(),
                        isReady = true,
                        isNewProject = false,
                        showCheckboxes = project.showCheckboxes,
                        // Скоринг та оцінка
                        valueImportance = project.valueImportance,
                        valueImpact = project.valueImpact,
                        effort = project.effort,
                        cost = project.cost,
                        risk = project.risk,
                        weightEffort = project.weightEffort,
                        weightCost = project.weightCost,
                        weightRisk = project.weightRisk,
                        rawScore = project.rawScore,
                        displayScore = project.displayScore,
                        scoringStatus = project.scoringStatus,
                        isScoringEnabled = project.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                        // Системна конфігурація
                        basePresetCode = structure?.basePresetCode,
                        enabledCapabilityIds = structureFeatures.filterValues { it }.keys.map(::featureLabelToCapabilityId).toSet(),
                        experimentalCapabilityIds = structure?.experimentalCapabilityIds ?: emptyList(),
                        currentPresetLabel = presetLabel,
                        features = structureFeatures,
                        // Синхронізація ключових прапорців для UI-логіки
                        autoLinkSubprojects = structureFeatures["Auto link subprojects"] ?: true,
                        isProjectManagementEnabled = structureFeatures["Advanced"] == true,
                    )
                }
            } else {
                // Якщо проект видалено або не знайдено — повертаємо користувача назад
                _events.send(ContextSettingsEvent.NavigateBack("Проект не знайдено"))
            }
        }

        /**
         * Перевірка стану можливості через конфігурацію (роль + експериментальні ID + legacy прапорці)
         */
        private fun isEnabledForConfig(
            id: CapabilityId,
            config: ContextConfiguration?,
        ): Boolean {
            if (id.raw == "dashboard") return true
            if (config == null) return id.raw != "advanced" // дефолтні налаштування для порожнього конфігу

            // Перевірка через роль
            val enabledByRole = ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode).contains(id)

            // Перевірка через legacy прапорці
            val isLegacy =
                when (id.raw) {
                    "inbox" -> config.enableInbox == true
                    "log" -> config.enableLog == true
                    "artifact" -> config.enableArtifact == true
                    "advanced" -> config.enableAdvanced == true
                    "dashboard" -> config.enableDashboard == true
                    "backlog" -> config.enableBacklog == true
                    "attachments" -> config.enableAttachments == true
                    "auto_link_subprojects" -> config.enableAutoLinkSubprojects == true
                    else -> false
                }

            // Перевірка через експериментальні ID або старі прапорці
            return enabledByRole || config.experimentalCapabilityIds.contains(id) || isLegacy
        }

        fun onSave() {
            viewModelScope.launch {
                if (_uiState.value.title.text.isBlank()) {
                    _events.send(ContextSettingsEvent.NavigateBack("Назва проекту не може бути пустою"))
                    return@launch
                }
                saveProject()
                _events.send(ContextSettingsEvent.NavigateBack("Збережено"))
            }
        }

        private suspend fun saveProject() {
            val projectId: String = savedStateHandle["projectId"] ?: return
            val project = contextRepository.getContextById(projectId) ?: return

            val updatedProject =
                project.copy(
                    name = _uiState.value.title.text,
                    description = _uiState.value.description.text.ifEmpty { null },
                    tags = _uiState.value.tags,
                    showCheckboxes = _uiState.value.showCheckboxes,
                    isContextManagementEnabled = _uiState.value.isProjectManagementEnabled,
                    valueImportance = _uiState.value.valueImportance,
                    valueImpact = _uiState.value.valueImpact,
                    effort = _uiState.value.effort,
                    cost = _uiState.value.cost,
                    risk = _uiState.value.risk,
                    weightEffort = _uiState.value.weightEffort,
                    weightCost = _uiState.value.weightCost,
                    weightRisk = _uiState.value.weightRisk,
                    rawScore = _uiState.value.rawScore,
                    displayScore = _uiState.value.displayScore,
                    scoringStatus = _uiState.value.scoringStatus,
                )
            contextRepository.updateContext(updatedProject)
            persistFeatureFlags()
        }

        fun onTextChange(newValue: TextFieldValue) = _uiState.update { it.copy(title = newValue) }

        fun onDescriptionChange(newValue: TextFieldValue) = _uiState.update { it.copy(description = newValue) }

        override fun onValueImportanceChange(value: Float) = _uiState.update { it.copy(valueImportance = value) }

        override fun onValueImpactChange(value: Float) = _uiState.update { it.copy(valueImpact = value) }

        override fun onEffortChange(value: Float) = _uiState.update { it.copy(effort = value) }

        override fun onCostChange(value: Float) = _uiState.update { it.copy(cost = value) }

        override fun onRiskChange(value: Float) = _uiState.update { it.copy(risk = value) }

        override fun onWeightEffortChange(value: Float) = _uiState.update { it.copy(weightEffort = value) }

        override fun onWeightCostChange(value: Float) = _uiState.update { it.copy(weightCost = value) }

        override fun onWeightRiskChange(value: Float) = _uiState.update { it.copy(weightRisk = value) }

        override fun onScoringStatusChange(newStatus: String) {
            _uiState.update {
                it.copy(
                    scoringStatus = newStatus,
                    isScoringEnabled = newStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                )
            }
        }

        fun openDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = true) }

        fun closeDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = false) }

        fun onDescriptionChangeAndCloseEditor(newDescription: String) {
            _uiState.update {
                it.copy(
                    description = it.description.copy(text = newDescription),
                    isDescriptionEditorOpen = false,
                )
            }
        }

        fun onTabSelected(index: Int) = _uiState.update { it.copy(selectedTabIndex = index) }

        fun onShowCheckboxesChange(show: Boolean) = _uiState.update { it.copy(showCheckboxes = show) }

        fun onAddTag(tag: String) = _uiState.update { it.copy(tags = it.tags + tag) }

        fun onRemoveTag(tag: String) = _uiState.update { it.copy(tags = it.tags - tag) }

        fun onProjectManagementChange(enabled: Boolean) {
            _uiState.update { it.copy(isProjectManagementEnabled = enabled) }
        }

        fun onAutoLinkSubprojectsChange(enabled: Boolean) {
            _uiState.update {
                it.copy(
                    autoLinkSubprojects = enabled,
                    features = it.features + ("Auto link subprojects" to enabled),
                )
            }
        }

        fun onApplyPreset(code: String) {
            val pid = projectId ?: return
            viewModelScope.launch {
                // 1. Застосовуємо пресет (це оновить basePresetCode та, можливо, структуру)
                structurePresetService.applyPresetToContext(pid, code)

                // 2. Отримуємо можливості, що відповідають цьому пресету
                val presetCapabilities = ContextRoleRegistry.getCapabilitiesForRole(code)

                // 3. Розділяємо можливості на legacy та експериментальні
                val allKnownLegacyCaps =
                    setOf(
                        "inbox", "log", "artifact", "advanced", "dashboard", "backlog", "attachments",
                        "auto_link_subprojects",
                    )
                val experimentalIdsFromPreset = presetCapabilities.filter { it.raw !in allKnownLegacyCaps }

                // 4. Оновлюємо конфігурацію в БД, щоб прапорці відповідали пресету
                val structure = contextStructureRepository.ensureStructure(pid)
                val updatedStructure =
                    structure.copy(
                        basePresetCode = code,
                        // Оновлення legacy-прапорців
                        enableInbox = presetCapabilities.contains(CapabilityId("inbox")),
                        enableLog = presetCapabilities.contains(CapabilityId("log")),
                        enableArtifact = presetCapabilities.contains(CapabilityId("artifact")),
                        enableAdvanced = presetCapabilities.contains(CapabilityId("advanced")),
                        enableDashboard = true,
                        enableBacklog = presetCapabilities.contains(CapabilityId("backlog")),
                        enableAttachments = presetCapabilities.contains(CapabilityId("attachments")),
                        enableAutoLinkSubprojects = presetCapabilities.contains(CapabilityId("auto_link_subprojects")),
                        // Оновлення списку експериментальних ID
                        experimentalCapabilityIds = experimentalIdsFromPreset,
                    )
                contextStructureRepository.updateStructure(updatedStructure)

                // 5. Перезавантажуємо дані, щоб UI оновився згідно зі змінами
                loadExistingProject(pid)
            }
        }

        /**
         * Перемикання стану окремої можливості (фічі)
         */
        fun onToggleFeature(
            key: String,
            enabled: Boolean,
        ) {
            if (key == "Dashboard") return

            // 1. Мапимо текстовий ключ UI на системний CapabilityId
            val capabilityId = featureLabelToCapabilityId(key)

            _uiState.update { state ->
                // 2. Оновлюємо список експериментальних можливостей для збереження в БД
                val updatedExperimentalIds =
                    state.experimentalCapabilityIds.toMutableList().apply {
                        if (enabled) {
                            if (!contains(capabilityId)) add(capabilityId)
                        } else {
                            remove(capabilityId)
                        }
                    }

                // 3. Копіюємо стан із синхронізацією всіх залежних полів
                state.copy(
                    // Оновлюємо мапу для UI списку
                    features = state.features + (key to enabled),
                    enabledCapabilityIds =
                        state.enabledCapabilityIds.toMutableSet().apply {
                            if (enabled) add(capabilityId) else remove(capabilityId)
                        },
                    // Оновлюємо список для майбутнього persistFeatureFlags()
                    experimentalCapabilityIds = updatedExperimentalIds,
                    // Синхронізуємо спеціальні прапорці стану
                    isProjectManagementEnabled =
                        if (capabilityId.raw == "advanced") {
                            enabled
                        } else {
                            state.isProjectManagementEnabled
                        },
                    autoLinkSubprojects =
                        if (capabilityId.raw == "auto_link_subprojects") {
                            enabled
                        } else {
                            state.autoLinkSubprojects
                        },
                )
            }
        }

        private fun featureLabelToCapabilityId(label: String): CapabilityId =
            when (label) {
                "Inbox" -> CapabilityId("inbox")
                "Log" -> CapabilityId("log")
                "Artifact" -> CapabilityId("artifact")
                "Advanced" -> CapabilityId("advanced")
                "Dashboard" -> CapabilityId("dashboard")
                "Backlog" -> CapabilityId("backlog")
                "Attachments" -> CapabilityId("attachments")
                "Auto link subprojects" -> CapabilityId("auto_link_subprojects")
                else -> CapabilityId(label.lowercase().replace(" ", "_"))
            }

        /**
         * Збереження стану можливостей у БД
         */
        private suspend fun persistFeatureFlags() {
            val pid = projectId ?: return
            val currentState = _uiState.value

            // 1. Отримуємо існуючу структуру або створюємо нову
            val structure = contextStructureRepository.ensureStructure(pid)

            // 2. Створюємо оновлений об'єкт структури
            val updated =
                structure.copy(
                    // Зберігаємо код обраної ролі (пресета)
                    basePresetCode = currentState.basePresetCode,
                    // Зберігаємо список активованих ідентифікаторів можливостей
                    experimentalCapabilityIds = currentState.experimentalCapabilityIds,
                    // Підтримка legacy-колонок (для сумісності)
                    enableInbox = currentState.features["Inbox"] == true,
                    enableLog = currentState.features["Log"] == true,
                    enableArtifact = currentState.features["Artifact"] == true,
                    enableAdvanced = currentState.features["Advanced"] == true,
                    enableDashboard = currentState.features["Dashboard"] == true,
                    enableBacklog = currentState.features["Backlog"] == true,
                    enableAttachments = currentState.features["Attachments"] == true,
                    enableAutoLinkSubprojects = currentState.features["Auto link subprojects"] == true,
                    updatedAt = System.currentTimeMillis(),
                )

            // 3. Записуємо в БД
            contextStructureRepository.updateStructure(updated)

            // 4. Оновлюємо внутрішній стан UI для миттєвої реакції екрана
            _uiState.update { state ->
                state.copy(
                    isProjectManagementEnabled = updated.enableAdvanced == true,
                    autoLinkSubprojects = updated.enableAutoLinkSubprojects == true,
                )
            }
        }

        override fun onSetReminder(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
        ) {
            val calendar =
                Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                }
            val newReminderTime = calendar.timeInMillis
            _uiState.update { it.copy(reminderTime = newReminderTime) }

            projectId?.let { id ->
                viewModelScope.launch {
                    reminderRepository.createReminder(id, "PROJECT", newReminderTime)
                }
            }
        }

        override fun onClearReminder() {
            _uiState.update { it.copy(reminderTime = null) }
            projectId?.let { id ->
                viewModelScope.launch {
                    reminderRepository.clearRemindersForEntity(id)
                }
            }
        }

        fun onOpenStructure() {
            projectId?.let { id ->
                viewModelScope.launch {
                    _events.send(ContextSettingsEvent.Navigate(NavTarget.ContextStructure(id)))
                }
            }
        }
    }
