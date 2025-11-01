

package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GoalTransportMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onCreateInstanceRequest: () -> Unit,
    onMoveInstanceRequest: () -> Unit,
    onCopyGoalRequest: () -> Unit,
    onCopyContentToClipboardRequest: () -> Unit,
    isGoalItem: Boolean,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false,
                ),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрити",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TransportMenuItem(
                            icon = Icons.AutoMirrored.Filled.Send,
                            title = "Перемістити",
                            description = "Перемістити елемент в інший проект",
                            onClick = {
                                onMoveInstanceRequest()
                                onDismiss()
                            },
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        TransportMenuItem(
                            icon = Icons.Default.ContentCopy,
                            title = "Копіювати",
                            description = "Створити копію цього підпроекту в іншому проекті",
                            onClick = {
                                onCopyGoalRequest()
                                onDismiss()
                            },
                        )

                        if (isGoalItem) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            TransportMenuItem(
                                icon = Icons.Default.ContentCopy,
                                title = "Клонувати ціль",
                                description = "Створити копію цієї цілі",
                                onClick = {
                                    onCopyGoalRequest()
                                    onDismiss()
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}


@Composable
private fun TransportMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "scaleAnim",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {
                        pressed = true
                        onClick()
                        pressed = false
                    },
                ),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
