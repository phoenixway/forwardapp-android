package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun TodayStageHeader(
    selectedTab: DayManagementTab,
    onSelectTab: (DayManagementTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stages = DayManagementTab.todaySubTabs()
    val index = stages.indexOf(selectedTab)
    if (index < 0) return
    val previous = stages.getOrNull(index - 1)
    val next = stages.getOrNull(index + 1)

    val stageColor = MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
        contentColor = stageColor,
        border = BorderStroke(1.dp, stageColor.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(stageColor, CircleShape))
            Text(
                text = selectedTab.stageTitle(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { previous?.let(onSelectTab) }, enabled = previous != null) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Попередня стадія дня",
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { next?.let(onSelectTab) }, enabled = next != null) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Наступна стадія дня",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun DayManagementTab.stageTitle(): String =
    when (this) {
        DayManagementTab.DAY_START -> "Day Start"
        DayManagementTab.DAY_THEMES -> "Themes"
        DayManagementTab.DAY_FOCUSES -> "Day priorities"
        DayManagementTab.DAY_PLAN -> "Plan"
        DayManagementTab.JOURNAL -> "Journal"
        DayManagementTab.FINALIZATION -> "Finalization"
        else -> title
    }
