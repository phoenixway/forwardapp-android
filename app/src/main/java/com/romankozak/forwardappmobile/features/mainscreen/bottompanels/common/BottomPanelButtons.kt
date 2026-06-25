package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size
import com.romankozak.forwardappmobile.core.theme.InputModeColors

@Composable
fun BottomPanelIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    panelStyle: InputModeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = BottomPanelTokens.ActionButtonSize,
    iconSize: Dp = BottomPanelTokens.ActionIconSize,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun BottomPanelToggleButton(
    imageVector: ImageVector,
    contentDescription: String,
    selected: Boolean,
    panelStyle: InputModeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = BottomPanelTokens.ActionButtonSize,
    iconSize: Dp = BottomPanelTokens.ActionIconSize,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                contentColor =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        panelStyle.textColor.copy(alpha = 0.8f)
                    },
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}
