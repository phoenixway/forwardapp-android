package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HeaderInfoChip(icon: ImageVector, text: String, contentColor: Color) {
  Surface(
    color = contentColor.copy(alpha = 0.12f),
    contentColor = contentColor,
    shape = RoundedCornerShape(12.dp),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = contentColor,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
