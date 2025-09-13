package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import kotlinx.coroutines.delay

private data class PanelColors(
    val containerColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val inputFieldColor: Color,
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModernInputPanel(
    modifier: Modifier = Modifier,
    inputValue: TextFieldValue,
    inputMode: InputMode,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    onRecentsClick: () -> Unit,
    onAddListLinkClick: () -> Unit,
    onShowAddWebLinkDialog: () -> Unit,
    onShowAddObsidianLinkDialog: () -> Unit,
    onAddListShortcutClick: () -> Unit,
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    currentView: ProjectViewMode,
    onViewChange: (ProjectViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
    onImportBacklogFromMarkdown: () -> Unit,
    onExportBacklogToMarkdown: () -> Unit,
    onExportProjectState: () -> Unit,
    reminderParseResult: ReminderParseResult?,
    onClearReminder: () -> Unit,
    isNerActive: Boolean,
    onStartTrackingCurrentProject: () -> Unit,
    isProjectManagementEnabled: Boolean,
    onToggleProjectManagement: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    val modes =
        remember(isProjectManagementEnabled) {
            listOfNotNull(
                InputMode.AddGoal,
                InputMode.AddQuickRecord,
                if (isProjectManagementEnabled) InputMode.AddProjectLog else null,
                InputMode.SearchGlobal,
                InputMode.SearchInList,
            )
        }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableIntStateOf(1) }

    val currentModeIndex = modes.indexOf(inputMode)

    val panelColors =
        when (inputMode) {
            InputMode.AddGoal ->
                PanelColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    accentColor = MaterialTheme.colorScheme.primary,
                    inputFieldColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            InputMode.AddQuickRecord ->
                PanelColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    inputFieldColor = MaterialTheme.colorScheme.surface,
                )
            InputMode.SearchInList ->
                PanelColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    accentColor = MaterialTheme.colorScheme.primary,
                    inputFieldColor = MaterialTheme.colorScheme.surface,
                )
            InputMode.SearchGlobal ->
                PanelColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    inputFieldColor = MaterialTheme.colorScheme.surface,
                )
            InputMode.AddProjectLog ->
                PanelColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    inputFieldColor = MaterialTheme.colorScheme.surfaceVariant,
                )
        }

    val animatedContainerColor by animateColorAsState(
        targetValue = panelColors.containerColor,
        animationSpec = tween(400),
        label = "panel_color_animation",
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale",
    )

    LaunchedEffect(inputMode) {
        if (inputMode == InputMode.SearchInList || inputMode == InputMode.SearchGlobal) {
            delay(60)
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier =
            modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = animatedContainerColor,
        border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f)),
    ) {
        Column {
            NavigationBar(
                canGoBack = canGoBack,
                onBackClick = onBackClick,
                onForwardClick = onForwardClick,
                onHomeClick = onHomeClick,
                isAttachmentsExpanded = isAttachmentsExpanded,
                onToggleAttachments = onToggleAttachments,
                onEditList = onEditList,
                onShareList = onShareList,
                onDeleteList = onDeleteList,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = onMenuExpandedChange,
                currentView = currentView,
                onViewChange = onViewChange,
                onImportFromMarkdown = onImportFromMarkdown,
                onExportToMarkdown = onExportToMarkdown,
                contentColor = panelColors.contentColor,
                onRecentsClick = onRecentsClick,
                onInputModeSelected = onInputModeSelected,
                onImportBacklogFromMarkdown = onImportBacklogFromMarkdown,
                onExportBacklogToMarkdown = onExportBacklogToMarkdown,
                onExportProjectState = onExportProjectState,
                onStartTrackingCurrentProject = onStartTrackingCurrentProject,
                isProjectManagementEnabled = isProjectManagementEnabled,
                onToggleProjectManagement = onToggleProjectManagement,
            )

            AnimatedVisibility(
                visible = reminderParseResult != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                reminderParseResult?.let {
                    if (it.success) {
                        ReminderChip(
                            suggestionText = it.suggestionText ?: "",
                            onClear = onClearReminder,
                        )
                    } else {
                        Text(
                            text = "Не вдалося розпізнати дату/час: ${it.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box {
                    Surface(
                        onClick = { showModeMenu = true },
                        shape = CircleShape,
                        color = panelColors.contentColor.copy(alpha = 0.1f),
                        contentColor = panelColors.contentColor,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .scale(buttonScale)
                                .pointerInput(inputMode) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isPressed = true },
                                        onDragEnd = {
                                            isPressed = false
                                            val threshold = 50f
                                            when {
                                                dragOffset > threshold -> {
                                                    animationDirection = -1
                                                    val prevIndex = ((currentModeIndex - 1) + modes.size) % modes.size
                                                    onInputModeSelected(modes[prevIndex])
                                                }
                                                dragOffset < -threshold -> {
                                                    animationDirection = 1
                                                    val nextIndex = (currentModeIndex + 1) % modes.size
                                                    onInputModeSelected(modes[nextIndex])
                                                }
                                            }
                                            dragOffset = 0f
                                        },
                                    ) { _, dragAmount -> dragOffset += dragAmount }
                                },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = inputMode,
                                transitionSpec = {
                                    val slideIn =
                                        slideInHorizontally(animationSpec = tween(250)) {
                                            if (animationDirection ==
                                                1
                                            ) {
                                                it
                                            } else {
                                                -it
                                            }
                                        }
                                    val slideOut =
                                        slideOutHorizontally(animationSpec = tween(250)) {
                                            if (animationDirection ==
                                                1
                                            ) {
                                                -it
                                            } else {
                                                it
                                            }
                                        }
                                    (slideIn togetherWith slideOut).using(SizeTransform(clip = false))
                                },
                                label = "mode_icon_animation",
                            ) { mode ->
                                val icon =
                                    when (mode) {
                                        InputMode.AddGoal -> Icons.Outlined.Add
                                        InputMode.AddQuickRecord -> Icons.Outlined.Inbox
                                        InputMode.SearchInList -> Icons.Outlined.Search
                                        InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
                                        InputMode.AddProjectLog -> Icons.Outlined.PostAdd
                                    }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier =
                                        Modifier
                                            .size(22.dp)
                                            .graphicsLayer { rotationZ = if (isPressed) (dragOffset / 20f).coerceIn(-15f, 15f) else 0f },
                                )
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .background(color = panelColors.accentColor, shape = CircleShape)
                                .padding(1.dp)
                                .background(color = panelColors.contentColor.copy(alpha = 0.3f), shape = CircleShape),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 3)
                            .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = panelColors.inputFieldColor,
                    border = BorderStroke(1.dp, panelColors.accentColor.copy(alpha = 0.3f)),
                    shadowElevation = 1.dp,
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = onValueChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .focusRequester(focusRequester),
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = panelColors.contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (inputValue.text.isNotBlank()) onSubmit() }),
                        singleLine = false,
                        cursorBrush = SolidColor(panelColors.accentColor),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (inputValue.text.isEmpty()) {
                                        Text(
                                            text =
                                                when (inputMode) {
                                                    InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                                                    InputMode.AddQuickRecord -> stringResource(R.string.hint_add_quick_record)
                                                    InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                                                    InputMode.SearchGlobal -> stringResource(R.string.hint_search_global)
                                                    InputMode.AddProjectLog -> "Додати коментар до проекту..."
                                                },
                                            style =
                                                MaterialTheme.typography.bodyLarge.copy(
                                                    color = panelColors.contentColor.copy(alpha = 0.7f),
                                                    fontSize = 16.sp,
                                                ),
                                        )
                                    }
                                    innerTextField()
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    NerIndicator(
                                        isActive = isNerActive,
                                        hasText = inputValue.text.isNotBlank(),
                                    )

                                    AnimatedVisibility(
                                        visible = inputValue.text.isNotBlank(),
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        IconButton(
                                            onClick = { onValueChange(TextFieldValue("")) },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.clear_input),
                                                tint = panelColors.contentColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedVisibility(
                    visible = inputValue.text.isNotBlank(),
                    enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    IconButton(
                        onClick = onSubmit,
                        modifier =
                            Modifier
                                .size(44.dp)
                                .background(color = panelColors.accentColor, shape = CircleShape),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor =
                                    when (inputMode) {
                                        InputMode.AddGoal, InputMode.AddQuickRecord -> MaterialTheme.colorScheme.onPrimary
                                        InputMode.SearchInList -> MaterialTheme.colorScheme.onPrimary
                                        InputMode.SearchGlobal -> MaterialTheme.colorScheme.onTertiary
                                        InputMode.AddProjectLog -> MaterialTheme.colorScheme.onSecondary
                                    },
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
    if (showModeMenu) {
        InputModeSelectionDialog(
            currentInputMode = inputMode,
            isProjectManagementEnabled = isProjectManagementEnabled,
            onDismiss = { showModeMenu = false },
            onInputModeSelected = onInputModeSelected,
            onAddListLinkClick = onAddListLinkClick,
            onShowAddWebLinkDialog = onShowAddWebLinkDialog,
            onShowAddObsidianLinkDialog = onShowAddObsidianLinkDialog,
            onAddListShortcutClick = onAddListShortcutClick
        )
    }
}

@Composable
private fun NerIndicator(
    isActive: Boolean,
    hasText: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isActive && hasText,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "ner_indicator_transition")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "ner_indicator_scale",
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 800),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "ner_indicator_alpha",
        )

        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Smart recognition active",
            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha),
            modifier =
                Modifier
                    .size(18.dp)
                    .scale(scale),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NavigationBar(
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    currentView: ProjectViewMode,
    onViewChange: (ProjectViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
    contentColor: Color,
    onRecentsClick: () -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    modifier: Modifier = Modifier,
    onImportBacklogFromMarkdown: () -> Unit,
    onExportBacklogToMarkdown: () -> Unit,
    onExportProjectState: () -> Unit,
    onStartTrackingCurrentProject: () -> Unit,
    isProjectManagementEnabled: Boolean,
    onToggleProjectManagement: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val attachmentIconScale by animateFloatAsState(
        targetValue = if (isAttachmentsExpanded) 1.2f else 1.0f,
        label = "attachmentIconScale",
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val backButtonAlpha by animateFloatAsState(
                targetValue = if (canGoBack) 1f else 0.4f,
                label = "backButtonAlpha",
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .alpha(backButtonAlpha)
                        .clip(CircleShape)
                        .combinedClickable(
                            enabled = canGoBack,
                            onClick = onBackClick,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRecentsClick()
                            },
                        ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = if (canGoBack) contentColor else contentColor.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onForwardClick,
                enabled = false,
                modifier =
                    Modifier
                        .size(40.dp)
                        .alpha(0.38f),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.forward),
                    tint = contentColor.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.go_to_home_list),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
            ) {
                IconButton(
                    onClick = onRecentsClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = stringResource(R.string.recents),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = contentColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)),
            ) {
                Row(
                    modifier = Modifier.height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            onViewChange(ProjectViewMode.BACKLOG)
                            onInputModeSelected(InputMode.AddGoal)
                        },
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(
                                    color =
                                        if (currentView ==
                                            ProjectViewMode.BACKLOG
                                        ) {
                                            contentColor.copy(alpha = 0.2f)
                                        } else {
                                            Color.Transparent
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.List,
                            contentDescription = "Backlog",
                            modifier = Modifier.size(18.dp),
                            tint = contentColor,
                        )
                    }
                    IconButton(
                        onClick = {
                            onViewChange(ProjectViewMode.INBOX)
                            onInputModeSelected(InputMode.AddQuickRecord)
                        },
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(
                                    color =
                                        if (currentView ==
                                            ProjectViewMode.INBOX
                                        ) {
                                            contentColor.copy(alpha = 0.2f)
                                        } else {
                                            Color.Transparent
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = "Inbox",
                            modifier = Modifier.size(18.dp),
                            tint = contentColor,
                        )
                    }
                    if (isProjectManagementEnabled) {
                        IconButton(
                            onClick = {
                                onViewChange(ProjectViewMode.DASHBOARD)
                                onInputModeSelected(InputMode.AddQuickRecord)
                            },
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(
                                        color =
                                            if (currentView == ProjectViewMode.DASHBOARD) {
                                                contentColor.copy(
                                                    alpha = 0.2f,
                                                )
                                            } else {
                                                Color.Transparent
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard",
                                modifier = Modifier.size(18.dp),
                                tint = contentColor,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            val attachmentIconColor by animateColorAsState(
                targetValue = if (isAttachmentsExpanded) contentColor else contentColor.copy(alpha = 0.7f),
                label = "attachmentIconColor",
            )
            IconButton(
                onClick = {
                    // --- Початок логування ---
                    Log.d("ATTACHMENT_DEBUG", "--- Attachment Button Clicked ---")
                    Log.d("ATTACHMENT_DEBUG", "Initial state: currentView = $currentView, isAttachmentsExpanded = $isAttachmentsExpanded")

                    val comingFromAnotherView = currentView == ProjectViewMode.INBOX || currentView == ProjectViewMode.DASHBOARD
                    Log.d("ATTACHMENT_DEBUG", "comingFromAnotherView = $comingFromAnotherView")
                    // --- Кінець логування ---

                    // Крок 1: Якщо ми не в беклозі, перемкнутися на нього.
                    if (comingFromAnotherView) {
                        Log.d("ATTACHMENT_DEBUG", "ACTION: Calling onViewChange(BACKLOG).") // Лог
                        onViewChange(ProjectViewMode.BACKLOG)
                        onInputModeSelected(InputMode.AddGoal)
                    }

                    // Крок 2: Вирішити, чи потрібно показувати вкладення.
                    if (comingFromAnotherView) {
                        Log.d("ATTACHMENT_DEBUG", "DECISION: Switched view. Goal is to SHOW attachments.") // Лог
                        if (!isAttachmentsExpanded) {
                            Log.d("ATTACHMENT_DEBUG", "ACTION: Attachments are hidden, calling onToggleAttachments() to SHOW them.") // Лог
                            onToggleAttachments()
                        } else {
                            Log.d("ATTACHMENT_DEBUG", "ACTION: Attachments are already expanded, DOING NOTHING.") // Лог
                        }
                    } else {
                        Log.d("ATTACHMENT_DEBUG", "DECISION: Already in backlog. Calling onToggleAttachments() to TOGGLE.") // Лог
                        onToggleAttachments()
                    }
                    Log.d("ATTACHMENT_DEBUG", "--- Click Handler Finished ---") // Лог
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Attachment,
                    contentDescription = stringResource(R.string.toggle_attachments),
                    tint = attachmentIconColor,
                    modifier =
                        Modifier
                            .size(20.dp)
                            .scale(attachmentIconScale),
                )
            }

            Box {
                IconButton(
                    onClick = { onMenuExpandedChange(true) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) },
                    properties = PopupProperties(focusable = true),
                    modifier =
                        Modifier
                            .width(280.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(16.dp),
                            ),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.edit_list),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onEditList()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Toggle realization support",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onToggleProjectManagement()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Construction,
                                contentDescription = "Toggle realization support",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Start tracking current project",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onStartTrackingCurrentProject()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.share_list),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onShareList()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )

                    if (currentView == ProjectViewMode.INBOX) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Імпортувати з Markdown",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                onImportFromMarkdown()
                                onMenuExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Експортувати в Markdown",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                onExportToMarkdown()
                                onMenuExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }

                    if (currentView == ProjectViewMode.BACKLOG) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Імпортувати беклог з Markdown",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                onImportBacklogFromMarkdown()
                                onMenuExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Експортувати беклог в Markdown",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                onExportBacklogToMarkdown()
                                onMenuExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }

                    if (isProjectManagementEnabled) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Експортувати історію і стан",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                onExportProjectState()
                                onMenuExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Assessment,
                                    contentDescription = "Експортувати історію і стан",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete_list),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onDeleteList()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
