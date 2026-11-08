package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role

@Composable
fun BottomPanelIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = BottomPanelTokens.ActionButtonSize,
    iconSize: Dp = BottomPanelTokens.ActionIconSize,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    val colors = bottomPanelColors()
    if (onLongClick != null) {
        Box(modifier.size(buttonSize).clip(CircleShape).combinedClickable(
            role = Role.Button,
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
        ), contentAlignment = Alignment.Center) {
            Icon(imageVector, contentDescription, Modifier.size(iconSize), tint = colors.action)
        }
        return
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = colors.action,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = BottomPanelTokens.ActionButtonSize,
    iconSize: Dp = BottomPanelTokens.ActionIconSize,
) {
    val colors = bottomPanelColors()
    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor =
                    if (selected) {
                        colors.selectedActionContainer
                    } else {
                        Color.Transparent
                    },
                contentColor =
                    if (selected) {
                        colors.selectedActionContent
                    } else {
                        colors.action
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
