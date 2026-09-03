@file:Suppress("MagicNumber", "UnusedPrivateProperty", "PackageNaming", "LongMethod")

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import kotlinx.coroutines.delay

// Single StatusVisuals data class
private data class StatusVisuals(
    val emoji: String,
    val color: Color,
)

private const val STATUS_COLOR_ALPHA = 0.3f
private const val BRIEF_BACKGROUND_ALPHA = 0.8f
private const val TEXT_SECONDARY_ALPHA = 0.9f
private const val VIEW_MODE_BACKGROUND_ALPHA = 0.7f
private const val VIEW_MODE_TEXT_ALPHA = 0.8f
private const val SURFACE_CARD_ALPHA = 0.4f
private const val BORDER_ALPHA = 0.6f
private const val STATUS_PANEL_ALPHA = 0.85f
private const val ICON_CONTAINER_ALPHA = 0.5f
private const val EMOJI_ALPHA = 0.7f
private const val ON_SURFACE_MUTED_ALPHA = 0.6f
private const val ON_SURFACE_PRIMARY_ALPHA = 0.8f
private const val STATUS_DELAY_MS = 300L
private const val STATUS_ANIMATION_MS = 400
private const val STATUS_HINT_ANIMATION_MS = 250
private const val STATUS_HINT_ANIMATION_DELAY_MS = 100
private const val STATUS_ENTER_DIVIDER = 3

private val StatusNoPlanColor = Color(0xFFFF9800)
private val StatusPlanningColor = Color(0xFF9C27B0)
private val StatusInProgressColor = Color(0xFF2196F3)
private val StatusCompletedColor = Color(0xFF4CAF50)
private val StatusPausedColor = Color(0xFFFFC107)
private val ViewModeBacklogBg = Color(0xFFE8F5E9)
private val ViewModeBacklogFg = Color(0xFF2E7D32)
private val ViewModeInboxBg = Color(0xFFE3F2FD)
private val ViewModeInboxFg = Color(0xFF1565C0)
private val ViewModeAdvancedBg = Color(0xFFF3E5F5)
private val ViewModeAdvancedFg = Color(0xFF7B1FA2)
private val ViewModeConnectionsBg = Color(0xFFF5F5F5)
private val ViewModeConnectionsFg = Color(0xFF616161)
private val ViewModeDashboardBg = Color(0xFFFFF3E0)
private val ViewModeDashboardFg = Color(0xFFEF6C00)
private val ViewModeDirectionBg = Color(0xFFE0F7FA)
private val ViewModeDirectionFg = Color(0xFF00838F)
private val ViewModeLogBg = Color(0xFFE0F2F7)
private val ViewModeLogFg = Color(0xFF0277BD)
private val ViewModeJournalBg = Color(0xFFF4E7FF)
private val ViewModeJournalFg = Color(0xFF7B1FA2)
private val ViewModeProblemsBg = Color(0xFFFFEBEE)
private val ViewModeProblemsFg = Color(0xFFC62828)
private val ViewModeNotesBg = Color(0xFFFFFDE7)
private val ViewModeNotesFg = Color(0xFFFBC02D)
private val ViewModeVetCaseBg = Color(0xFFE8F5E9)
private val ViewModeVetCaseFg = Color(0xFF388E3C)

@Composable
internal fun getViewModeText(viewMode: ContextViewMode): String =
    when (viewMode) {
        ContextViewMode.BACKLOG -> "Backlog"
        ContextViewMode.INBOX -> "Inbox"
        ContextViewMode.CONNECTIONS -> "Connections"
        ContextViewMode.DASHBOARD -> "Dashboard"
        ContextViewMode.DIRECTION -> "Directions"
        ContextViewMode.LOG -> "Log"
        ContextViewMode.KEY_PROBLEMS -> "Issues"
        ContextViewMode.ADVANCED,
        ContextViewMode.NOTES,
        ContextViewMode.VET_CASE,
        -> "Unavailable"
    }

@Composable
private fun getStatusVisuals(status: String): StatusVisuals =
    when (status) {
        ContextStatusValues.NO_PLAN -> StatusVisuals("⚠️", StatusNoPlanColor.copy(alpha = STATUS_COLOR_ALPHA))
        ContextStatusValues.PLANNING -> StatusVisuals("📝", StatusPlanningColor.copy(alpha = STATUS_COLOR_ALPHA))
        ContextStatusValues.IN_PROGRESS -> StatusVisuals("▶️", StatusInProgressColor.copy(alpha = STATUS_COLOR_ALPHA))
        ContextStatusValues.COMPLETED -> StatusVisuals("✅", StatusCompletedColor.copy(alpha = STATUS_COLOR_ALPHA))
        ContextStatusValues.ON_HOLD -> StatusVisuals("⏸️", StatusNoPlanColor.copy(alpha = STATUS_COLOR_ALPHA))
        ContextStatusValues.PAUSED -> StatusVisuals("⏳", StatusPausedColor.copy(alpha = STATUS_COLOR_ALPHA))
        else -> StatusVisuals("", Color.Transparent)
    }

@Composable
private fun BriefStatusIndicator(
    status: String,
    modifier: Modifier = Modifier,
) {
    val visuals = getStatusVisuals(status = status)

    Box(
        modifier =
            modifier
                .size(20.dp)
                .background(
                    color = visuals.color.copy(alpha = BRIEF_BACKGROUND_ALPHA),
                    shape = RoundedCornerShape(6.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = visuals.emoji,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = TEXT_SECONDARY_ALPHA),
        )
    }
}

@Composable
private fun ViewModeIndicator(
    viewMode: ContextViewMode,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) =
        when (viewMode) {
            ContextViewMode.BACKLOG ->
                Pair(
                    ViewModeBacklogBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeBacklogFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.INBOX ->
                Pair(
                    ViewModeInboxBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeInboxFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.CONNECTIONS ->
                Pair(
                    ViewModeConnectionsBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeConnectionsFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.DASHBOARD ->
                Pair(
                    ViewModeDashboardBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeDashboardFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.DIRECTION ->
                Pair(
                    ViewModeDirectionBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeDirectionFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.LOG ->
                Pair(
                    ViewModeLogBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeLogFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.KEY_PROBLEMS ->
                Pair(
                    ViewModeProblemsBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeProblemsFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
            ContextViewMode.ADVANCED,
            ContextViewMode.NOTES,
            ContextViewMode.VET_CASE ->
                Pair(
                    ViewModeAdvancedBg.copy(alpha = VIEW_MODE_BACKGROUND_ALPHA),
                    ViewModeAdvancedFg.copy(alpha = VIEW_MODE_TEXT_ALPHA),
                )
        }

    Box(
        modifier =
            modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = getViewModeText(viewMode),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                ),
            color = textColor,
        )
    }
}

// Enhanced contextStatusIndicator with animations
@Composable
private fun contextStatusIndicator(
    status: String,
    statusText: String?,
    modifier: Modifier = Modifier,
) {
    val visuals = getStatusVisuals(status = status)
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        delay(STATUS_DELAY_MS)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter =
            fadeIn(
                animationSpec = tween(STATUS_ANIMATION_MS, easing = EaseOut),
            ) +
                slideInVertically(
                    animationSpec = tween(STATUS_ANIMATION_MS, easing = EaseOut),
                    initialOffsetY = { it / STATUS_ENTER_DIVIDER },
                ),
        exit = fadeOut() + slideOutVertically(),
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = SURFACE_CARD_ALPHA),
                    )
                    .border(
                        width = 0.5.dp,
                        color = visuals.color.copy(alpha = BORDER_ALPHA),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .alpha(STATUS_PANEL_ALPHA),
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
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                letterSpacing = 0.1.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ON_SURFACE_MUTED_ALPHA),
                    )
                    Text(
                        text = visuals.emoji,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ON_SURFACE_PRIMARY_ALPHA),
                    )
                    Text(
                        text = ContextStatusValues.getDisplayName(status),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                letterSpacing = 0.1.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ON_SURFACE_PRIMARY_ALPHA),
                    )
                }

                AnimatedVisibility(
                    visible = !statusText.isNullOrBlank(),
                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    STATUS_HINT_ANIMATION_MS,
                                    delayMillis = STATUS_HINT_ANIMATION_DELAY_MS,
                                ),
                        ) +
                            expandVertically(
                                animationSpec =
                                    tween(
                                        STATUS_HINT_ANIMATION_MS,
                                        delayMillis = STATUS_HINT_ANIMATION_DELAY_MS,
                                    ),
                            ),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = statusText ?: "",
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ON_SURFACE_MUTED_ALPHA),
                        maxLines = 2,
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .background(
                            color =
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = ICON_CONTAINER_ALPHA,
                                ),
                            shape = RoundedCornerShape(10.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🙂",
                    fontSize = 10.sp,
                    modifier = Modifier.alpha(EMOJI_ALPHA),
                )
            }
        }
    }
}

@Composable
fun ListTitleBar(
    modifier: Modifier = Modifier,
    project: Context?,
    currentViewMode: ContextViewMode? = null,
    onInboxClick: () -> Unit,
    onPasteClick: (() -> Unit)? = null,
) {
    var isStatusExpanded by remember { mutableStateOf(false) }

    val isProjectManagementActive =
        (project?.isContextManagementEnabled == true) &&
            (project.contextStatus != null)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier =
                    Modifier
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
                            },
                        )
                        .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = project?.name ?: stringResource(id = R.string.loading),
                    modifier = Modifier.weight(1f, fill = false),
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
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
                    BriefStatusIndicator(status = project.contextStatus!!)
                }

                Spacer(Modifier.width(8.dp))

                if (onPasteClick != null) {
                    IconButton(onClick = onPasteClick) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                        )
                    }
                }

                IconButton(onClick = onInboxClick) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Inbox",
                    )
                }
            }
            AnimatedVisibility(visible = isStatusExpanded) {
                // Використовуємо .let для створення локальної копії 'status'
                project?.contextStatus?.let { status ->
                    contextStatusIndicator(
                        status = status, // Тепер тут гарантовано не-null String
                        statusText = project.contextStatusText,
                    )
                }
            }
        }
    }
}
