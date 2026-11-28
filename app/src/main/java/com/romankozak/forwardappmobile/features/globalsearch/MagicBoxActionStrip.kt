package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal data class MagicBoxQuickNavigationAction(
    val imageVector: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
internal fun MagicBoxActionStrip(
    palette: OmniboxModePalette,
    hasVisibleResults: Boolean,
    resultCount: Int,
    quickNavigationAction: MagicBoxQuickNavigationAction?,
    onNavigateBack: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.04f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
            ),
        shape = RoundedCornerShape(18.dp),
        color = palette.searchSurface,
        tonalElevation = 3.dp,
        border = BorderStroke(
            width = 1.dp,
            color = palette.panelOutline.copy(alpha = 0.32f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionStripButton(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    palette = palette,
                    onClick = onNavigateBack,
                )
                quickNavigationAction?.let { action ->
                    ActionStripButton(
                        imageVector = action.imageVector,
                        contentDescription = action.contentDescription,
                        palette = palette,
                        onClick = action.onClick,
                        emphasized = true,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = hasVisibleResults,
                    enter = fadeIn(animationSpec = tween(delayMillis = 200)) + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    ResultsCountBadge(count = resultCount)
                }
                ActionStripButton(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Налаштування пошуку",
                    palette = palette,
                    onClick = onShowSettings,
                )
            }
        }
    }
}

@Composable
private fun ActionStripButton(
    imageVector: ImageVector,
    contentDescription: String,
    palette: OmniboxModePalette,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (emphasized) palette.modeIconContainer.copy(alpha = 0.88f) else palette.secondaryActionContainer,
        border = BorderStroke(
            width = 1.dp,
            color = palette.iconTint.copy(alpha = if (emphasized) 0.18f else 0.05f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (emphasized) palette.iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
