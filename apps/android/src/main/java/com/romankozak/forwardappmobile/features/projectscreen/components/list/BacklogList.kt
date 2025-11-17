package com.romankozak.forwardappmobile.features.projectscreen.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel.Event.*
import com.romankozak.forwardappmobile.features.projectscreen.UiState
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.MarkdownText
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
import com.romankozak.forwardappmobile.features.common.ParsedData
import com.romankozak.forwardappmobile.features.common.rememberParsedText
import com.romankozak.forwardappmobile.features.projectscreen.components.backlog.BacklogItemActionsBottomSheet
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.AnimatedContextEmoji
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.EnhancedRelatedLinkChip
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.EnhancedReminderBadge
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.EnhancedScoreStatusBadge
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.NoteIndicatorBadge
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.StatusIconsRow
import com.romankozak.forwardappmobile.shared.data.database.models.LinkType
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import android.util.Log
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItemScope
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.ModernTagChip
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.TagType


import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogView(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    listContent: List<ListItemContent>,
    viewModel: ProjectScreenViewModel,
    state: UiState,
    onRemindersClick: (ListItemContent) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onCopyContent: (ListItemContent) -> Unit,
) {
    val reorderableState = rememberReorderableLazyListState(listState) { from, to -> onMove(from.index, to.index) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<ListItemContent?>(null) }

    val hapticFeedback = LocalHapticFeedback.current

    if (showBottomSheet && selectedItemForActions != null) {
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onCopyContent = { onCopyContent(selectedItemForActions!!) },
            onRemindersClick = { onRemindersClick(selectedItemForActions!!) },
        )
    }


    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(listContent, key = { it.listItem.id }) { item ->
            ReorderableItem(reorderableState, key = item.listItem.id) { isDragging ->
                when (item) {
                    is ListItemContent.GoalItem -> GoalItem(
                        goal = item,
                        obsidianVaultName = "Personal",
                        onCheckedChange = { viewModel.onEvent(GoalChecked(item, it)) },
                        onItemClick = { viewModel.onEvent(GoalClick(item)) },
                        onLongClick = { viewModel.onEvent(GoalLongClick(item)) },
                        onTagClick = { viewModel.onEvent(TagClick(it)) },
                        onRelatedLinkClick = { viewModel.onEvent(RelatedLinkClick(it)) },
                        isSelected = state.selectedItemIds.contains(item.listItem.id),
                        reminders = emptyList() // TODO: Get reminders from state
                    )
                    is ListItemContent.LinkItem -> LinkItem(
                        link = item,
                        onClick = { viewModel.onEvent(LinkClick(item)) }
                    )
                    is ListItemContent.SublistItem -> {
                        SubProjectItem(
                            project = item.project,
                            childProjects = item.childProjects,
                            onCheckedChange = { viewModel.onEvent(SubprojectChecked(item, it)) },
                            onItemClick = { viewModel.onEvent(SubprojectClick(item.project.id)) },
                            onLongClick = { viewModel.onEvent(SubprojectLongClick(item)) },
                            onTagClick = { tag -> viewModel.onEvent(TagClick(tag)) },
                            onChildProjectClick = { childProject -> viewModel.onEvent(SubprojectClick(childProject.id)) },
                            onRelatedLinkClick = { relatedLink -> viewModel.onEvent(RelatedLinkClick(relatedLink)) },
                            contextMarkerToEmojiMap = state.contextMarkerToEmojiMap,
                            currentTimeMillis = state.currentTimeMillis,
                            isSelected = state.selectedItemIds.contains(item.listItem.id),
                            reminders = item.reminders,
                            reorderableScope = this,
                            endAction = {
                IconButton(
                    onClick = { /* Nothing to do here, it's a drag handle */ },
                    modifier =  with(this) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    },
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                }
            }
                        )
                    }

                    is ListItemContent.ChecklistItem -> TODO()
                    is ListItemContent.NoteDocumentItem -> TODO()
                    is ListItemContent.NoteItem -> TODO()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SubProjectItem(
    project: Project,
    childProjects: List<Project>,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onChildProjectClick: (Project) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    reorderableScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String>,
    currentTimeMillis: Long,
    isSelected: Boolean,
    reminders: List<Reminder> = emptyList(),
    endAction: @Composable () -> Unit = {},
) {
    Log.d("ProjectItem", "ProjectItem composable called for project: ${project.name}")
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
            Checkbox(
                checked = project.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                                        delay((parsedData.icons.size + index).toLong() * 50L)
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun GoalItem(
    goal: ListItemContent.GoalItem,
    obsidianVaultName: String,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    modifier: Modifier = Modifier,
    showCheckbox: Boolean = false,
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String> = emptyMap(),
    isSelected: Boolean,
    reminders: List<Reminder> = emptyList(),
    endAction: @Composable () -> Unit = {},
) {
    val reminder = reminders.firstOrNull()
    val parsedData = rememberParsedText(goal.goal.text, contextMarkerToEmojiMap)
    //val viewModel: GoalItemViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = goal.hashCode().toString(), factory = GoalItemViewModelFactory(goal, parsedData, reminder))
    //val shouldShowStatusIcons by viewModel.shouldShowStatusIcons.collectAsState()
    val shouldShowStatusIcons = true

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
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCheckbox) {
                    Checkbox(
                        checked = goal.goal.completed,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Goal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier =
                    Modifier
                        .padding(end = 48.dp) // Reserve space for the handle
                        .pointerInput(onItemClick, onLongClick) {
                            detectTapGestures(
                                onLongPress = { onLongClick() },
                                onTap = { onItemClick() },
                            )
                        },
                ) {
                    MarkdownText(
                        text = parsedData.mainText,
                        isCompleted = goal.goal.completed,
                        obsidianVaultName = obsidianVaultName,
                        onTagClick = onTagClick,
                        onTextClick = onItemClick,
                        onLongClick = onLongClick,
                        maxLines = 4,
                        style =
                        MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp,
                            letterSpacing = 0.1.sp,
                            fontSize = 12.sp,
                            fontWeight = if (goal.goal.completed) FontWeight.Normal else FontWeight.Medium,
                        ),
                    )

                    AnimatedVisibility(
                        visible = shouldShowStatusIcons,
                        enter =
                        slideInVertically(
                            initialOffsetY = { height -> -height },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        ) + fadeIn(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusIconsRow(
                                goal = goal.goal,
                                parsedData = parsedData,
                                reminder = reminder,
                                emojiToHide = emojiToHide,
                                onRelatedLinkClick = {}, //TODO: FIX IT onRelatedLinkClick
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                endAction()
            }
        }
    }
}

@Composable
internal fun LinkItem(
    link: ListItemContent.LinkItem,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale",
    )

    val isSubProject = link.link.type == LinkType.PROJECT
    val backgroundColor = if (isSubProject) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }
    val contentColor = if (isSubProject) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }


    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                )
            }
            .semantics {
                contentDescription = "${link.link.type?.name ?: "LINK"}: ${link.link.target}"
                role = Role.Button
            },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = when (link.link.type) {
                    LinkType.PROJECT -> Icons.Default.AccountTree
                    LinkType.URL -> Icons.Default.Link
                    LinkType.OBSIDIAN -> Icons.Default.Book
                    else -> Icons.Default.BrokenImage
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            if (!isSubProject) {
                Text(
                    text = link.link.target,
                    style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.15.sp,
                        fontSize = 10.sp,
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}