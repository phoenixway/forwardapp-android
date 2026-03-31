@file:OptIn(ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.goalproperties

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.ContextSettingsEvent
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.ui.components.notesEditors.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreenActions
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreenState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabUiState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabActions
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabContent

@Composable
fun GoalSettingsScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: GoalSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var activePickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }

    GoalSettingsNavigationEffect(
        navController = navController,
        navigationManager = navigationManager,
        viewModel = viewModel,
    )
    GoalSettingsListChooserEffect(
        savedStateHandle = savedStateHandle,
        viewModel = viewModel,
    )
    GoalSettingsContent(
        navController = navController,
        uiState = uiState,
        viewModel = viewModel,
        onOpenPicker = { tab, createAction ->
            activePickerTab = tab
            pendingCreateAction = createAction
        },
    )
    GoalSettingsDescriptionEditor(
        uiState = uiState,
        viewModel = viewModel,
    )

    activePickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions = uiState.availableContexts,
            attachmentOptions = uiState.availableAttachments,
            preselectedContextIds = uiState.relatedLinks.filter { it.type == com.romankozak.forwardappmobile.core.data.models.entities.LinkType.CONTEXT }.mapTo(mutableSetOf()) { it.target },
            preselectedAttachmentIds = uiState.selectedAttachmentIds,
            initialTab = initialTab,
            initialCreateAction = pendingCreateAction,
            onDismiss = {
                activePickerTab = null
                pendingCreateAction = null
            },
            onContextSelected = { id ->
                viewModel.onListChooserResult(id)
                activePickerTab = null
                pendingCreateAction = null
            },
            onAttachmentSelected = { id ->
                viewModel.onAttachmentSelected(id)
                activePickerTab = null
                pendingCreateAction = null
            },
            onCreateRootContext = null,
            onCreateDocument = { draft -> viewModel.createAttachmentForPicker(draft) },
        )
    }
}

@Composable
private fun GoalSettingsNavigationEffect(
    navController: NavController,
    navigationManager: EnhancedNavigationManager?,
    viewModel: GoalSettingsViewModel,
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                is ContextSettingsEvent.NavigateBack -> navController.popBackStack()
                is ContextSettingsEvent.Navigate ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = it.target,
                    )
            }
        }
    }
}

@Composable
private fun GoalSettingsListChooserEffect(
    savedStateHandle: SavedStateHandle?,
    viewModel: GoalSettingsViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, savedStateHandle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val result = savedStateHandle?.get<String>("list_chooser_result")
                    if (result != null) {
                        viewModel.onListChooserResult(result)
                        savedStateHandle.remove<String>("list_chooser_result")
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun GoalSettingsContent(
    navController: NavController,
    uiState: GoalSettingsUiState,
    viewModel: GoalSettingsViewModel,
    onOpenPicker: (LinkPickerTab, PickerCreateAction?) -> Unit,
) {
    val tabs = goalSettingsTabs()
    SettingsScreen(
        state =
            SettingsScreenState(
                title = if (uiState.isNewGoal) "New Goal" else "Edit Goal",
                tabs = tabs,
                tabIcons = goalSettingsTabIcons(),
                selectedTabIndex = uiState.selectedTabIndex,
                isSaveEnabled = uiState.title.text.isNotBlank(),
            ),
        actions =
            SettingsScreenActions(
                onTabSelected = viewModel::onTabSelected,
                onSave = viewModel::onSave,
            ),
        navController = navController,
    ) {
        GoalSettingsTabContent(
            selectedTab = tabs[it],
            uiState = uiState,
            viewModel = viewModel,
            onOpenPicker = onOpenPicker,
        )
    }
}

@Composable
private fun GoalSettingsDescriptionEditor(
    uiState: GoalSettingsUiState,
    viewModel: GoalSettingsViewModel,
) {
    if (uiState.isDescriptionEditorOpen) {
        FullScreenMarkdownEditor(
            initialValue = uiState.description,
            onDismiss = { viewModel.closeDescriptionEditor() },
            onSave = { newText -> viewModel.onDescriptionChangeAndCloseEditor(newText) },
        )
    }
}

@Composable
private fun GoalSettingsTabContent(
    selectedTab: String,
    uiState: GoalSettingsUiState,
    viewModel: GoalSettingsViewModel,
    onOpenPicker: (LinkPickerTab, PickerCreateAction?) -> Unit,
) {
    when (selectedTab) {
        "General" ->
            GeneralTabContent(
                state =
                    GeneralTabState(
                        title = uiState.title,
                        description = uiState.description,
                    ),
                actions =
                    GeneralTabActions(
                        onTitleChange = viewModel::onTextChange,
                        onDescriptionChange = viewModel::onDescriptionChange,
                        onExpandDescriptionClick = viewModel::openDescriptionEditor,
                    ),
                titleLabel = "Назва цілі",
            )
        "Evaluation" ->
            EvaluationTabContent(
                uiState =
                    EvaluationTabUiState(
                        valueImportance = uiState.valueImportance,
                        valueImpact = uiState.valueImpact,
                        effort = uiState.effort,
                        cost = uiState.cost,
                        risk = uiState.risk,
                        weightEffort = uiState.weightEffort,
                        weightCost = uiState.weightCost,
                        weightRisk = uiState.weightRisk,
                        rawScore = uiState.rawScore,
                        scoringStatus = uiState.scoringStatus,
                        isScoringEnabled = uiState.isScoringEnabled,
                    ),
                onViewModelAction = viewModel,
            )
        "Reminders" ->
            RemindersTabContent(
                reminderTime = uiState.reminderTime,
                onViewModelAction = viewModel,
            )
        "Links" ->
            LinksTabContent(
                links = uiState.relatedLinks,
                onAddProjectLink = { onOpenPicker(LinkPickerTab.CONTEXTS, null) },
                onAddDocumentLink = { onOpenPicker(LinkPickerTab.ATTACHMENTS, null) },
                onAddWebLink = { onOpenPicker(LinkPickerTab.ATTACHMENTS, PickerCreateAction.WEB_LINK) },
                onAddObsidianLink = { onOpenPicker(LinkPickerTab.ATTACHMENTS, PickerCreateAction.OBSIDIAN) },
                onRemoveLink = viewModel::onRemoveLinkAssociation,
            )
    }
}

private fun goalSettingsTabs(): List<String> = listOf("General", "Evaluation", "Reminders", "Links")

private fun goalSettingsTabIcons() =
    listOf(
        Icons.Default.Settings,
        Icons.Default.BarChart,
        Icons.Default.Notifications,
        Icons.Default.Link,
    )
