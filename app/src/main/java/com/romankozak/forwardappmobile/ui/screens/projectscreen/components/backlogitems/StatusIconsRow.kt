package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.common.ParsedTextData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusIconsRow(
    goal: Goal,
    parsedData: ParsedTextData,
    reminder: Reminder?,
    emojiToHide: String?,
    onRelatedLinkClick: (com.romankozak.forwardappmobile.data.database.models.RelatedLink) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        reminder?.let {
            EnhancedReminderBadge(
                reminder = it,
            )
        }

        EnhancedScoreStatusBadge(
            scoringStatus = goal.scoringStatus,
            displayScore = goal.displayScore,
        )

        parsedData.icons
            .filterNot { icon -> icon == emojiToHide }
            .forEachIndexed { index, icon ->
                key(icon) {
                    var delayedVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 50L)
                        delayedVisible = true
                    }
                    AnimatedVisibility(
                        visible = delayedVisible,
                        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    ) {
                        AnimatedContextEmoji(
                            emoji = icon,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }

        if (!goal.description.isNullOrBlank()) {
            NoteIndicatorBadge(modifier = Modifier.align(Alignment.CenterVertically))
        }

        goal.relatedLinks?.filter { it.type != null }?.forEachIndexed { index, link ->
            key(link.target + link.type?.name) {
                var delayedVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay((parsedData.icons.size + index) * 50L)
                    delayedVisible = true
                }
                AnimatedVisibility(
                    visible = delayedVisible,
                    enter = slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { fullWidth -> fullWidth } + fadeIn(),
                ) {
                    EnhancedRelatedLinkChip(
                        link = link,
                        onClick = { onRelatedLinkClick(link) },
                    )
                }
            }
        }
    }
}
