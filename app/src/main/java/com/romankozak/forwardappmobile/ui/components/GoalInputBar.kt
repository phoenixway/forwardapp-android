// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/GoalInputBar.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.ui.screens.goaldetail.InputMode
import kotlinx.coroutines.delay

private val modes = listOf(InputMode.AddGoal, InputMode.SearchInList, InputMode.SearchGlobal)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GoalInputBar(
    modifier: Modifier = Modifier,
    inputValue: TextFieldValue,
    inputMode: InputMode,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    onRecentsClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableStateOf(1) }

    val currentModeIndex = modes.indexOf(inputMode)

    // ✨ ЗМІНЕНО: Оновлена логіка кольорів для режиму "Додати" (Варіант 2)
    val (containerColor, contentColor, accentColor) = when (inputMode) {
        InputMode.AddGoal -> Triple(
            MaterialTheme.colorScheme.surfaceContainer, // Нейтральний фон
            MaterialTheme.colorScheme.primary,            // Яскравий акцент для контенту
            MaterialTheme.colorScheme.primary
        )
        InputMode.SearchInList -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary
        )
        InputMode.SearchGlobal -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiary
        )
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(400),
        label = "bar_color_animation"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    LaunchedEffect(inputMode) {
        delay(60)
        focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
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
                Surface(
                    onClick = { showModeMenu = true },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    contentColor = contentColor,
                    //border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .scale(buttonScale)
                        .size(48.dp)
                        .pointerInput(inputMode) {
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
                                            val prevIndex = (currentModeIndex - 1 + modes.size) % modes.size
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
                                dragOffset += dragAmount
                            }
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (kotlin.math.abs(dragOffset) > 15f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                if (dragOffset > 0) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                                Color.Transparent,
                                                if (dragOffset < 0) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                            )
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            )
                        }

                        AnimatedContent(
                            targetState = inputMode,
                            transitionSpec = {
                                val slideIn = when (animationDirection) {
                                    1 -> slideInHorizontally { it } + fadeIn(initialAlpha = 0.3f)
                                    else -> slideInHorizontally { -it } + fadeIn(initialAlpha = 0.3f)
                                }
                                val slideOut = when (animationDirection) {
                                    1 -> slideOutHorizontally { -it } + fadeOut(targetAlpha = 0.3f)
                                    else -> slideOutHorizontally { it } + fadeOut(targetAlpha = 0.3f)
                                }
                                (slideIn togetherWith slideOut).using(
                                    SizeTransform(clip = false)
                                )
                            },
                            label = "mode_icon_animation"
                        ) { mode ->
                            val icon = when (mode) {
                                InputMode.AddGoal -> Icons.Default.Add
                                InputMode.SearchInList -> Icons.Default.Search
                                InputMode.SearchGlobal -> Icons.Outlined.Search
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    modifier = Modifier.width(220.dp)
                ) {
                    modes.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (mode) {
                                        InputMode.AddGoal -> stringResource(R.string.mode_add_goal)
                                        InputMode.SearchInList -> stringResource(R.string.mode_search_in_list)
                                        InputMode.SearchGlobal -> stringResource(R.string.mode_search_global)
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                val icon = when (mode) {
                                    InputMode.AddGoal -> Icons.Default.Add
                                    InputMode.SearchInList -> Icons.Default.Search
                                    InputMode.SearchGlobal -> Icons.Outlined.Search
                                }
                                Icon(icon, null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                onInputModeSelected(mode)
                                showModeMenu = false
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                BasicTextField(
                    value = inputValue,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = contentColor
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputValue.text.isNotBlank()) onSubmit()
                    }),
                    singleLine = true,
                    cursorBrush = SolidColor(contentColor),
                    decorationBox = { innerTextField ->
                        if (inputValue.text.isEmpty()) {
                            Text(
                                text = when (inputMode) {
                                    InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                                    InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                                    InputMode.SearchGlobal -> stringResource(R.string.hint_search_global)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = contentColor.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                )
            }

            AnimatedVisibility(
                visible = inputValue.text.isNotBlank(),
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                FilledTonalIconButton(
                    onClick = onSubmit,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = contentColor.copy(alpha = 0.8f),
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