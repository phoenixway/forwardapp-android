package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { previous?.let(onSelectTab) }, enabled = previous != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Попередня стадія дня",
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = selectedTab.stageTitle(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
