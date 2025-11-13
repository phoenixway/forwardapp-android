package com.romankozak.forwardappmobile.features.mainscreen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.mainscreen.components.MainScreenScaffold
import com.romankozak.forwardappmobile.features.mainscreen.models.ProjectUiEvent

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val appComponent = LocalAppComponent.current
    val viewModel: MainScreenViewModel = remember(appComponent) { appComponent.mainScreenViewModel }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is ProjectUiEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }

    MainScreenScaffold(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
