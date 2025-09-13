package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ProjectStatus
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode

@Composable
private fun getStatusVisuals(status: ProjectStatus): StatusVisuals =
    when (status) {
        ProjectStatus.NO_PLAN -> StatusVisuals("⚠️", Color(0xFFFF9800).copy(alpha = 0.3f))
        ProjectStatus.PLANNING -> StatusVisuals("📝", Color(0xFF9C27B0).copy(alpha = 0.3f))
        ProjectStatus.IN_PROGRESS -> StatusVisuals("▶️", Color(0xFF2196F3).copy(alpha = 0.3f))
        ProjectStatus.COMPLETED -> StatusVisuals("✅", Color(0xFF4CAF50).copy(alpha = 0.3f))
        ProjectStatus.ON_HOLD -> StatusVisuals("⏸️", Color(0xFFFF9800).copy(alpha = 0.3f))
        ProjectStatus.PAUSED -> StatusVisuals("⏳", Color(0xFFFFC107).copy(alpha = 0.3f))
    }

@Composable
private fun BriefStatusIndicator(
    status: ProjectStatus,
    modifier: Modifier = Modifier,
) {
    val visuals = getStatusVisuals(status = status)

    Box(
        modifier =
            modifier
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
fun ListTitleBar(
    goalList: GoalList?,
    currentViewMode: ProjectViewMode? = null,
    modifier: Modifier = Modifier,
) {
    var isStatusExpanded by remember { mutableStateOf(false) }

    val isProjectManagementActive = goalList?.isProjectManagementEnabled == true && goalList.projectStatus != null

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
                        ).padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = goalList?.name ?: stringResource(id = R.string.loading),
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

                if (isProjectManagementActive) {
                    Spacer(Modifier.width(8.dp))
                    BriefStatusIndicator(status = goalList!!.projectStatus!!)
                }
            }
            val TAG = "LISTTITLE_DEBUG"
            Log.d(TAG, "currentViewMode ==" + currentViewMode.toString())
            if (currentViewMode != null) {
                Text(
                    text = getViewModeText(currentViewMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            AnimatedVisibility(visible = isStatusExpanded) {
                if (goalList?.projectStatus != null) {
                    ProjectStatusIndicator(
                        status = goalList.projectStatus,
                        statusText = goalList.projectStatusText,
                        viewMode = currentViewMode,
                    )
                }
            }
        }
    }
}

@Composable
private fun getViewModeText(viewMode: ProjectViewMode): String =
    when (viewMode) {
        ProjectViewMode.BACKLOG -> "Backlog"
        ProjectViewMode.INBOX -> "Inbox"
        ProjectViewMode.DASHBOARD -> "Dashboard"
    }
