package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared Dashboard chrome only; each section retains its own data and action authority. */
@Composable
internal fun WorkspaceDashboardSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.16f))) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(colors.primaryContainer.copy(alpha = 0.16f))
                .padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, Modifier.size(20.dp), tint = colors.primary)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                actions()
            }
            content()
        }
    }
}
