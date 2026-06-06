package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ExpandingProjectHierarchyBottomNav
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ModernBottomNavButton
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.SearchResultsView
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.rememberHierarchyFocusMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy.BreadcrumbNavigation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy.ProjectHierarchyView
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectHierarchyScreenContent(
    modifier: Modifier = Modifier,
    uiState: ProjectHierarchyScreenUiState,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    listState: LazyListState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onEditBeacon: (String) -> Unit = {},
) {
    val currentSubState =
        remember(uiState.subStateStack) {
            uiState.currentSubState
        }

    val isSearchActive =
        remember(currentSubState) {
            currentSubState is ProjectHierarchyScreenSubState.LocalSearch
        }

    val searchQuery =
        remember(uiState.searchQuery) {
            uiState.searchQuery.text
        }

    val isFocusMode =
        rememberHierarchyFocusMode(
            breadcrumbs = uiState.currentBreadcrumbs,
            hasFocusedProject =
                currentSubState is ProjectHierarchyScreenSubState.ProjectFocused ||
                    currentSubState is ProjectHierarchyScreenSubState.OrientationFocused,
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                ),
    ) {
        if (isSearchActive) {
            when {
                searchQuery.isBlank() -> {
                    SearchStartState(
                        history = uiState.searchHistory,
                        onSuggestionClick = {
                            onEvent(ContextHierarchyScreenEvent.SearchFromHistory(it))
                        },
                    )
                }
                !uiState.isReadyForFiltering -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    SearchResultsView(
                        results = uiState.searchResults,
                        query = searchQuery,
                        selectedFilter = uiState.searchResultFilter,
                        selectedSort = uiState.searchResultSort,
                        onFilterChange = { onEvent(ContextHierarchyScreenEvent.SearchFilterChanged(it)) },
                        onSortChange = { onEvent(ContextHierarchyScreenEvent.SearchSortChanged(it)) },
                        onRevealClick = { onEvent(ContextHierarchyScreenEvent.SearchResultClick(it)) },
                        onOpenClick = { onEvent(ContextHierarchyScreenEvent.ContextClick(it)) },
                        onPerformGlobalSearch = { onEvent(ContextHierarchyScreenEvent.GlobalSearchPerform(it)) },
                    )
                }
            }
        } else {
            val showBreadcrumbs =
                remember(uiState.currentBreadcrumbs) {
                    uiState.currentBreadcrumbs.isNotEmpty()
                }

            AnimatedVisibility(
                visible = showBreadcrumbs && !isFocusMode,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(150)),
            ) {
                BreadcrumbNavigation(
                    breadcrumbs = uiState.currentBreadcrumbs,
                    onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { projectId ->
                        uiState.projectHierarchy.allProjects.find { it.id == projectId }
                            ?.let { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) }
                    },
                )
            }

            val isListEmpty =
                remember(uiState.projectHierarchy) {
                    uiState.projectHierarchy.topLevelProjects.isEmpty() &&
                        uiState.projectHierarchy.childMap.isEmpty()
                }

            if (!uiState.isReadyForFiltering && isListEmpty) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (isListEmpty) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp,
                    ) {
                        Text(
                            text = "Create your first context",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }
            } else {
                ProjectHierarchyView(
                    modifier = Modifier.weight(1f),
                    hierarchy = uiState.projectHierarchy,
                    flattenedHierarchy = uiState.flattenedHierarchy,
                    orientationHierarchy = uiState.orientationHierarchy,
                    breadcrumbs = uiState.currentBreadcrumbs,
                    focusedProjectId =
                        if (isFocusMode) {
                            when (currentSubState) {
                                is ProjectHierarchyScreenSubState.ProjectFocused -> currentSubState.projectId
                                else -> null
                            }
                        } else {
                            null
                        },
                    focusedOrientationNodeId =
                        if (isFocusMode) {
                            when (currentSubState) {
                                is ProjectHierarchyScreenSubState.OrientationFocused -> currentSubState.nodeId
                                else -> null
                            }
                        } else {
                            null
                        },
                    highlightedProjectId = null,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    hierarchySettings = HierarchyDisplaySettings(),
                    listState = listState,
                    longDescendantsMap = uiState.longDescendantsMap,
                    selectedContextIds = uiState.selectedContextIds,
                    clipboardContextIds = uiState.clipboardContextIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onEvent = onEvent,
                    onEditBeacon = onEditBeacon,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onProjectClicked = { onEvent(ContextHierarchyScreenEvent.ContextClick(it)) },
                    onToggleSelection = { onEvent(ContextHierarchyScreenEvent.ToggleContextSelection(it)) },
                    onStartSelection = { onEvent(ContextHierarchyScreenEvent.StartContextSelection(it)) },
                    onMenuRequested = { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) },
                    onProjectReorder = { from, to, pos ->
                        onEvent(ContextHierarchyScreenEvent.ContextReorder(from, to, pos))
                    },
                    onFocusProject = { onEvent(ContextHierarchyScreenEvent.FocusContext(it)) },
                    onAddSubproject = { onEvent(ContextHierarchyScreenEvent.AddSubprojectRequest(it)) },
                    onDeleteProject = { onEvent(ContextHierarchyScreenEvent.DeleteRequest(it)) },
                    onEditProject = { onEvent(ContextHierarchyScreenEvent.EditRequest(it)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchStartState(
    history: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Start typing to search contexts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "You can search by context name, tag, or a part of text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

        if (history.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Recent searches",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    history.take(8).forEach { query ->
                        FilterChip(
                            selected = false,
                            onClick = { onSuggestionClick(query) },
                            label = { Text(query) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StableHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModernBottomNavButton(
        text = "Home",
        icon = Icons.Outlined.Home,
        onClick = onClick,
    )
}

@Composable
internal fun OptimizedExpandingProjectHierarchyBottomNav(
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    onShowCommandDeck: () -> Unit,
    onRecentsClick: () -> Unit,
    onDayPlanClick: () -> Unit,
    onHomeClick: () -> Unit,
    onStrManagementClick: () -> Unit,
    strategicManagementEnabled: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onActivityTrackerClick: () -> Unit,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
) {
    val stableOnHomeClick = remember { { onHomeClick() } }
    val stableOnDayPlanClick = remember { { onDayPlanClick() } }
    val stableOnToggleSearch = remember { onToggleSearch }
    val stableOnRecentsClick = remember { { onRecentsClick() } }
    val stableOnActivityTrackerClick = remember { { onActivityTrackerClick() } }
    val stableOnShowCommandDeck = remember { { onShowCommandDeck() } }

    ExpandingProjectHierarchyBottomNav(
        onToggleSearch = stableOnToggleSearch,
        onGlobalSearchClick = onGlobalSearchClick,
        onShowCommandDeck = stableOnShowCommandDeck,
        onRecentsClick = stableOnRecentsClick,
        onDayPlanClick = stableOnDayPlanClick,
        onHomeClick = stableOnHomeClick,
        onStrManagementClick = onStrManagementClick,
        strategicManagementEnabled = strategicManagementEnabled,
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        onActivityTrackerClick = stableOnActivityTrackerClick,
        onEvent = onEvent,
    )
}
