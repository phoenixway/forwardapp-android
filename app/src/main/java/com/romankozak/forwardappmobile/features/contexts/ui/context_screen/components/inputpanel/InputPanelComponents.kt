package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import kotlinx.coroutines.delay

@Composable
internal fun InputTextField(
    modifier: Modifier = Modifier,
    inputValue: TextFieldValue,
    inputMode: InputMode,
    panelColors: PanelColors,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    isNerActive: Boolean,
) {
    Surface(
        modifier =
            modifier
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 3)
                .defaultMinSize(minHeight = 44.dp),
        shape = RoundedCornerShape(20.dp),
        color = panelColors.inputFieldColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, panelColors.accentColor.copy(alpha = 0.3f)),
    ) {
        BasicTextField(
            value = inputValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = panelColors.contentColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (inputValue.text.isNotBlank()) onSubmit() }),
            cursorBrush = SolidColor(panelColors.accentColor),
            decorationBox = { innerTextField ->
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputValue.text.isEmpty()) {
                            Text(
                                text =
                                    when (inputMode) {
                                        InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                                        InputMode.AddQuickRecord -> stringResource(R.string.hint_add_quick_record)
                                        InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                                        else -> "Додати..."
                                    },
                                color = panelColors.contentColor.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                    NerIndicator(isActive = isNerActive, hasText = inputValue.text.isNotBlank())
                }
            },
        )
    }
}

@Composable
internal fun ModeSelectorButton(
    inputMode: InputMode,
    panelColors: PanelColors,
    onOpenMenu: () -> Unit,
    onModeChange: (InputMode) -> Unit,
    isProjectManagementEnabled: Boolean,
    currentView: ContextViewMode,
) {
    val modes =
        remember(isProjectManagementEnabled, currentView) {
            listOfNotNull(
                InputMode.AddGoal,
                InputMode.AddQuickRecord,
                if (isProjectManagementEnabled) InputMode.AddProjectLog else null,
                InputMode.SearchGlobal,
            )
        }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Surface(
        onClick = onOpenMenu,
        shape = CircleShape,
        color = panelColors.contentColor.copy(alpha = 0.1f),
        modifier =
            Modifier.size(48.dp).pointerInput(inputMode) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 50f) onModeChange(modes[(modes.indexOf(inputMode) + 1) % modes.size])
                        dragOffset = 0f
                    },
                ) { _, amount -> dragOffset += amount }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector =
                    when (inputMode) {
                        InputMode.AddGoal -> Icons.Outlined.Add
                        InputMode.AddQuickRecord -> Icons.Outlined.Inbox
                        InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
                        else -> Icons.Outlined.PostAdd
                    },
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun BackForwardButton(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
) {
    val haptic = LocalHapticFeedback.current
    var showForwardIcon by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier.size(40.dp).clip(CircleShape).combinedClickable(
                onClick = { if (state.canGoBack) actions.onBackClick() else actions.onNavigateHome() },
                onLongClick = {
                    if (state.canGoForward) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showForwardIcon = true
                        actions.onForwardClick()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (showForwardIcon) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = if (state.canGoBack) contentColor else contentColor.copy(alpha = 0.3f),
        )
    }

    if (showForwardIcon) {
        LaunchedEffect(Unit) {
            delay(400)
            showForwardIcon = false
        }
    }
}

@Composable
internal fun AnimatedSendButton(
    isVisible: Boolean,
    panelColors: PanelColors,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = isVisible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp).background(panelColors.accentColor, CircleShape),
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = if (panelColors.accentColor.luminance() > 0.5f) Color.Black else Color.White,
                ),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
        }
    }
}
