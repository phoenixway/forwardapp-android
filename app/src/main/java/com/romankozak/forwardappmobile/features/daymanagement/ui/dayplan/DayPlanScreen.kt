package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.common.MatrixRainView

const val TAG = "NAV_DEBUG"
internal const val PRELOAD_CONTENT_DELAY_MILLIS = 100L
internal const val MATRIX_SPLASH_VISIBLE_DELAY_MILLIS = 600L
internal const val MATRIX_SPLASH_FADE_OUT_DELAY_MILLIS = 500L
internal const val LINK_PICKER_OPEN_DELAY_MILLIS = 160L
internal const val CONTENT_FADE_IN_DURATION_MILLIS = 300
internal const val CONTENT_FADE_IN_DELAY_MILLIS = 400

data class DayPlanScreenNavigator(
    val navController: NavController,
    val navigationManager: EnhancedNavigationManager?,
    val onNavigateToBacklog: (DayTask) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayPlanScreen(
    initialDayPlanId: String,
    navigator: DayPlanScreenNavigator,
    addTaskTrigger: Int,
    viewModel: DayPlanViewModel,
    modifier: Modifier = Modifier,
) {
    val systemUiController = rememberSystemUiController()
    val isLight = !isSystemInDarkTheme()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val presentationState = rememberDayPlanPresentationState(viewModel)
    val contentState =
        DayPlanContentState(
            initialDayPlanId = initialDayPlanId,
            uiState = presentationState.uiState,
            navigator = navigator,
            snackbarHostState = presentationState.snackbarHostState,
        )

    HandleDayPlanScreenEffects(
        presentationState = presentationState,
        viewModel = viewModel,
        config =
            DayPlanEffectsConfig(
                navigator = navigator,
                initialDayPlanId = initialDayPlanId,
                addTaskTrigger = addTaskTrigger,
                isLight = isLight,
                systemUiController = systemUiController,
            ),
    )
    DayPlanContent(
        state = contentState,
        viewModel = viewModel,
        visualState =
            DayPlanVisualState(
                isContentReady = presentationState.isContentReady.value,
                showMatrixSplash = presentationState.showMatrixSplash.value,
                onMatrixViewCreated = { view: MatrixRainView ->
                    presentationState.matrixView.value = view
                },
            ),
        modifier = modifier,
    )
    DayPlanConnectionsHost(
        state = contentState,
        dialogState = presentationState.dialogState,
        overlayState = presentationState.overlayState,
        deps =
            DayPlanConnectionDeps(
                viewModel = viewModel,
                scope = scope,
                context = context,
            ),
    )
    DayPlanDialogsHost(
        state = contentState,
        dialogState = presentationState.dialogState,
        overlayState = presentationState.overlayState,
        viewModel = viewModel,
        hapticFeedback = hapticFeedback,
    )
}
