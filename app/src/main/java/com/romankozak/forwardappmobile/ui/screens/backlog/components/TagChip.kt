package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagChip(
    text: String,
    onDismiss: () -> Unit,
    isDismissible: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                ).border(
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                ).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.15.sp,
                    fontSize = 10.sp,
                ),
            color = MaterialTheme.colorScheme.secondary,
        )
        if (isDismissible) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Remove tag",
                modifier =
                    Modifier
                        .size(16.dp)
                        .clickable(onClick = onDismiss),
            )
        }
    }
}
