package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import com.romankozak.forwardappmobile.data.database.models.Reminder
import androidx.compose.material.icons.filled.AccountTree
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import kotlinx.coroutines.delay



@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectItem(
    project: Project,
    childProjects: List<Project>,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onChildProjectClick: (Project) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    modifier: Modifier = Modifier,
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String>,
    currentTimeMillis: Long,
    isSelected: Boolean,
    reminders: List<Reminder> = emptyList(),
    endAction: @Composable () -> Unit = {},
) {
    android.util.Log.d("ProjectItem", "ProjectItem composable called for project: ${project.name}")
    val reminder = reminders.firstOrNull()
    val parsedData = rememberParsedText(project.name, contextMarkerToEmojiMap)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = "Project",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .pointerInput(onItemClick, onLongClick) {
                        detectTapGestures(
                            onLongPress = { onLongClick() },
                            onTap = { onItemClick() },
                        )
                    },
            ) {
                val textColor = if (project.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val textDecoration = if (project.isCompleted) TextDecoration.LineThrough else null

                Text(
                    text = parsedData.mainText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (project.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    textDecoration = textDecoration,
                )

                val hasStatusContent =
                    (project.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                        (reminder != null) ||
                        (parsedData.icons.isNotEmpty()) ||
                        (!project.description.isNullOrBlank()) ||
                        childProjects.isNotEmpty() ||
                        (!project.tags.isNullOrEmpty())

                AnimatedVisibility(
                    visible = hasStatusContent,
                    enter =
                    slideInVertically(
                        initialOffsetY = { height -> -height },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    ) + fadeIn(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
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
                                scoringStatus = project.scoringStatus,
                                displayScore = project.displayScore,
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
                                            enter =
                                            scaleIn(
                                                animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                ),
                                            ) + fadeIn(),
                                        ) {
                                            AnimatedContextEmoji(
                                                emoji = icon,
                                                modifier = Modifier.align(Alignment.CenterVertically),
                                            )
                                        }
                                    }
                                }

                            if (!project.description.isNullOrBlank()) {
                                NoteIndicatorBadge(modifier = Modifier.align(Alignment.CenterVertically))
                            }

                            project.tags?.filter { it.isNotBlank() }?.forEach { tag ->
                                val formattedTag = "#${tag.trim().trimStart('#')}"
                                ModernTagChip(
                                    text = formattedTag,
                                    onClick = { onTagClick(formattedTag) },
                                    tagType = TagType.PROJECT,
                                )
                            }

                            childProjects.forEachIndexed { index, childProject ->
                                key(childProject.id) {
                                    var delayedVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) {
                                        delay((parsedData.icons.size + index) * 50L)
                                        delayedVisible = true
                                    }
                                    AnimatedVisibility(
                                        visible = delayedVisible,
                                        enter =
                                        slideInHorizontally(
                                            initialOffsetX = { fullWidth -> fullWidth },
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        ) + fadeIn(),
                                    ) {
                                        EnhancedRelatedLinkChip(
                                            link = RelatedLink(
                                                type = LinkType.PROJECT,
                                                target = childProject.id,
                                                displayName = childProject.name
                                            ),
                                            onClick = { onChildProjectClick(childProject) },
                                        )
                                    }
                                }
                            }


                        }
                    }
                }
            }
            endAction()
        }
    }
}
