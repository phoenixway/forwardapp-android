package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.common.components.ExpandableFlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Spring

@Composable
private fun EnhancedSublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .semantics { contentDescription = "Підсписок" }
                .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .background(
                        brush =
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    ),
                            ),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubprojectItemRow(
    subprojectContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onTagClick: (String) -> Unit = {},
    childProjects: List<Project> = emptyList(),
    onChildProjectClick: (Project) -> Unit = {},
    currentTimeMillis: Long,
    contextMarkerToEmojiMap: Map<String, String>,
    emojiToHide: String?,
) {
    val subproject = subprojectContent.project
    val parsedData = rememberParsedText(subproject.name, contextMarkerToEmojiMap)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EnhancedCustomCheckbox(
            checked = subproject.isCompleted,
            onCheckedChange = onCheckedChange,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(onClick, onLongClick) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() },
                    )
                },
        ) {
            val textColor = if (subproject.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val textDecoration = if (subproject.isCompleted) TextDecoration.LineThrough else null

            Text(
                text = parsedData.mainText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                textDecoration = textDecoration,
            )

            val hasExtraContent = !subproject.tags.isNullOrEmpty() ||
                (subproject.scoringStatus != ScoringStatus.NOT_ASSESSED) ||
                (subproject.reminderTime != null) ||
                (parsedData.icons.isNotEmpty()) ||
                childProjects.isNotEmpty()

            ExpandableFlowRow(
                maxHeight = 72.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (hasExtraContent) 6.dp else 4.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EnhancedSublistIconBadge(
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )

                    childProjects.forEach { child ->
                        val link = RelatedLink(
                            type = LinkType.PROJECT,
                            target = child.id,
                            displayName = child.name,
                        )
                        RelatedLinkChip(
                            link = link,
                            onClick = { onChildProjectClick(child) },
                        )
                    }

                    subproject.reminderTime?.let { time ->
                        EnhancedReminderBadge(
                            reminderTime = time,
                            currentTimeMillis = currentTimeMillis,
                        )
                    }

                    EnhancedScoreStatusBadge(
                        scoringStatus = subproject.scoringStatus,
                        displayScore = subproject.displayScore,
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
                                    enter = scaleIn(
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    ) + fadeIn(),
                                ) {
                                    AnimatedContextEmoji(
                                        emoji = icon,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                }
                            }
                        }

                    if (!subproject.tags.isNullOrEmpty()) {
                        subproject.tags.filter { it.isNotBlank() }.forEach { tag ->
                            val formattedTag = "#${tag.trim().trimStart('#')}"
                            ModernTagChip(
                                text = formattedTag,
                                onClick = { onTagClick(formattedTag) },
                                tagType = TagType.PROJECT,
                            )
                        }
                    }
                }
            }

            if (!subproject.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = subproject.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (isSelected) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
