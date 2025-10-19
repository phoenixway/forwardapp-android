package com.romankozak.forwardappmobile.ui.screens.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.vector.ImageVector
import com.romankozak.forwardappmobile.ui.components.AdaptiveSegmentedControl
import com.romankozak.forwardappmobile.ui.components.SegmentedTab

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    title: String,
    navController: NavController,
    tabs: List<String>,
    tabIcons: List<ImageVector>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    content: @Composable (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsTopAppBar(
                title = title,
                navController = navController,
                onSave = onSave,
                isSaveEnabled = isSaveEnabled
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            val tabsWithIcons: List<SegmentedTab> = tabs.mapIndexed { index, tabTitle ->
                SegmentedTab(tabTitle, tabIcons[index])
            }
            
            AdaptiveSegmentedControl(
                tabs = tabsWithIcons,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            HorizontalDivider()
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) +
                        slideInVertically(
                            animationSpec = tween(400),
                            initialOffsetY = { it / 8 }
                        )).togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                animationSpec = tween(200),
                                targetOffsetY = { -it / 8 }
                            )
                    )
                },
                label = "content_animation"
            ) { tabIndex ->
                content(tabIndex)
            }
        }
    }
}
