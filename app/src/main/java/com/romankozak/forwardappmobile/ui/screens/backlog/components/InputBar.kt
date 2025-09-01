package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import kotlinx.coroutines.delay
import kotlin.math.abs

private val modes = listOf(InputMode.SearchInList, InputMode.SearchGlobal, InputMode.AddGoal, InputMode.AddQuickRecord)
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoalInputBar(
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

    ) {
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableIntStateOf(1) }


    val currentModeIndex = modes.indexOf(inputMode)

    val (containerColor, contentColor, accentColor) = when (inputMode) {
        InputMode.AddGoal -> Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
        InputMode.AddQuickRecord -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
        InputMode.SearchInList -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondary)
        InputMode.SearchGlobal -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiary)
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(400),
        label = "bar_color_animation"
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
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 6.dp,
        tonalElevation = 4.dp,
        color = animatedContainerColor,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onRecentsClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(R.string.recent_lists),
                    tint = contentColor
                )
            }

            Box {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        Text(
                            text = stringResource(R.string.swipe_to_change_mode),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    },
                    state = rememberTooltipState()
                ) {
                    Surface(
                        onClick = { showModeMenu = true },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        contentColor = contentColor,
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(48.dp)
                            .pointerInput(inputMode) {
                                // --- ЗМІНЕНО: Оновлено структуру виклику ---
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isPressed = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onDragEnd = {
                                        isPressed = false
                                        val threshold = 50f
                                        when {
                                            dragOffset > threshold -> {
                                                animationDirection = -1
                                                // --- ЗМІНЕНО: Додано дужки для ясності ---
                                                val prevIndex = ((currentModeIndex - 1) + modes.size) % modes.size
                                                onInputModeSelected(modes[prevIndex])
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            dragOffset < -threshold -> {
                                                animationDirection = 1
                                                val nextIndex = (currentModeIndex + 1) % modes.size
                                                onInputModeSelected(modes[nextIndex])
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                        dragOffset = 0f
                                    }
                                ) { _, dragAmount ->
                                    // Ця лямбда тепер є останнім аргументом, а не іменованим 'onDrag'
                                    dragOffset += dragAmount
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = inputMode,
                                transitionSpec = {
                                    val slideIn = when (animationDirection) {
                                        1 -> slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(200))
                                        else -> slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(200))
                                    }
                                    val slideOut = when (animationDirection) {
                                        1 -> slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(200))
                                        else -> slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200))
                                    }
                                    (slideIn togetherWith slideOut).using(SizeTransform(clip = false))
                                },
                                label = "mode_icon_animation"
                            ) { mode ->
                                val icon = when (mode) {
                                    InputMode.AddGoal -> Icons.Outlined.Add
                                    InputMode.AddQuickRecord -> Icons.Outlined.Inbox // Or any other icon you prefer for the inbox
                                    InputMode.SearchInList -> Icons.Outlined.Search
                                    InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = if (isPressed) (dragOffset / 20f).coerceIn(-15f, 15f) else 0f }
                                )
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    modifier = Modifier.width(260.dp)
                ) {
                    // Group 1: Search
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_search_in_list), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onInputModeSelected(InputMode.SearchInList)
                            showModeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_search_everywhere), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Outlined.TravelExplore, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onInputModeSelected(InputMode.SearchGlobal)
                            showModeMenu = false
                        }
                    )

                    Divider()

                    // Group 2: Add Attachments
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_list_link)) },
                        leadingIcon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showModeMenu = false
                            onAddListLinkClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_web_link)) },
                        leadingIcon = { Icon(Icons.Outlined.Public, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showModeMenu = false
                            onShowAddWebLinkDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_obsidian_link)) },
                        leadingIcon = { Icon(Icons.Outlined.DataObject, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showModeMenu = false
                            onShowAddObsidianLinkDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_quick_record)) }, // Create this string resource
                        leadingIcon = { Icon(Icons.Outlined.Inbox, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onInputModeSelected(InputMode.AddQuickRecord)
                            showModeMenu = false
                        }
                    )

                    Divider()

                    // Group 3: Add Backlog Items
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_list_shortcut)) },
                        leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onAddListShortcutClick()
                            showModeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_goal_component), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onInputModeSelected(InputMode.AddGoal)
                            showModeMenu = false
                        }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = inputValue.text.isEmpty(),
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200)),
                ) {
                    Text(
                        text = when (inputMode) {
                            InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                            InputMode.AddQuickRecord -> stringResource(R.string.hint_add_quick_record) // Create this string resource
                            InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                            InputMode.SearchGlobal -> stringResource(R.string.hint_search_global)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                }

                BasicTextField(
                    value = inputValue,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (inputValue.text.isNotBlank()) onSubmit() }),
                    singleLine = true,
                    cursorBrush = SolidColor(contentColor),
                )
            }
            AnimatedVisibility(
                visible = inputValue.text.isNotBlank() && (inputMode == InputMode.AddGoal || inputMode == InputMode.AddQuickRecord),
                enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                FilledTonalIconButton(
                    onClick = onSubmit,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = accentColor,
                        contentColor = containerColor
                    )
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