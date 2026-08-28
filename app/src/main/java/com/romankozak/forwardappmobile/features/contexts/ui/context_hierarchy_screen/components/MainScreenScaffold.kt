package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavigationHistoryMenu
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ProjectHierarchyScreenContent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.SearchProjectHierarchyBottomBar
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier

private const val UI_TAG = "ProjectHierarchyScreenUI_DEBUG"

private data class HierarchyAddAction(
    val item: HoldMenuItem,
    val action: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyScreenScaffold(
    uiState: ProjectHierarchyScreenUiState,
    focusedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    enhancedNavigationManager: EnhancedNavigationManager,
    lastOngoingActivity: ActivityRecord?,
    viewModel: ContextHierarchyScreenViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCloseScreen: () -> Unit,
    onEditBeacon: (String) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
    onAddMainBeacon: () -> Unit = {},
    onAddMainBeaconGroup: () -> Unit = {},
) {
    val holdMenuController = rememberHoldMenu2()
    val listState = rememberLazyListState()
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val backHandlerEnabled by remember(uiState.subStateStack, uiState.currentBreadcrumbs) {
        derivedStateOf {
            val enabled =
                uiState.subStateStack.size > 1 ||
                    uiState.currentBreadcrumbs.isNotEmpty()
            Log.d(UI_TAG, "BackHandler enabled = $enabled")
            enabled
        }
    }

    BackHandler(enabled = backHandlerEnabled) {
        Log.i(UI_TAG, "Custom BackHandler INVOKED")
        onEvent(ContextHierarchyScreenEvent.BackClick)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            val focusedOrientationNode =
                (uiState.currentSubState as? ProjectHierarchyScreenSubState.OrientationFocused)
                    ?.nodeId
                    ?.let { focusedId ->
                        uiState.orientationHierarchy
                            .firstOrNull { it.node.id == focusedId }
                            ?.node
                    }
            val focusedProject =
                when (focusedOrientationNode) {
                    is OrientationHierarchyNode.ContextNode -> focusedOrientationNode.context
                    else ->
                        (uiState.currentSubState as? ProjectHierarchyScreenSubState.ProjectFocused)
                            ?.projectId
                            ?.let { focusedId -> uiState.projectHierarchy.allProjects.find { it.id == focusedId } }
                }
            val focusedBeaconNode =
                focusedOrientationNode as? OrientationHierarchyNode.Beacon
            val focusedGroupNode =
                focusedOrientationNode as? OrientationHierarchyNode.Group
            val focusedNoBeacon =
                focusedOrientationNode === OrientationHierarchyNode.NoBeacon
            val canPasteContextIntoNoBeacon =
                focusedNoBeacon &&
                    uiState.clipboardContextIds.isNotEmpty() &&
                    uiState.clipboardOperation == ContextClipboardOperationUi.CUT
            val canPasteToFocusedNode =
                (
                    uiState.clipboardContextIds.isNotEmpty() &&
                        (focusedProject != null || focusedBeaconNode != null)
                ) ||
                    canPasteContextIntoNoBeacon ||
                    (
                        uiState.hasBeaconClipboard &&
                            (focusedBeaconNode != null || focusedGroupNode != null)
                    )
            ProjectHierarchyScreenTopAppBar(
                onBackClick = onCloseScreen,
                showHierarchyBack = uiState.isSelectionMode || backHandlerEnabled,
                onHierarchyBackClick = {
                    if (uiState.isSelectionMode) {
                        onEvent(ContextHierarchyScreenEvent.ClearContextSelection)
                    } else {
                        onEvent(ContextHierarchyScreenEvent.BackClick)
                    }
                },
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedContextIds.size,
                canPasteToFocusedContext = canPasteToFocusedNode,
                onCopySelection = { onEvent(ContextHierarchyScreenEvent.CopySelectedContexts) },
                onCutSelection = { onEvent(ContextHierarchyScreenEvent.CutSelectedContexts) },
                onPasteToFocusedContext = {
                    when {
                        uiState.clipboardContextIds.isNotEmpty() && focusedProject != null ->
                            onEvent(ContextHierarchyScreenEvent.PasteContextLink(focusedProject))
                        uiState.clipboardContextIds.isNotEmpty() && focusedBeaconNode != null ->
                            onEvent(ContextHierarchyScreenEvent.PasteContextLinksIntoBeacon(focusedBeaconNode.id))
                        canPasteContextIntoNoBeacon ->
                            onEvent(ContextHierarchyScreenEvent.PasteContextLinksIntoNoBeacon)
                        uiState.hasBeaconClipboard && focusedBeaconNode != null ->
                            onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoBeacon(focusedBeaconNode.id))
                        uiState.hasBeaconClipboard && focusedGroupNode != null ->
                            onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(focusedGroupNode.id))
                    }
                },
                isSiblingReorderMode = uiState.isSiblingReorderMode,
                onToggleSiblingReorderMode = {
                    onEvent(ContextHierarchyScreenEvent.ToggleSiblingReorderMode)
                },
                onSearchClick = {
                    onEvent(ContextHierarchyScreenEvent.SearchQueryChanged(TextFieldValue("")))
                },
            )
        },
        bottomBar = {
            val isSearchActive = uiState.subStateStack.any { it is ProjectHierarchyScreenSubState.LocalSearch }
            if (isSearchActive) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isSearchActive) 8.dp else 16.dp,
                                vertical = if (isSearchActive) 8.dp else 12.dp,
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .then(CommandDeckBackgroundModifier())
                            .padding(
                                horizontal = if (isSearchActive) 4.dp else 22.dp,
                                vertical = if (isSearchActive) 4.dp else 12.dp,
                            ),
                ) {
                    SearchProjectHierarchyBottomBar(
                        searchQuery = uiState.searchQuery,
                        onQueryChange = { onEvent(ContextHierarchyScreenEvent.SearchQueryChanged(it)) },
                        onCloseSearch = {
                            onEvent(ContextHierarchyScreenEvent.CloseSearch)
                        },
                        onPerformGlobalSearch = { onEvent(ContextHierarchyScreenEvent.GlobalSearchPerform(it)) },
                        onShowSearchHistory = { showSearchHistorySheet = true },
                    )
                }
            }
        },
        floatingActionButton = {
            val isSearchActiveFab = uiState.subStateStack.any { it is ProjectHierarchyScreenSubState.LocalSearch }
            var showAddMenu by remember { mutableStateOf(false) }

            AnimatedVisibility(visible = !isSearchActiveFab && !uiState.isSelectionMode) {
                val scriptsEnabled = uiState.featureToggles[FeatureFlag.ScriptsLibrary] == true
                val addActions =
                    buildList {
                        add(
                            HierarchyAddAction(
                                item =
                                    HoldMenuItem(
                                        label = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_project),
                                        icon = Icons.Default.FolderOpen,
                                    ),
                                action = { onEvent(ContextHierarchyScreenEvent.AddNewContextRequest) },
                            ),
                        )
                        add(
                            HierarchyAddAction(
                                item =
                                    HoldMenuItem(
                                        label = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_note),
                                        icon = Icons.Default.Description,
                                    ),
                                action = { onEvent(ContextHierarchyScreenEvent.AddNoteDocumentRequest) },
                            ),
                        )
                        add(
                            HierarchyAddAction(
                                item =
                                    HoldMenuItem(
                                        label = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_checklist),
                                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    ),
                                action = { onEvent(ContextHierarchyScreenEvent.AddChecklistRequest) },
                            ),
                        )
                        if (scriptsEnabled) {
                            add(
                                HierarchyAddAction(
                                    item =
                                        HoldMenuItem(
                                            label = "Скрипт",
                                            icon = Icons.Default.Code,
                                        ),
                                    action = { onEvent(ContextHierarchyScreenEvent.AddScriptRequest) },
                                ),
                            )
                        }
                        add(
                            HierarchyAddAction(
                                item =
                                    HoldMenuItem(
                                        label = "Головний орієнтир",
                                        icon = Icons.Default.FilterCenterFocus,
                                    ),
                                action = onAddMainBeacon,
                            ),
                        )
                        add(
                            HierarchyAddAction(
                                item =
                                    HoldMenuItem(
                                        label = "Група орієнтирів",
                                        icon = Icons.Default.FolderOpen,
                                    ),
                                action = onAddMainBeaconGroup,
                            ),
                        )
                    }
                HoldMenu2Button(
                    items = addActions.map { it.item },
                    controller = holdMenuController,
                    onSelect = { index ->
                        addActions.getOrNull(index)?.action?.invoke()
                    },
                    onTap = { showAddMenu = !showAddMenu },
                    menuAlignment = com.romankozak.forwardappmobile.features.common.components.holdmenu2.MenuAlignment.END,
                    iconPosition = com.romankozak.forwardappmobile.features.common.components.holdmenu2.IconPosition.END,
                ) {
                    Box {
                        FloatingActionButton(onClick = { showAddMenu = !showAddMenu }) {
                            Icon(Icons.Default.Add, contentDescription = "Додати")
                        }
                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                        ) {
                            addActions.forEach { addAction ->
                                DropdownMenuItem(
                                    leadingIcon =
                                        addAction.item.icon?.let { icon ->
                                            { Icon(icon, contentDescription = null) }
                                        },
                                    text = { Text(text = addAction.item.label) },
                                    onClick = {
                                        showAddMenu = false
                                        addAction.action()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        ProjectHierarchyScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onEvent = onEvent,
            listState = listState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onEditBeacon = onEditBeacon,
            onDeleteBeacon = onDeleteBeacon,
        )
    }

    if (uiState.showNavigationMenu) {
        NavigationHistoryMenu(
            navManager = enhancedNavigationManager,
            onDismiss = { onEvent(ContextHierarchyScreenEvent.HideHistory) },
        )
    }

    SearchHistoryBottomSheet(
        showSheet = showSearchHistorySheet,
        onDismiss = { showSearchHistorySheet = false },
        searchHistory = uiState.searchHistory,
        onHistoryClick = {
            onEvent(ContextHierarchyScreenEvent.SearchFromHistory(it))
            showSearchHistorySheet = false
        },
        onRemoveHistoryEntry = { onEvent(ContextHierarchyScreenEvent.RemoveSearchHistoryEntry(it)) },
        onClearAllHistory = { onEvent(ContextHierarchyScreenEvent.ClearSearchHistory) },
    )

    NewRecentListsSheet(
        showSheet = uiState.showRecentListsSheet,
        recentItems = uiState.recentItems,
        onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissRecentLists) },
        onItemClick = { onEvent(ContextHierarchyScreenEvent.RecentItemSelected(it)) },
        onPinClick = { onEvent(ContextHierarchyScreenEvent.RecentItemPinClick(it)) },
    )

    HandleProjectHierarchyDialogs(
        uiState = uiState,
        focusedContextIds = focusedContextIds,
        onEvent = onEvent,
    )

    uiState.recordForReminderDialog?.let { record ->
        ReminderPropertiesDialog(
            onDismiss = { viewModel.onReminderDialogDismiss() },
            onSetReminder = { timestamp -> viewModel.onSetReminder(timestamp) },
            onRemoveReminder =
                if (record.reminderTime != null) {
                    { _: String -> viewModel.onClearReminder() }
                } else {
                    null
                },
            currentReminders =
                listOfNotNull(record.reminderTime).map {
                    Reminder(
                        entityId = record.id,
                        entityType = "TASK",
                        reminderTime = it,
                        status = "SCHEDULED",
                        creationTime = System.currentTimeMillis(),
                    )
                },
        )
    }

    HoldMenu2Overlay(controller = holdMenuController)
}
