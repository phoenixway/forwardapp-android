package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback

@Composable
fun DayManagementNavigationEffects(
    viewModel: DayManagementViewModel,
    uiState: DayManagementState,
    navigationManager: EnhancedNavigationManager?,
    mainNavController: NavController,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DayManagementUiEvent.NavigateToProject -> {
                    navigationManager.navigateOrFallback(
                        navController = mainNavController,
                        target = NavTarget.ContextDetail(contextId = event.projectId),
                        recordInHistory = true,
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val result =
                snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Спробувати знову",
                    duration = SnackbarDuration.Long,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLoading()
            }
        }
    }
}
