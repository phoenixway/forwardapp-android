package com.romankozak.forwardappmobile.features.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import androidx.compose.material.icons.outlined.Today
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.ai.insights.AiInsightsViewModel
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState
import com.romankozak.forwardappmobile.features.strategicmanagement.StrategicManagementScreen
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.net.URLEncoder

private const val TAB_SWIPE_THRESHOLD_PX = 48f

@Composable
internal fun MainScreenPagerContent(
    onNavigateToRecentItem: (RecentItem) -> Unit,
    tabs: List<CommandDeckTab>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    navController: NavController,
    navigationManager: EnhancedNavigationManager?,
    context: android.content.Context,
    aiInsightsViewModel: AiInsightsViewModel,
    focusContextsViewModel: FocusContextsViewModel,
    tacticalMissionViewModel: TacticalMissionViewModel,
    tacticalObsidianVaultName: String,
    dayManagementViewModel: DayManagementViewModel,
    currentDayManagementTab: DayManagementTab,
    defaultTodayTab: DayManagementTab,
    dayManagementRuntimeViewModel: DayManagementRuntimeViewModel,
    dayPlanViewModel: DayPlanViewModel,
    activityTrackerViewModel: ActivityTrackerViewModel,
    dayFocusesViewModel: DayFocusesViewModel,
    sessionModeState: SessionModeState,
    latestSessionReason: String?,
    onSessionModeSelected: (com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode) -> Unit,
    isSessionModeCardExpanded: Boolean,
    onSessionModeCardExpandedChange: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(tabs.size, pagerState.currentPage) {
                    var horizontalDragAccum = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalDragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalDragAccum += dragAmount
                        },
                        onDragCancel = { horizontalDragAccum = 0f },
                        onDragEnd = {
                            val dragDelta = horizontalDragAccum
                            horizontalDragAccum = 0f
                            if (abs(dragDelta) < TAB_SWIPE_THRESHOLD_PX) return@detectHorizontalDragGestures

                            val targetPage =
                                when {
                                    dragDelta < 0f ->
                                        if (pagerState.currentPage < tabs.lastIndex) {
                                            pagerState.currentPage + 1
                                        } else {
                                            0
                                        }
                                    dragDelta > 0f ->
                                        if (pagerState.currentPage > 0) {
                                            pagerState.currentPage - 1
                                        } else {
                                            tabs.lastIndex
                                        }
                                    else -> null
                                }

                            targetPage?.let { page ->
                                scope.launch {
                                    pagerState.scrollToPage(page)
                                }
                            }
                        },
                    )
                },
        beyondViewportPageCount = 1,
        userScrollEnabled = false,
    ) { page ->
        when (tabs[page]) {
            CommandDeckTab.Dashboard ->
                DashboardContent(
                    onNavigateToRecentItem = onNavigateToRecentItem,
                    modifier = Modifier.fillMaxSize(),
                    onOpenFocusedContext = { contextId ->
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.ContextDetail(contextId = contextId),
                            recordInHistory = true,
                        )
                    },
                    sessionModeState = sessionModeState,
                    latestSessionReason = latestSessionReason,
                    onSessionModeSelected = onSessionModeSelected,
                    isSessionModeCardExpanded = isSessionModeCardExpanded,
                    onSessionModeCardExpandedChange = onSessionModeCardExpandedChange,
                    onOpenSessionContext = { contextId ->
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.ContextDetail(contextId = contextId),
                            recordInHistory = true,
                        )
                    },
                    aiInsightsViewModel = aiInsightsViewModel,
                    focusContextsViewModel = focusContextsViewModel,
                )
            CommandDeckTab.Strategy ->
                StrategicManagementScreen(
                    navController = navController,
                    navigationManager = navigationManager,
                )
            CommandDeckTab.Core ->
                CoreLevelScreen(
                    navController = navController,
                    navigationManager = navigationManager,
                )
            CommandDeckTab.StrategicArc ->
                StrategicArcScreen(
                    navController = navController,
                    navigationManager = navigationManager,
                )
            CommandDeckTab.Tactics ->
                TacticalManagementScreen(
                    showFabMenu = false,
                    viewModel = tacticalMissionViewModel,
                    onLinkedProjectClick = { projectId ->
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.ContextDetail(contextId = projectId),
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLinkedAttachmentClick = { attachment ->
                        when {
                            attachment.attachmentType == "NOTE_DOCUMENT" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.NoteDocument(id = attachment.entityId),
                                )
                            attachment.attachmentType == "MUSIC_NOTE" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.MusicNote(id = attachment.entityId),
                                )
                            attachment.attachmentType == "CHECKLIST" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.Checklist(id = attachment.entityId),
                                )
                            attachment.linkType == LinkType.CONTEXT &&
                                !attachment.target.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.ContextDetail(contextId = attachment.target),
                                    recordInHistory = true,
                                )
                            (
                                attachment.linkType == LinkType.URL ||
                                    attachment.linkType == LinkType.OBSIDIAN
                            ) && !attachment.target.isNullOrBlank() -> {
                                val resolvedTarget =
                                    buildExternalTarget(
                                        attachment.linkType,
                                        attachment.target,
                                        attachment.vault,
                                        tacticalObsidianVaultName,
                                    )
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(resolvedTarget)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }.onFailure {
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.AttachmentsLibrary,
                                    ) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    runCatching {
                                        navController.getBackStackEntry("attachments_library_screen")
                                            .savedStateHandle["attachment_library_query"] = attachment.id
                                    }
                                }
                            }
                            else -> {
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.AttachmentsLibrary,
                                ) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                runCatching {
                                    navController.getBackStackEntry("attachments_library_screen")
                                        .savedStateHandle["attachment_library_query"] = attachment.id
                                }
                            }
                        }
                    },
                )
            CommandDeckTab.Today ->
                DayManagementScreen(
                    mainNavController = navController,
                    navigationManager = navigationManager,
                    viewModel = dayManagementViewModel,
                    runtimeViewModel = dayManagementRuntimeViewModel,
                    dayPlanViewModel = dayPlanViewModel,
                    activityTrackerViewModel = activityTrackerViewModel,
                    dayFocusesViewModel = dayFocusesViewModel,
                    currentDayManagementTab =
                        currentDayManagementTab
                            .takeIf { it in DayManagementTab.todaySubTabs() }
                            ?: defaultTodayTab,
                    showFabMenu = false,
                )
        }
    }
}


private fun buildExternalTarget(
    linkType: LinkType?,
    target: String,
    vault: String? = null,
    globalObsidianVaultName: String? = null,
): String {
    val trimmed = target.trim()
    if (linkType == LinkType.OBSIDIAN && !trimmed.startsWith("obsidian://", ignoreCase = true)) {
        val vaultName = vault?.takeIf { it.isNotBlank() } ?: globalObsidianVaultName?.takeIf { it.isNotBlank() }
        val encodedFile = URLEncoder.encode(trimmed, "UTF-8")
        return if (vaultName != null) {
            "obsidian://open?vault=${URLEncoder.encode(vaultName, "UTF-8")}&file=$encodedFile"
        } else {
            "obsidian://open?file=$encodedFile"
        }
    }
    return trimmed
}


