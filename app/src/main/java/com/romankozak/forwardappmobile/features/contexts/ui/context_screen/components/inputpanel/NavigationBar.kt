package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import kotlinx.coroutines.delay


@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun NavigationBar(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
    modifier: Modifier = Modifier,
    holdMenuController: HoldMenu2Controller,
) {
    val lastNonSearchMode = remember { mutableStateOf<InputMode?>(null) }
    LaunchedEffect(state.inputMode) {
        if (state.inputMode != InputMode.SearchInList && state.inputMode != InputMode.SearchGlobal) {
            lastNonSearchMode.value = state.inputMode
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val baseWidth = if (state.isProjectManagementEnabled) 380.dp else 320.dp
        val showRecents = availableWidth > (baseWidth - 40.dp)

        Row(
            modifier = Modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // --- LEFT SIDE ---
            BackForwardButton(state, actions, contentColor)

            Icon(
                Icons.Outlined.AlternateEmail,
                "Ієрархія проєктів",
                tint = contentColor.copy(alpha = 0.7f),
                modifier =
                    Modifier
                        .size(40.dp)
                        .combinedClickable(
                            onClick = actions.onShowProjectHierarchy,
                            onLongClick = actions.onNavigateHome,
                        )
                        .padding(10.dp),
            )

            Row {
                AnimatedVisibility(visible = showRecents) {
                    IconButton(onClick = actions.onRecentsClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Outlined.Restore,
                            "Недавні",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                color =
                                    if (state.inputMode == InputMode.SearchInList || state.inputMode == InputMode.SearchGlobal) {
                                        contentColor.copy(alpha = 0.16f)
                                    } else {
                                        Color.Transparent
                                    },
                            )
                            .combinedClickable(
                                onClick = {
                                    if (state.inputMode == InputMode.SearchInList || state.inputMode == InputMode.SearchGlobal) {
                                        actions.onInputModeSelected(lastNonSearchMode.value ?: InputMode.AddGoal)
                                    } else {
                                        lastNonSearchMode.value = state.inputMode
                                        actions.onInputModeSelected(InputMode.SearchInList)
                                    }
                                },
                                onLongClick = {
                                    if (state.inputMode == InputMode.SearchInList || state.inputMode == InputMode.SearchGlobal) {
                                        actions.onInputModeSelected(lastNonSearchMode.value ?: InputMode.AddGoal)
                                    } else {
                                        lastNonSearchMode.value = state.inputMode
                                        actions.onInputModeSelected(InputMode.SearchGlobal)
                                    }
                                },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = contentColor.copy(alpha = 0.2f)),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        "Пошук (довгий тап — скрізь)",
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // --- RIGHT SIDE ---
            ViewModeToggle(
                currentView = state.currentView,
                isProjectManagementEnabled = state.isProjectManagementEnabled,
                enableInbox = state.enableInbox,
                enableLog = state.enableLog,
                enableArtifact = state.enableArtifact,
                enableBacklog = state.enableBacklog,
                enableDashboard = state.enableDashboard,
                enableAttachments = state.enableAttachments,
                onViewChange = actions.onViewChange,
                onInputModeSelected = actions.onInputModeSelected,
                contentColor = contentColor,
                holdMenuController = holdMenuController,
            )
            OptionsMenu(state = state, actions = actions, contentColor = contentColor)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BackForwardButton(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
) {
    val shouldShowButton = true

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
            modifier =
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = true,
                        onClick = {
                            if (state.canGoBack) {
                                actions.onBackClick()
                            } else {
                                actions.onNavigateHome()
                            }
                        },
                        onLongClick = {
                            if (state.canGoForward) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showForwardIcon = true
                                actions.onForwardClick()
                            }
                        },
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            BackForwardIcon(state = state, showForwardIcon = showForwardIcon, contentColor = contentColor)

            if (state.canGoForward && !showForwardIcon) {
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Box(
                        modifier =
                            Modifier.padding(4.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(width = 1.dp, color = contentColor.copy(alpha = 0.5f), shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BackForwardIcon(
    state: NavPanelState,
    showForwardIcon: Boolean,
    contentColor: Color,
) {
    val iconColor by
        animateColorAsState(
            targetValue = if (state.canGoBack) contentColor else contentColor.copy(alpha = 0.3f),
            label = "backIconColor",
        )
    val iconScale by
        animateFloatAsState(targetValue = if (state.canGoBack) 1.2f else 1.0f, label = "backIconScale")

    AnimatedContent(
        targetState = showForwardIcon,
        transitionSpec = {
            (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                (slideOutHorizontally { -it / 2 } + fadeOut())
        },
        label = "BackForwardIconAnimation",
    ) { isForward ->
        Icon(
            imageVector =
                if (isForward) {
                    Icons.AutoMirrored.Filled.ArrowForward
                } else {
                    Icons.AutoMirrored.Filled.ArrowBack
                },
            contentDescription = "Назад (довге натискання - Вперед)",
            modifier = Modifier.size(20.dp).scale(if (isForward) 1.2f else iconScale),
            tint = if (isForward) MaterialTheme.colorScheme.primary else iconColor,
        )
    }
}