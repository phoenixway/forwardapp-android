package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.features.sync.WifiSyncStatus
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHierarchyScreenTopAppBar(
    isSearchActive: Boolean,
    isFocusMode: Boolean,
    focusedProjectTitle: String?,
    focusedProjectMenuClick: (() -> Unit)?,
    focusedProjectOpenClick: (() -> Unit)?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onShowHistory: () -> Unit,
    onShowReminders: () -> Unit,
    syncStatus: WifiSyncStatus,
    onSyncIndicatorClick: () -> Unit,
    featureToggles: Map<FeatureFlag, Boolean>,
) {
    var swipeState by remember { mutableStateOf(0f) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .then(CommandDeckBackgroundModifier())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeState += dragAmount
                                },
                                onDragEnd = {
                                    if (swipeState > 50) {
                                        onGoBack()
                                    } else if (swipeState < -50) {
                                        onGoForward()
                                    }
                                    swipeState = 0f
                                },
                            )
                        },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFocusMode && focusedProjectTitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = focusedProjectTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier =
                                Modifier
                                    .weight(1f, fill = false)
                                    .let { base ->
                                        if (focusedProjectOpenClick != null) {
                                            base.clickable(onClick = focusedProjectOpenClick)
                                        } else {
                                            base
                                        }
                                    },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (focusedProjectOpenClick != null) {
                            IconButton(
                                onClick = focusedProjectOpenClick,
                                modifier = Modifier.size(36.dp).padding(start = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NorthEast,
                                    contentDescription = "Open project",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Contexts",
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (!isSearchActive) {
                    if (isFocusMode && focusedProjectMenuClick != null) {
                        IconButton(onClick = focusedProjectMenuClick) {
                            Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))
                        }
                    } else {
                        if (featureToggles[FeatureFlag.WifiSync] == true) {
                            SyncStatusIndicator(
                                status = syncStatus,
                                onClick = onSyncIndicatorClick,
                            )
                        }
                        AnimatedVisibility(visible = false) {
                            IconButton(onClick = onGoBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(id = R.string.back))
                            }
                        }
                        AnimatedVisibility(visible = false) {
                            IconButton(onClick = onGoForward) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(id = R.string.forward))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusIndicator(
    status: WifiSyncStatus,
    onClick: () -> Unit,
) {
    val baseColor =
        when (status) {
            WifiSyncStatus.Syncing -> MaterialTheme.colorScheme.primary
            is WifiSyncStatus.Error -> MaterialTheme.colorScheme.error
            WifiSyncStatus.Offline -> MaterialTheme.colorScheme.outline
            is WifiSyncStatus.ServerRunning -> MaterialTheme.colorScheme.tertiary
            WifiSyncStatus.Idle -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            WifiSyncStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val infiniteTransition = rememberInfiniteTransition(label = "sync_indicator_transition")
    val pulse =
        if (status is WifiSyncStatus.Syncing) {
            infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 650),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "sync_indicator_pulse",
            ).value
        } else {
            1f
        }

    Box(
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(18.dp)
                .graphicsLayer(
                    scaleX = pulse,
                    scaleY = pulse,
                )
                .clip(CircleShape)
                .background(baseColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { }
}
