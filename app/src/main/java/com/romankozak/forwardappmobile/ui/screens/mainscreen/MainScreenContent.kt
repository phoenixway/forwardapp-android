package com.romankozak.forwardappmobile.ui.screens.mainscreen

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ExpandingBottomNav
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ModernBottomNavButton
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.SearchResultsView
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.BreadcrumbNavigation
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyView
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode



import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope



@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreenContent(
    modifier: Modifier = Modifier,
    uiState: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
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
            currentSubState is MainSubState.LocalSearch
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
                onRevealClick = { onEvent(MainScreenEvent.SearchResultClick(it)) },
                onOpenClick = { onEvent(MainScreenEvent.ProjectClick(it)) },
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
                    onNavigate = { onEvent(MainScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(MainScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { projectId ->
                        uiState.projectHierarchy.allProjects.find { it.id == projectId }
                            ?.let { onEvent(MainScreenEvent.ProjectMenuRequest(it)) }
                    },
                )
            }

            
            val isListEmpty =
                remember(uiState.projectHierarchy) {
                    uiState.projectHierarchy.topLevelProjects.isEmpty() &&
                        uiState.projectHierarchy.childMap.isEmpty()
                }

            if (isListEmpty) {
                
                val emptyText =
                    remember(uiState.planningMode, uiState.planningSettings) {
                        when (uiState.planningMode) {
                            PlanningMode.Today -> "No projects with tag '#${uiState.planningSettings.dailyTag}'"
                            PlanningMode.Medium -> "No projects with tag '#${uiState.planningSettings.mediumTag}'"
                            PlanningMode.Long -> "No projects with tag '#${uiState.planningSettings.longTag}'"
                            else -> "Create your first project"
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
                    breadcrumbs = uiState.currentBreadcrumbs,
                    focusedProjectId =
                        when (currentSubState) {
                            is MainSubState.ProjectFocused -> currentSubState.projectId
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
                    onProjectClicked = { onEvent(MainScreenEvent.ProjectClick(it)) },
                    onToggleExpanded = { onEvent(MainScreenEvent.ToggleProjectExpanded(it)) },
                    onMenuRequested = { onEvent(MainScreenEvent.ProjectMenuRequest(it)) },
                    onNavigateToProject = {  },
                    onProjectReorder = { from, to, pos ->
                        onEvent(MainScreenEvent.ProjectReorder(from, to, pos))
                    },
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
internal fun OptimizedExpandingBottomNav(
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onDayPlanClick: () -> Unit,
    onHomeClick: () -> Unit,
    onStrManagementClick: () -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAiChatClick: () -> Unit,
    onActivityTrackerClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onShowReminders: () -> Unit,
) {
    
    val stableOnHomeClick = remember { { onHomeClick() } }
    val stableOnDayPlanClick = remember { { onDayPlanClick() } }
    val stableOnToggleSearch = remember { onToggleSearch }
    val stableOnRecentsClick = remember { { onRecentsClick() } }
    val stableOnActivityTrackerClick = remember { { onActivityTrackerClick() } }

    
    ExpandingBottomNav(
        onToggleSearch = stableOnToggleSearch,
        onGlobalSearchClick = onGlobalSearchClick,
        currentMode = currentMode,
        onPlanningModeChange = onPlanningModeChange,
        onContextsClick = onContextsClick,
        onRecentsClick = stableOnRecentsClick,
        onDayPlanClick = stableOnDayPlanClick,
        onHomeClick = stableOnHomeClick,
        onStrManagementClick = onStrManagementClick,
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        onAiChatClick = onAiChatClick,
        onActivityTrackerClick = stableOnActivityTrackerClick,
        onInsightsClick = onInsightsClick,
        onShowReminders = onShowReminders,
    )
}
