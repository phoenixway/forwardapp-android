// File: NavControls.kt

// Corrected File: ui/screens/backlog/components/inputpanel/NavControls.kt

package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NavControls(state: NavPanelState, actions: NavPanelActions, contentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CloseSearchButton(
            isVisible = state.inputMode == InputMode.SearchInList,
            contentColor = contentColor,
            onCloseSearch = actions.onCloseSearch
        )

        BackForwardButton(
            state = state,
            actions = actions,
            contentColor = contentColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        NavigationButtons(
            contentColor = contentColor,
            onHomeClick = actions.onHomeClick,
            onRevealInExplorer = actions.onRevealInExplorer
        )
    }
}

@Composable
private fun CloseSearchButton(
    isVisible: Boolean,
    contentColor: Color,
    onCloseSearch: () -> Unit
) {
    AnimatedVisibility(visible = isVisible) {
        val closeIconColor by animateColorAsState(
            targetValue = contentColor.copy(alpha = 0.7f),
            label = "closeIconColor"
        )
        IconButton(
            onClick = onCloseSearch,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                "Закрити пошук",
                tint = closeIconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BackForwardButton(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color
) {
    val shouldShowButton = state.inputMode != InputMode.SearchInList &&
            (state.canGoBack || state.canGoForward)

    AnimatedVisibility(visible = shouldShowButton) {
        val haptic = LocalHapticFeedback.current
        var showForwardIcon by remember { mutableStateOf(false) }

        LaunchedEffect(showForwardIcon) {
            if (showForwardIcon) {
                delay(400L)
                showForwardIcon = false
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .combinedClickable(
                    enabled = state.canGoBack || state.canGoForward,
                    onClick = { if (state.canGoBack) actions.onBackClick() },
                    onLongClick = {
                        if (state.canGoForward) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showForwardIcon = true
                            actions.onForwardClick()
                        }
                    },
                    indication = ripple(bounded = false),
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            BackForwardIcon(
                state = state,
                showForwardIcon = showForwardIcon,
                contentColor = contentColor
            )

            // Forward Action Indicator - Fixed: Use Box-compatible AnimatedVisibility
            if (state.canGoForward && !showForwardIcon) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(
                                width = 1.dp,
                                color = contentColor.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BackForwardIcon(
    state: NavPanelState,
    showForwardIcon: Boolean,
    contentColor: Color
) {
    val iconColor by animateColorAsState(
        targetValue = if (state.canGoBack) contentColor else contentColor.copy(alpha = 0.3f),
        label = "backIconColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (state.canGoBack) 1.2f else 1.0f,
        label = "backIconScale"
    )

    AnimatedContent(
        targetState = showForwardIcon,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "BackForwardIconAnimation"
    ) { isForward ->
        Icon(
            imageVector = if (isForward) {
                Icons.AutoMirrored.Filled.ArrowForward
            } else {
                Icons.AutoMirrored.Filled.ArrowBack
            },
            contentDescription = "Назад (довге натискання - Вперед)",
            modifier = Modifier
                .size(20.dp)
                .scale(if (isForward) 1.2f else iconScale),
            tint = if (isForward) MaterialTheme.colorScheme.primary else iconColor
        )
    }
}

@Composable
private fun NavigationButtons(
    contentColor: Color,
    onHomeClick: () -> Unit,
    onRevealInExplorer: () -> Unit
) {
    val homeIconColor by animateColorAsState(
        targetValue = contentColor.copy(alpha = 0.7f),
        label = "homeIconColor"
    )
    val homeIconScale by animateFloatAsState(
        targetValue = 1.0f,
        label = "homeIconScale"
    )

    IconButton(
        onClick = onHomeClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            Icons.Filled.Home,
            "Дім",
            tint = homeIconColor,
            modifier = Modifier
                .size(20.dp)
                .scale(homeIconScale)
        )
    }

    val revealIconColor by animateColorAsState(
        targetValue = contentColor.copy(alpha = 0.7f),
        label = "revealIconColor"
    )
    val revealIconScale by animateFloatAsState(
        targetValue = 1.0f,
        label = "revealIconScale"
    )

    IconButton(
        onClick = onRevealInExplorer,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            Icons.Filled.MyLocation,
            "Показати у списку",
            tint = revealIconColor,
            modifier = Modifier
                .size(20.dp)
                .scale(revealIconScale)
        )
    }
}