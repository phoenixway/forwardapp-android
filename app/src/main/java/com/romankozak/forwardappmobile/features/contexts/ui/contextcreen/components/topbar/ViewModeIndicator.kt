/*
package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode

@Composable
public fun ViewModeIndicator(
    viewMode: ProjectViewMode,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, borderColor, textColor) =
        when (viewMode) {
            ProjectViewMode.BACKLOG ->
                Triple(
                    Color(0xFFE8F5E9).copy(alpha = 0.7f),
                    Color(0xFF81C784).copy(alpha = 0.4f),
                    Color(0xFF2E7D32).copy(alpha = 0.8f),
                )
            ProjectViewMode.INBOX ->
                Triple(
                    Color(0xFFE3F2FD).copy(alpha = 0.7f),
                    Color(0xFF64B5F6).copy(alpha = 0.4f),
                    Color(0xFF1565C0).copy(alpha = 0.8f),
                )
            ProjectViewMode.ADVANCED ->
                Triple(
                    Color(0xFFF3E5F5).copy(alpha = 0.7f),
                    Color(0xFFBA68C8).copy(alpha = 0.4f),
                    Color(0xFF7B1FA2).copy(alpha = 0.8f),
                )
            ProjectViewMode.ATTACHMENTS ->
                Triple(
                    Color(0xFFE8EAF6).copy(alpha = 0.7f),
                    Color(0xFF7986CB).copy(alpha = 0.4f),
                    Color(0xFF283593).copy(alpha = 0.8f),
                )
            ProjectViewMode.DASHBOARD ->
                Triple(
                    Color(0xFFFFF3E0).copy(alpha = 0.7f),
                    Color(0xFFFFB74D).copy(alpha = 0.4f),
                    Color(0xFFEF6C00).copy(alpha = 0.8f),
                )
        }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .border(
                    width = 0.8.dp,
                    color = borderColor,
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
*/
