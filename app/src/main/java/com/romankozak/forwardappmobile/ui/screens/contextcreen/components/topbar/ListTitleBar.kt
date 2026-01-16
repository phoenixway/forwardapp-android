package com.romankozak.forwardappmobile.ui.screens.contextcreen.components.topbar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import kotlinx.coroutines.delay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

// Single StatusVisuals data class
private data class StatusVisuals(
    val emoji: String,
    val color: Color
)

@Composable
internal fun getViewModeText(viewMode: ProjectViewMode): String =
    when (viewMode) {
        ProjectViewMode.BACKLOG -> "Backlog"
        ProjectViewMode.INBOX -> "Inbox"
        ProjectViewMode.ADVANCED -> "Advanced View"
        ProjectViewMode.ATTACHMENTS -> "Attachments"
        ProjectViewMode.DASHBOARD -> "Dashboard"
    }

@Composable
private fun getStatusVisuals(status: String): StatusVisuals =
    when (status) {
        ProjectStatusValues.NO_PLAN -> StatusVisuals("⚠️", Color(0xFFFF9800).copy(alpha = 0.3f))
        ProjectStatusValues.PLANNING -> StatusVisuals("📝", Color(0xFF9C27B0).copy(alpha = 0.3f))
        ProjectStatusValues.IN_PROGRESS -> StatusVisuals("▶️", Color(0xFF2196F3).copy(alpha = 0.3f))
        ProjectStatusValues.COMPLETED -> StatusVisuals("✅", Color(0xFF4CAF50).copy(alpha = 0.3f))
        ProjectStatusValues.ON_HOLD -> StatusVisuals("⏸️", Color(0xFFFF9800).copy(alpha = 0.3f))
        ProjectStatusValues.PAUSED -> StatusVisuals("⏳", Color(0xFFFFC107).copy(alpha = 0.3f))
        else -> StatusVisuals("", Color.Transparent)
    }

@Composable
private fun BriefStatusIndicator(
    status: String,
    modifier: Modifier = Modifier,
) {
    val visuals = getStatusVisuals(status = status)

    Box(
        modifier = modifier
            .size(20.dp)
            .background(
                color = visuals.color.copy(alpha = 0.8f),
                shape = RoundedCornerShape(6.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = visuals.emoji,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun ViewModeIndicator(
    viewMode: ProjectViewMode,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = when (viewMode) {
        ProjectViewMode.BACKLOG -> Pair(
            Color(0xFFE8F5E9).copy(alpha = 0.7f),
            Color(0xFF2E7D32).copy(alpha = 0.8f)
        )
        ProjectViewMode.INBOX -> Pair(
            Color(0xFFE3F2FD).copy(alpha = 0.7f),
            Color(0xFF1565C0).copy(alpha = 0.8f)
        )
        ProjectViewMode.ADVANCED -> Pair(
            Color(0xFFF3E5F5).copy(alpha = 0.7f),
            Color(0xFF7B1FA2).copy(alpha = 0.8f)
        )
        ProjectViewMode.ATTACHMENTS -> Pair(
            Color(0xFFF5F5F5).copy(alpha = 0.7f),
            Color(0xFF616161).copy(alpha = 0.8f)
        )
        ProjectViewMode.DASHBOARD -> Pair(
            Color(0xFFFFF3E0).copy(alpha = 0.7f),
            Color(0xFFEF6C00).copy(alpha = 0.8f)
        )
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = getViewModeText(viewMode),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = textColor,
        )
    }
}

// Enhanced ProjectStatusIndicator with animations
@Composable
private fun ProjectStatusIndicator(
    status: String,
    statusText: String?,
    modifier: Modifier = Modifier,
) {
    val visuals = getStatusVisuals(status = status)
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(400, easing = EaseOut),
        ) + slideInVertically(
            animationSpec = tween(400, easing = EaseOut),
            initialOffsetY = { it / 3 },
        ),
        exit = fadeOut() + slideOutVertically(),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                )
                .border(
                    width = 0.5.dp,
                    color = visuals.color.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .alpha(0.85f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = visuals.emoji,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    Text(
                        text = ProjectStatusValues.getDisplayName(status),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }

                AnimatedVisibility(
                    visible = !statusText.isNullOrBlank(),
                    enter = fadeIn(
                        animationSpec = tween(250, delayMillis = 100),
                    ) + expandVertically(
                        animationSpec = tween(250, delayMillis = 100),
                    ),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = statusText ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🙂",
                    fontSize = 10.sp,
                    modifier = Modifier.alpha(0.7f),
                )
            }
        }
    }
}

@Composable
fun ListTitleBar(
    modifier: Modifier = Modifier,
    project: Project?,
    currentViewMode: ProjectViewMode? = null,
    onInboxClick: () -> Unit,
) {
    var isStatusExpanded by remember { mutableStateOf(false) }

    val isProjectManagementActive = (project?.isProjectManagementEnabled == true) &&
            (project.projectStatus != null)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isProjectManagementActive) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                isStatusExpanded = !isStatusExpanded
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = project?.name ?: stringResource(id = R.string.loading),
                    modifier = Modifier.weight(1f, fill = false),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (currentViewMode != null) {
                    Spacer(Modifier.width(8.dp))
                    ViewModeIndicator(viewMode = currentViewMode)
                }

                if (isProjectManagementActive) {
                    Spacer(Modifier.width(8.dp))
                    BriefStatusIndicator(status = project.projectStatus!!)
                }

                Spacer(Modifier.width(8.dp))

                IconButton(onClick = onInboxClick) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Inbox"
                    )
                }
            }

            AnimatedVisibility(visible = isStatusExpanded) {
                if (project?.projectStatus != null) {
                    ProjectStatusIndicator(
                        status = project.projectStatus,
                        statusText = project.projectStatusText,
                    )
                }
            }
        }
    }
}
