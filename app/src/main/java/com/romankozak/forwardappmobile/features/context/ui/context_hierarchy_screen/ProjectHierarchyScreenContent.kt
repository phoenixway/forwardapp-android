package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.components.ExpandingProjectHierarchyBottomNav
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.components.ModernBottomNavButton
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.components.SearchResultsView
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.hierarchy.BreadcrumbNavigation
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.hierarchy.ProjectHierarchyView
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.PlanningMode



import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope



@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyScreenContent(
    modifier: Modifier = Modifier,
    uiState: ProjectHierarchyScreenUiState,
    onEvent: (ProjectHierarchyScreenEvent) -> Unit,
    listState: LazyListState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
        remember(uiState.currentBreadcrumbs) {
            uiState.currentBreadcrumbs.isNotEmpty()
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (isSearchActive && searchQuery.isNotBlank()) {
            
            SearchResultsView(
                results = uiState.searchResults,
                onRevealClick = { onEvent(ProjectHierarchyScreenEvent.SearchResultClick(it)) },
                onOpenClick = { onEvent(ProjectHierarchyScreenEvent.ProjectClick(it)) },
            )
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
                    onNavigate = { onEvent(ProjectHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ProjectHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { projectId ->
                        uiState.projectHierarchy.allProjects.find { it.id == projectId }
                            ?.let { onEvent(ProjectHierarchyScreenEvent.ProjectMenuRequest(it)) }
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
                
                val emptyText =
                    remember(uiState.planningMode, uiState.planningSettings) {
                        when (uiState.planningMode) {
                            PlanningMode.Today -> "No contexts with tag '#${uiState.planningSettings.dailyTag}'"
                            PlanningMode.Medium -> "No contexts with tag '#${uiState.planningSettings.mediumTag}'"
                            PlanningMode.Long -> "No contexts with tag '#${uiState.planningSettings.longTag}'"
                            else -> "Create your first context"
                        }
                    }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emptyText, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                
                ProjectHierarchyView(
                    modifier = Modifier.weight(1f),
                    hierarchy = uiState.projectHierarchy,
                    flattenedHierarchy = uiState.flattenedHierarchy,
                    breadcrumbs = uiState.currentBreadcrumbs,
                    focusedProjectId =
                        when (currentSubState) {
                            is ProjectHierarchyScreenSubState.ProjectFocused -> currentSubState.projectId
                            else -> null
                        },
                    highlightedProjectId = null,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    planningMode = uiState.planningMode,
                    hierarchySettings = HierarchyDisplaySettings(),
                    listState = listState,
                    longDescendantsMap = emptyMap(),
                    onEvent = onEvent,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onProjectClicked = { onEvent(ProjectHierarchyScreenEvent.ProjectClick(it)) },
                    onToggleExpanded = { onEvent(ProjectHierarchyScreenEvent.ToggleProjectExpanded(it)) },
                    onMenuRequested = { onEvent(ProjectHierarchyScreenEvent.ProjectMenuRequest(it)) },
                    onProjectReorder = { from, to, pos ->
                        onEvent(ProjectHierarchyScreenEvent.ProjectReorder(from, to, pos))
                    },
                    onFocusProject = { onEvent(ProjectHierarchyScreenEvent.FocusProject(it)) },
                    onAddSubproject = { onEvent(ProjectHierarchyScreenEvent.AddSubprojectRequest(it)) },
                    onDeleteProject = { onEvent(ProjectHierarchyScreenEvent.DeleteRequest(it)) },
                    onEditProject = { onEvent(ProjectHierarchyScreenEvent.EditRequest(it)) },
                )
            }
        }
    }
}



@Composable
private fun StableHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    
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
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    planningModesEnabled: Boolean,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onDayPlanClick: () -> Unit,
    onHomeClick: () -> Unit,
    onStrManagementClick: () -> Unit,
    strategicManagementEnabled: Boolean,
    aiChatEnabled: Boolean,
    aiInsightsEnabled: Boolean,
    aiLifeManagementEnabled: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAiChatClick: () -> Unit,
    onActivityTrackerClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onShowReminders: () -> Unit,
    onLifeStateClick: () -> Unit,
    onTacticsClick: () -> Unit,
    onEvent: (ProjectHierarchyScreenEvent) -> Unit,
) {
    
    val stableOnHomeClick = remember { { onHomeClick() } }
    val stableOnDayPlanClick = remember { { onDayPlanClick() } }
    val stableOnToggleSearch = remember { onToggleSearch }
    val stableOnRecentsClick = remember { { onRecentsClick() } }
    val stableOnActivityTrackerClick = remember { { onActivityTrackerClick() } }
    val stableOnShowCommandDeck = remember { { onShowCommandDeck() } }
    val stableOnContextsClick = remember { { onContextsClick() } }

    
    ExpandingProjectHierarchyBottomNav(
        onToggleSearch = stableOnToggleSearch,
        onGlobalSearchClick = onGlobalSearchClick,
        onShowCommandDeck = stableOnShowCommandDeck,
        currentMode = currentMode,
        onPlanningModeChange = onPlanningModeChange,
        planningModesEnabled = planningModesEnabled,
        onContextsClick = stableOnContextsClick,
        onRecentsClick = stableOnRecentsClick,
        onDayPlanClick = stableOnDayPlanClick,
        onHomeClick = stableOnHomeClick,
        onStrManagementClick = onStrManagementClick,
        strategicManagementEnabled = strategicManagementEnabled,
        aiChatEnabled = aiChatEnabled,
        aiInsightsEnabled = aiInsightsEnabled,
        aiLifeManagementEnabled = aiLifeManagementEnabled,
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        onAiChatClick = onAiChatClick,
        onActivityTrackerClick = stableOnActivityTrackerClick,
        onInsightsClick = onInsightsClick,
        onShowReminders = onShowReminders,
        onLifeStateClick = onLifeStateClick,
        onTacticsClick = onTacticsClick,
        onEvent = onEvent,
    )
}
