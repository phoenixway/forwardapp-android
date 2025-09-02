package com.romankozak.forwardappmobile.ui.screens.backlog.components

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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import kotlinx.coroutines.delay
import kotlin.math.abs

private val modes = listOf(InputMode.SearchInList, InputMode.SearchGlobal, InputMode.AddGoal, InputMode.AddQuickRecord)

private data class PanelColors(
    val containerColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val inputFieldColor: Color
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
) {
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableIntStateOf(1) }

    val currentModeIndex = modes.indexOf(inputMode)

    val panelColors = when (inputMode) {
        InputMode.AddGoal -> PanelColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            accentColor = MaterialTheme.colorScheme.primary,
            inputFieldColor = MaterialTheme.colorScheme.surfaceVariant
        )
        InputMode.AddQuickRecord -> PanelColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            accentColor = MaterialTheme.colorScheme.secondary,
            inputFieldColor = MaterialTheme.colorScheme.surface
        )
        InputMode.SearchInList -> PanelColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            accentColor = MaterialTheme.colorScheme.primary,
            inputFieldColor = MaterialTheme.colorScheme.surface
        )
        InputMode.SearchGlobal -> PanelColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            accentColor = MaterialTheme.colorScheme.tertiary,
            inputFieldColor = MaterialTheme.colorScheme.surface
        )
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = panelColors.containerColor,
        animationSpec = tween(400),
        label = "panel_color_animation"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    LaunchedEffect(inputMode) {
        if (inputMode != InputMode.AddGoal) {
            delay(60)
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = animatedContainerColor,
        border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f))
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
                onExportBacklogToMarkdown = onExportBacklogToMarkdown
            )

            Row(
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Surface(
                        onClick = { showModeMenu = true },
                        shape = CircleShape,
                        color = panelColors.contentColor.copy(alpha = 0.1f),
                        contentColor = panelColors.contentColor,
                        modifier = Modifier
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
                                    }
                                ) { _, dragAmount -> dragOffset += dragAmount }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = inputMode,
                                transitionSpec = {
                                    val slideIn = slideInHorizontally(animationSpec = tween(250)) { if (animationDirection == 1) it else -it }
                                    val slideOut = slideOutHorizontally(animationSpec = tween(250)) { if (animationDirection == 1) -it else it }
                                    (slideIn togetherWith slideOut).using(SizeTransform(clip = false))
                                },
                                label = "mode_icon_animation"
                            ) { mode ->
                                val icon = when (mode) {
                                    InputMode.AddGoal -> Icons.Outlined.Add
                                    InputMode.AddQuickRecord -> Icons.Outlined.Inbox
                                    InputMode.SearchInList -> Icons.Outlined.Search
                                    InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer { rotationZ = if (isPressed) (dragOffset / 20f).coerceIn(-15f, 15f) else 0f }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(color = panelColors.accentColor, shape = CircleShape)
                            .padding(1.dp)
                            .background(color = panelColors.contentColor.copy(alpha = 0.3f), shape = CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = panelColors.inputFieldColor,
                    border = BorderStroke(1.dp, panelColors.accentColor.copy(alpha = 0.3f)),
                    shadowElevation = 1.dp
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = panelColors.contentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (inputValue.text.isNotBlank()) onSubmit() }),
                        singleLine = true,
                        cursorBrush = SolidColor(panelColors.accentColor),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputValue.text.isEmpty()) {
                                    Text(
                                        text = when (inputMode) {
                                            InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                                            InputMode.AddQuickRecord -> stringResource(R.string.hint_add_quick_record)
                                            InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                                            InputMode.SearchGlobal -> stringResource(R.string.hint_search_global)
                                        },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = panelColors.contentColor.copy(alpha = 0.7f),
                                            fontSize = 16.sp
                                        ),
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedVisibility(
                    visible = inputValue.text.isNotBlank() && (inputMode == InputMode.AddGoal || inputMode == InputMode.AddQuickRecord),
                    enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    IconButton(
                        onClick = onSubmit,
                        modifier = Modifier
                            .size(44.dp)
                            .background(color = panelColors.accentColor, shape = CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showModeMenu) {
        val menuWidth = 280.dp


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
        ) {
            DropdownMenu(
                expanded = showModeMenu,
                onDismissRequest = { showModeMenu = false },

                offset = DpOffset(x = -menuWidth / 2, y = 0.dp),
                modifier = Modifier
                    .width(menuWidth)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {

                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (inputMode) {
                                InputMode.AddGoal -> stringResource(R.string.menu_add_goal_component)
                                InputMode.AddQuickRecord -> stringResource(R.string.menu_add_quick_record)
                                InputMode.SearchInList -> stringResource(R.string.menu_search_in_list)
                                InputMode.SearchGlobal -> stringResource(R.string.menu_search_everywhere)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = panelColors.accentColor
                        )
                    },
                    onClick = { /* Do nothing */ },
                    modifier = Modifier.background(panelColors.contentColor.copy(alpha = 0.08f))
                )


                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "ПОШУК",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_search_in_list), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(20.dp), tint = if (inputMode == InputMode.SearchInList) panelColors.accentColor else MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onInputModeSelected(InputMode.SearchInList)
                        showModeMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_search_everywhere), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.TravelExplore, null, modifier = Modifier.size(20.dp), tint = if (inputMode == InputMode.SearchGlobal) panelColors.accentColor else MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onInputModeSelected(InputMode.SearchGlobal)
                        showModeMenu = false
                    }
                )


                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "ДОДАВАННЯ ПОСИЛАНЬ",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_list_link), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        showModeMenu = false
                        onAddListLinkClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_web_link), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Public, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        showModeMenu = false
                        onShowAddWebLinkDialog()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_obsidian_link), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.DataObject, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        showModeMenu = false
                        onShowAddObsidianLinkDialog()
                    }
                )



                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "ДОДАВАННЯ В БЕКЛОГ",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_list_shortcut), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onAddListShortcutClick()
                        showModeMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_goal_component), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp), tint = if (inputMode == InputMode.AddGoal) panelColors.accentColor else MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onInputModeSelected(InputMode.AddGoal)
                        showModeMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_quick_record), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Inbox, null, modifier = Modifier.size(20.dp), tint = if (inputMode == InputMode.AddQuickRecord) panelColors.accentColor else MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onInputModeSelected(InputMode.AddQuickRecord)
                        showModeMenu = false
                    }
                )
            }
        }
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
    onExportBacklogToMarkdown: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val attachmentIconScale by animateFloatAsState(
        targetValue = if (isAttachmentsExpanded) 1.2f else 1.0f,
        label = "attachmentIconScale",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val backButtonAlpha by animateFloatAsState(
                targetValue = if (canGoBack) 1f else 0.4f,
                label = "backButtonAlpha"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .alpha(backButtonAlpha)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = canGoBack,
                        onClick = onBackClick,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRecentsClick()
                        }
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = if (canGoBack) contentColor else contentColor.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onForwardClick,
                enabled = false,
                modifier = Modifier
                    .size(40.dp)
                    .alpha(0.38f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.forward),
                    tint = contentColor.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.go_to_home_list),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            // --- ПОЧАТОК ЗМІНИ ---
            IconButton(
                onClick = onRecentsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Останні", // Recent
                    tint = contentColor.copy(alpha = 0.8f), // "Ненав'язливий" вигляд
                    modifier = Modifier.size(20.dp)
                )
            }
            // --- КІНЕЦЬ ЗМІНИ ---
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = contentColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.height(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onViewChange(ProjectViewMode.BACKLOG)
                            onInputModeSelected(InputMode.AddGoal)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (currentView == ProjectViewMode.BACKLOG) contentColor.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.List,
                            contentDescription = "Backlog",
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    }
                    IconButton(
                        onClick = {
                            onViewChange(ProjectViewMode.INBOX)
                            onInputModeSelected(InputMode.AddQuickRecord)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (currentView == ProjectViewMode.INBOX) contentColor.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = "Inbox",
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            val attachmentIconColor by animateColorAsState(
                targetValue = if (isAttachmentsExpanded) contentColor else contentColor.copy(alpha = 0.7f),
                label = "attachmentIconColor"
            )
            IconButton(
                onClick = {
                    if (currentView == ProjectViewMode.INBOX) {
                        onViewChange(ProjectViewMode.BACKLOG)
                        onInputModeSelected(InputMode.AddGoal)
                    }
                    onToggleAttachments()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Attachment,
                    contentDescription = stringResource(R.string.toggle_attachments),
                    tint = attachmentIconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(attachmentIconScale)
                )
            }
            Box {
                IconButton(
                    onClick = { onMenuExpandedChange(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) },
                    modifier = Modifier
                        .width(240.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.edit_list),
                                style = MaterialTheme.typography.bodyMedium
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.share_list),
                                style = MaterialTheme.typography.bodyMedium
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    if (currentView == ProjectViewMode.INBOX) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Імпортувати з Markdown",
                                    style = MaterialTheme.typography.bodyMedium
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Експортувати в Markdown",
                                    style = MaterialTheme.typography.bodyMedium
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    if (currentView == ProjectViewMode.BACKLOG) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Імпортувати беклог з Markdown",
                                    style = MaterialTheme.typography.bodyMedium
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Експортувати беклог в Markdown",
                                    style = MaterialTheme.typography.bodyMedium
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete_list),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}