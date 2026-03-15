package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val LIFE_STATE_CARD_CORNER_DP = 18
private const val LIFE_STATE_BACKGROUND_ALPHA = 0.04f
private const val LIFE_STATE_TITLE_SIZE_SP = 16
private const val LIFE_STATE_STATUS_DOT_SIZE_DP = 10
private const val LIFE_STATE_STATUS_TEXT_SIZE_SP = 12
private const val LEVEL_OK_COLOR_HEX = 0xFF4CAF50
private const val LEVEL_ATTENTION_COLOR_HEX = 0xFFFFC107
private const val LEVEL_CRITICAL_COLOR_HEX = 0xFFF44336
private val LEVEL_OK_COLOR = Color(LEVEL_OK_COLOR_HEX)
private val LEVEL_ATTENTION_COLOR = Color(LEVEL_ATTENTION_COLOR_HEX)
private val LEVEL_CRITICAL_COLOR = Color(LEVEL_CRITICAL_COLOR_HEX)

private enum class LevelStatus {
    OK,
    ATTENTION,
    CRITICAL,
}

private fun levelColor(status: LevelStatus): Color =
    when (status) {
        LevelStatus.OK -> LEVEL_OK_COLOR
        LevelStatus.ATTENTION -> LEVEL_ATTENTION_COLOR
        LevelStatus.CRITICAL -> LEVEL_CRITICAL_COLOR
    }

@Composable
fun LifeManagementState() {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(LIFE_STATE_CARD_CORNER_DP.dp))
                .background(Color.White.copy(alpha = LIFE_STATE_BACKGROUND_ALPHA))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Life-management State",
            fontSize = LIFE_STATE_TITLE_SIZE_SP.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(LIFE_STATE_STATUS_DOT_SIZE_DP.dp)
                    .clip(CircleShape)
                    .background(levelColor(LevelStatus.ATTENTION)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Warning",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = LIFE_STATE_STATUS_TEXT_SIZE_SP.sp,
            )
        }
    }
}
