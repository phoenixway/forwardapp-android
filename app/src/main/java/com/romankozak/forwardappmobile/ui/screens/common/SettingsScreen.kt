package com.romankozak.forwardappmobile.ui.screens.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.AdaptiveSegmentedControl
import com.romankozak.forwardappmobile.ui.components.SegmentedTab

private const val CONTENT_ENTER_ANIMATION_MS = 400
private const val CONTENT_EXIT_ANIMATION_MS = 200
private const val CONTENT_SLIDE_DIVISOR = 8

data class SettingsScreenState(
    val title: String,
    val tabs: List<String>,
    val tabIcons: List<ImageVector>,
    val selectedTabIndex: Int,
    val isSaveEnabled: Boolean,
)

data class SettingsScreenActions(
    val onTabSelected: (Int) -> Unit,
    val onSave: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsScreenState,
    actions: SettingsScreenActions,
    navController: NavController,
    content: @Composable (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsTopAppBar(
                title = state.title,
                navController = navController,
                onSave = actions.onSave,
                isSaveEnabled = state.isSaveEnabled,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val tabsWithIcons: List<SegmentedTab> =
                state.tabs.mapIndexed { index, tabTitle ->
                    SegmentedTab(tabTitle, state.tabIcons[index])
                }

            AdaptiveSegmentedControl(
                tabs = tabsWithIcons,
                selectedTabIndex = state.selectedTabIndex,
                onTabSelected = actions.onTabSelected,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(5.dp))

            HorizontalDivider()
            AnimatedContent(
                targetState = state.selectedTabIndex,
                transitionSpec = {
                    (
                        fadeIn(animationSpec = tween(CONTENT_ENTER_ANIMATION_MS)) +
                            slideInVertically(
                                animationSpec = tween(CONTENT_ENTER_ANIMATION_MS),
                                initialOffsetY = { it / CONTENT_SLIDE_DIVISOR },
                            )
                    ).togetherWith(
                        fadeOut(animationSpec = tween(CONTENT_EXIT_ANIMATION_MS)) +
                            slideOutVertically(
                                animationSpec = tween(CONTENT_EXIT_ANIMATION_MS),
                                targetOffsetY = { -it / CONTENT_SLIDE_DIVISOR },
                            ),
                    )
                },
                label = "content_animation",
            ) { tabIndex ->
                content(tabIndex)
            }
        }
    }
}
