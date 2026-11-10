package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class DashboardHeaderEmphasis { Focus, Access, Supporting }

@Composable
internal fun DashboardSectionHeader(
    title: String,
    subtitle: String? = null,
    isExpanded: Boolean,
    onClick: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    emphasis: DashboardHeaderEmphasis = DashboardHeaderEmphasis.Access,
) {
    val isFocus = emphasis == DashboardHeaderEmphasis.Focus
    val isSupporting = emphasis == DashboardHeaderEmphasis.Supporting
    val foreground = if (isSupporting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocus) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
            contentColor = foreground,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (isFocus) 12.dp else 0.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isFocus) {
                Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 10.dp).size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (isFocus) 20.sp else if (isSupporting) 14.sp else 18.sp,
                        fontFamily = if (isFocus) MaterialTheme.typography.titleMedium.fontFamily else FontFamily.SansSerif,
                    ),
                    fontWeight = if (isFocus) FontWeight.SemiBold else FontWeight.Medium,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            action?.invoke()
            IconButton(onClick = onClick) { Icon(
                imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(if (isExpanded) R.string.deck_collapse else R.string.deck_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            ) }
        }
    }
}

