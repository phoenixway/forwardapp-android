package com.romankozak.forwardappmobile.features.context.ui.contextcreen.components.backlogitems

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModernTagChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagType: TagType = TagType.PROJECT,
) {
    val colors = getTagColors(tagType, false)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border =
            BorderStroke(
                width = 0.8.dp,
                color = colors.borderStart.copy(alpha = 0.4f),
            ),
        onClick = onClick,
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        colors.backgroundStart,
                                        colors.backgroundEnd,
                                    ),
                            ),
                    ),
        ) {
            Text(
                text = text,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                color = colors.content,
                modifier =
                    Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
