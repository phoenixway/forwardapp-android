package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val TAG = "MORE_ACTIONS_BUTTON"

@Composable
fun MoreActionsButton(
    isDragging: Boolean,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    Log.d(TAG, "MoreActionsButton composing, isDragging=$isDragging")
    
    IconButton(
        onClick = {
            if (!isDragging) {
                Log.d(TAG, "onClick triggered (not dragging)")
                onMoreClick()
            } else {
                Log.d(TAG, "onClick blocked (currently dragging)")
            }
        },
        enabled = !isDragging,
        modifier = modifier
            .then(dragHandleModifier)
    ) {
        Icon(
            imageVector = if (isDragging) Icons.Default.DragHandle else Icons.Default.MoreVert,
            contentDescription = if (isDragging) "Drag handle" else "More actions",
            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
