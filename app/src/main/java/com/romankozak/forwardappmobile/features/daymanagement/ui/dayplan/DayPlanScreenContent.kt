package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist.TaskList
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist.TaskListActions
import com.romankozak.forwardappmobile.ui.common.MatrixRainView

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Помилка завантаження",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Спробувати ще раз") }
    }
}

@Composable
fun DayPlanContent(
    state: DayPlanContentState,
    viewModel: DayPlanViewModel,
    visualState: DayPlanVisualState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visualState.isContentReady,
            enter =
                fadeIn(
                    animationSpec =
                        tween(
                            CONTENT_FADE_IN_DURATION_MILLIS,
                            delayMillis = CONTENT_FADE_IN_DELAY_MILLIS,
                        ),
                ),
            modifier = Modifier.fillMaxSize(),
        ) {
            DayPlanPrimaryContent(state = state, viewModel = viewModel)
        }

        AnimatedVisibility(
            visible = visualState.showMatrixSplash,
            exit = fadeOut(animationSpec = tween(CONTENT_FADE_IN_DURATION_MILLIS)),
        ) {
            AndroidView(
                factory = { context ->
                    MatrixRainView(context).also(visualState.onMatrixViewCreated)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DayPlanPrimaryContent(
    state: DayPlanContentState,
    viewModel: DayPlanViewModel,
) {
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.uiState.isLoading -> LoadingState()
            state.uiState.error != null && state.uiState.tasks.isEmpty() -> {
                ErrorState(
                    error = state.uiState.error!!,
                    onRetry = { viewModel.loadDataForPlan(state.initialDayPlanId) },
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TaskList(
                        tasks = state.uiState.tasks,
                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        actions =
                            TaskListActions(
                                onTaskClick = viewModel::onEditTaskClicked,
                                onTaskLongPress = viewModel::onTaskLongPressed,
                                onTasksReordered = { reorderedList ->
                                    state.uiState.dayPlan?.let { dayPlan ->
                                        viewModel.updateTasksOrder(dayPlan.id, reorderedList)
                                    }
                                },
                                onToggleTask = viewModel::toggleTaskCompletion,
                                onParentInfoClick = { parentInfo ->
                                    navigateToParentInfo(parentInfo = parentInfo, navigator = state.navigator)
                                },
                            ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = state.snackbarHostState,
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun navigateToParentInfo(
    parentInfo: ParentInfo,
    navigator: DayPlanScreenNavigator,
) {
    when (parentInfo.type) {
        ParentType.PROJECT -> {
            navigator.navigationManager.navigateOrFallback(
                navController = navigator.navController,
                target = NavTarget.ContextDetail(contextId = parentInfo.id),
                recordInHistory = true,
            )
        }

        ParentType.GOAL -> {
            parentInfo.projectId?.let { listId ->
                navigator.navigationManager.navigateOrFallback(
                    navController = navigator.navController,
                    target = NavTarget.ContextDetail(contextId = listId, goalId = parentInfo.id),
                    recordInHistory = true,
                )
            } ?: Log.e(TAG, "Goal parentInfo has null projectId for goalId: ${parentInfo.id}")
        }
    }
}
