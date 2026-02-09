package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import java.util.Locale

private data class ReservedBadgeStyle(
    val tint: Color,
    val symbol: String? = null,
    val icon: ImageVector? = null,
    val glow: Boolean = false,
)

@Composable
fun ReservedRoleBadge(
    roleCode: String?,
    modifier: Modifier = Modifier,
) {
    val normalized = roleCode?.trim()?.lowercase(Locale.ROOT)
    val style =
        when (normalized) {
            ContextRoleRegistry.ROLE_PROJECT ->
                ReservedBadgeStyle(
                    tint = Color(0xFF2E7D32),
                    icon = Icons.Default.Check,
                )
            ContextRoleRegistry.ROLE_DIRECTION ->
                ReservedBadgeStyle(
                    tint = Color(0xFF1565C0),
                    icon = Icons.Default.ArrowUpward,
                )
            ContextRoleRegistry.ROLE_MAIN_BEACON ->
                ReservedBadgeStyle(
                    tint = Color(0xFFEF6C00),
                    symbol = "✦",
                    glow = true,
                )
            ContextRoleRegistry.ROLE_MANAGEMENT ->
                ReservedBadgeStyle(
                    tint = Color(0xFF4E5BA6),
                    icon = Icons.Default.Settings,
                )
            else -> null
        } ?: return

    val shape = RoundedCornerShape(999.dp)
    val glowModifier =
        if (style.glow) {
            Modifier.shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = style.tint.copy(alpha = 0.28f),
                spotColor = style.tint.copy(alpha = 0.28f),
            )
        } else {
            Modifier
        }

    Surface(
        modifier =
            modifier
                .then(glowModifier)
                .height(18.dp)
                .defaultMinSize(minWidth = 18.dp),
        shape = shape,
        color = style.tint.copy(alpha = 0.14f),
        contentColor = style.tint,
        border = BorderStroke(1.dp, style.tint.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                style.icon != null -> {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.tint,
                        modifier = Modifier.height(11.dp),
                    )
                }
                style.symbol != null -> {
                    Text(
                        text = style.symbol,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = style.tint,
                    )
                }
            }
        }
    }
}
