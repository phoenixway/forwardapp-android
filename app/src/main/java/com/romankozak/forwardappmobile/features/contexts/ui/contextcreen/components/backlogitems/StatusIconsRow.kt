package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.backlogitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.common.ParsedTextData
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InternalStatusIconsRow(
    relatedLinks: List<RelatedLink>?,
    scoringStatus: String,
    displayScore: Int,
    description: String?,
    parsedData: ParsedTextData,
    reminder: Reminder?,
    emojiToHide: String?,
    onRelatedLinkClick: (RelatedLink) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (reminder != null) {
            EnhancedReminderBadge(
                reminder = reminder,
            )
        }

        EnhancedScoreStatusBadge(
            scoringStatus = scoringStatus,
            displayScore = displayScore,
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

        if (!description.isNullOrBlank()) {
            NoteIndicatorBadge(modifier = Modifier.align(Alignment.CenterVertically))
        }

        relatedLinks?.filter { it.type != null }?.forEachIndexed { index, link ->
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

@Composable
fun StatusIconsRow(
    goal: Goal,
    parsedData: ParsedTextData,
    reminder: Reminder?,
    emojiToHide: String?,
    onRelatedLinkClick: (RelatedLink) -> Unit
) {
    InternalStatusIconsRow(
        relatedLinks = goal.relatedLinks,
        scoringStatus = goal.scoringStatus,
        displayScore = goal.displayScore,
        description = goal.description,
        parsedData = parsedData,
        reminder = reminder,
        emojiToHide = emojiToHide,
        onRelatedLinkClick = onRelatedLinkClick
    )
}

@Composable
fun StatusIconsRow(
    project: Project,
    parsedData: ParsedTextData,
    reminder: Reminder?,
    emojiToHide: String?,
    onRelatedLinkClick: (RelatedLink) -> Unit
) {
    InternalStatusIconsRow(
        relatedLinks = project.relatedLinks,
        scoringStatus = project.scoringStatus,
        displayScore = project.displayScore,
        description = project.description,
        parsedData = parsedData,
        reminder = reminder,
        emojiToHide = emojiToHide,
        onRelatedLinkClick = onRelatedLinkClick
    )
}