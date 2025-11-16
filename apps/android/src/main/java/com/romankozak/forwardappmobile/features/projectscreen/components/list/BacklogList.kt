package com.romankozak.forwardappmobile.features.projectscreen.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
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
import com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems.StatusIconsRow
import com.romankozak.forwardappmobile.shared.data.database.models.LinkType
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink

import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogView(
    items: List<ListItemContent>,
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
        items(items, key = { it.listItem.id }) { item ->
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
                        // TODO: Implement SublistItem
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