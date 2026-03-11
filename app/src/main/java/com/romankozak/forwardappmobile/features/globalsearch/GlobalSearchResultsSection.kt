package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun SearchResultsContent(
    commandResults: List<OmniboxCommandResult>,
    selectedCommandIndex: Int?,
    onCommandClick: (OmniboxCommandId) -> Unit,
    accentColor: Color,
    results: List<GlobalSearchResultItem>,
    query: String,
    viewModel: GlobalSearchViewModel,
    obsidianVaultName: String,
    context: Context,
    listState: LazyListState,
    selectedResultUniqueId: String?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val groupedResults = remember(results) {
        results
            .groupBy { it.groupKey() }
            .map { (key, items) ->
                val presentation = items.first().typePresentation()
                ResultGroup(key = key, presentation = presentation, items = items)
            }
    }
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }
    var commandsExpanded by remember(query) { mutableStateOf(true) }

    LaunchedEffect(groupedResults) {
        val activeKeys = groupedResults.map { it.key }.toSet()
        groupedResults.forEach { group ->
            if (groupExpandedState[group.key] == null) {
                groupExpandedState[group.key] = true
            }
        }
        groupExpandedState.keys.toList().forEach { key ->
            if (key !in activeKeys) {
                groupExpandedState.remove(key)
            }
        }
    }
    LaunchedEffect(selectedResultUniqueId, groupedResults) {
        val selectedGroupKey =
            selectedResultUniqueId?.let { id ->
                groupedResults.firstOrNull { group -> group.items.any { it.uniqueId == id } }?.key
            }
        if (selectedGroupKey != null && groupExpandedState[selectedGroupKey] == false) {
            groupExpandedState[selectedGroupKey] = true
        }
    }

    val listItemIndexByResultId = mutableMapOf<String, Int>()
    var lazyIndex = 0
    if (commandResults.isNotEmpty()) {
        lazyIndex += 1 // commands header
        if (commandsExpanded) {
            lazyIndex += commandResults.size
        }
    }
    groupedResults.forEach { group ->
        lazyIndex += 1 // group header
        if (groupExpandedState[group.key] != false) {
            group.items.forEach { item ->
                listItemIndexByResultId[item.uniqueId] = lazyIndex
                lazyIndex += 1
            }
        }
    }
    LaunchedEffect(selectedResultUniqueId, listItemIndexByResultId) {
        val targetIndex = selectedResultUniqueId?.let { id -> listItemIndexByResultId[id] } ?: return@LaunchedEffect
        if (targetIndex > 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (commandResults.isNotEmpty()) {
                stickyHeader(key = "header_commands") {
                    SearchResultGroupHeader(
                        presentation =
                            ResultTypePresentation(
                                label = "Дії",
                                icon = Icons.Default.Tune,
                                tone = ResultBadgeTone.Surface,
                            ),
                        count = commandResults.size,
                        isExpanded = commandsExpanded,
                        onToggle = { commandsExpanded = !commandsExpanded },
                    )
                }
                if (commandsExpanded) {
                    itemsIndexed(
                        items = commandResults,
                        key = { _, item -> "command_${item.id.name}" },
                    ) { index, item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            CommandSearchResultCard(
                                command = item,
                                query = query,
                                isSelected = selectedCommandIndex == index,
                                accentColor = accentColor,
                                onCommandClick = onCommandClick,
                            )
                        }
                    }
                }
            }

            groupedResults.forEach { group ->
                val isExpanded = groupExpandedState[group.key] != false
                stickyHeader(key = "header_${group.key}") {
                    SearchResultGroupHeader(
                        presentation = group.presentation,
                        count = group.items.size,
                        isExpanded = isExpanded,
                        onToggle = { groupExpandedState[group.key] = !isExpanded },
                    )
                }

                if (isExpanded) {
                    itemsIndexed(
                        items = group.items,
                        key = { _, result -> result.uniqueId },
                    ) { index, result ->
                        AnimatedVisibility(
                            visible = true,
                            enter =
                                slideInVertically(
                                    animationSpec =
                                        spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 300f,
                                        ),
                                    initialOffsetY = { it / 2 },
                                ) +
                                    fadeIn(
                                        animationSpec =
                                            tween(
                                                durationMillis = 300,
                                                delayMillis = index * 35,
                                            ),
                                    ),
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                val typePresentation = result.typePresentation()
                                when (result) {
                        is GlobalSearchResultItem.GoalItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.goal.text,
                                subtitle = result.goal.description,
                                supporting = result.pathSegments.joinToString(" → ").ifBlank { result.projectName },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(result.backlogItem.contextId, result.projectName)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.goBackToRevealProject(result.backlogItem.contextId)
                                },
                            )
                        }
                        is GlobalSearchResultItem.LinkItem -> {
                            val searchResult = result.searchResult
                            val linkData = searchResult.link.linkData
                            val secondaryAction: (() -> Unit)? =
                                when (linkData.type) {
                                    LinkType.CONTEXT -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onDataResultOpened(result.uniqueId)
                                            viewModel.navigateToProjectForResult(linkData.target, null)
                                        }
                                    }
                                    LinkType.URL,
                                    LinkType.OBSIDIAN,
                                    -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onDataResultOpened(result.uniqueId)
                                            handleRelatedLinkClick(
                                                link = linkData,
                                                obsidianVaultName = obsidianVaultName,
                                                context = context,
                                            )
                                        }
                                    }
                                    null -> null
                                }
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = linkData.displayName ?: linkData.target,
                                subtitle = linkData.target,
                                supporting = "Контекст: ${searchResult.contextName}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(searchResult.contextId, searchResult.contextName)
                                },
                                secondaryActionIcon = if (secondaryAction != null) Icons.AutoMirrored.Filled.OpenInNew else null,
                                secondaryActionDescription = "Додаткова дія",
                                onSecondaryAction = secondaryAction,
                            )
                        }
                        is GlobalSearchResultItem.SubcontextItem -> {
                            val subproject = result.searchResult.subcontext
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = subproject.name,
                                subtitle = "Батьківський контекст: ${result.searchResult.parentContextName}",
                                supporting = result.searchResult.pathSegments.joinToString(" → "),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(subproject.id, subproject.name)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.goBackToRevealProject(subproject.id)
                                },
                            )
                        }
                        is GlobalSearchResultItem.ContextItem -> {
                            val project = result.searchResult.context
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = project.name,
                                subtitle = project.description,
                                supporting = result.searchResult.pathSegments.joinToString(" → "),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(project.id, project.name)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.goBackToRevealProject(project.id)
                                },
                            )
                        }

                        is GlobalSearchResultItem.ActivityItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.record.text,
                                subtitle = result.record.noteText,
                                supporting = "Трекер: ${formatter.format(Date(result.record.createdAt))}",
                                onClick = {
                                    val contextId = result.record.contextId
                                    if (!contextId.isNullOrBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onDataResultOpened(result.uniqueId)
                                        viewModel.navigateToProjectForResult(contextId, null)
                                    }
                                },
                                secondaryActionIcon = null,
                                secondaryActionDescription = null,
                                onSecondaryAction = null,
                            )
                        }
                        is GlobalSearchResultItem.InboxItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.record.text,
                                subtitle = null,
                                supporting = "Inbox: ${formatter.format(Date(result.record.createdAt))}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                                secondaryActionIcon = Icons.Default.ChevronRight,
                                secondaryActionDescription = "Відкрити контекст",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                            )
                        }
                        is GlobalSearchResultItem.AttachmentItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.searchResult.title,
                                subtitle = result.searchResult.subtitle,
                                supporting = "Контекст: ${result.searchResult.contextName ?: "не вказано"}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val contextId = result.searchResult.ownerContextId
                                    if (!contextId.isNullOrBlank()) {
                                        viewModel.onDataResultOpened(result.uniqueId)
                                        viewModel.navigateToProjectForResult(contextId, result.searchResult.contextName)
                                    }
                                },
                                secondaryActionIcon = Icons.Default.ChevronRight,
                                secondaryActionDescription = "Відкрити контекст",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val contextId = result.searchResult.ownerContextId
                                    if (!contextId.isNullOrBlank()) {
                                        viewModel.onDataResultOpened(result.uniqueId)
                                        viewModel.navigateToProjectForResult(contextId, result.searchResult.contextName)
                                    }
                                },
                            )
                        }
                            }
                            ResultTypeBadge(
                                presentation = typePresentation,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp, end = 8.dp),
                            )
                        }
                    }
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), Color.Transparent),
                            ),
                    ),
        )
    }
}

@Composable
private fun SearchResultGroupHeader(
    presentation: ResultTypePresentation,
    count: Int,
    isExpanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val (container, content) = resultBadgeColors(presentation.tone)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onToggle != null) { onToggle?.invoke() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.label,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = container,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onToggle != null) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути секцію" else "Розгорнути секцію",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UnifiedSearchResultCard(
    presentation: ResultTypePresentation,
    isSelected: Boolean,
    query: String,
    title: String,
    subtitle: String?,
    supporting: String?,
    onClick: () -> Unit,
    secondaryActionIcon: ImageVector?,
    secondaryActionDescription: String?,
    onSecondaryAction: (() -> Unit)?,
) {
    val (badgeContainer, badgeContent) = resultBadgeColors(presentation.tone)
    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = tween(durationMillis = 170),
        label = "selected_result_scale",
    )

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = selectedScale, scaleY = selectedScale),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        shape = RoundedCornerShape(16.dp),
        border =
            androidx.compose.foundation.BorderStroke(
                if (isSelected) 1.35.dp else 1.dp,
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = presentation.label,
                    tint = badgeContent,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(
                    text = title,
                    query = query,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HighlightedText(
                        text = subtitle,
                        query = query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (!supporting.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HighlightedText(
                        text = supporting,
                        query = query,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            if (secondaryActionIcon != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = secondaryActionIcon,
                        contentDescription = secondaryActionDescription ?: "Action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    maxLines: Int,
) {
    val annotated =
        remember(text, query) {
            if (query.isBlank()) return@remember buildAnnotatedString { append(text) }
            val loweredText = text.lowercase(Locale.getDefault())
            val loweredQuery = query.lowercase(Locale.getDefault())
            val start = loweredText.indexOf(loweredQuery)
            if (start < 0) {
                buildAnnotatedString { append(text) }
            } else {
                val end = start + query.length
                buildAnnotatedString {
                    append(text.substring(0, start))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(start, end))
                    }
                    append(text.substring(end))
                }
            }
        }

    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

internal enum class GlobalSearchType(
    val label: String,
    val icon: ImageVector,
) {
    Attachments("Вкладення", Icons.Default.Description),
    Contexts("Контексти", Icons.Default.AccountTree),
    Goals("Цілі", Icons.Default.Flag),
    Links("Посилання", Icons.Default.Link),
    Activity("Активності", Icons.Default.History),
    Inbox("Inbox", Icons.Outlined.MoveToInbox),
    ;

    fun matches(item: GlobalSearchResultItem): Boolean =
        when (this) {
            Attachments -> item is GlobalSearchResultItem.AttachmentItem
            Contexts -> item is GlobalSearchResultItem.ContextItem || item is GlobalSearchResultItem.SubcontextItem
            Goals -> item is GlobalSearchResultItem.GoalItem
            Links -> item is GlobalSearchResultItem.LinkItem
            Activity -> item is GlobalSearchResultItem.ActivityItem
            Inbox -> item is GlobalSearchResultItem.InboxItem
        }
}

internal fun formatTypeChipLabel(
    selected: Set<GlobalSearchType>,
    all: Set<GlobalSearchType>,
): String {
    if (selected.size == all.size) return "Типи: Усі"
    if (selected.size == 1) return "Типи: ${selected.first().label}"
    return "Типи: ${selected.size}"
}

internal enum class GlobalSearchSort(val label: String) {
    Relevance("Релевантність"),
    Type("Тип"),
    Alphabetical("A-Z"),
}

internal fun resultTitle(item: GlobalSearchResultItem): String =
    when (item) {
        is GlobalSearchResultItem.GoalItem -> item.goal.text
        is GlobalSearchResultItem.LinkItem -> item.searchResult.link.linkData.displayName ?: item.searchResult.link.linkData.target
        is GlobalSearchResultItem.SubcontextItem -> item.searchResult.subcontext.name
        is GlobalSearchResultItem.ContextItem -> item.searchResult.context.name
        is GlobalSearchResultItem.ActivityItem -> item.record.text
        is GlobalSearchResultItem.InboxItem -> item.record.text
        is GlobalSearchResultItem.AttachmentItem -> item.searchResult.title
    }

private data class ResultGroup(
    val key: String,
    val presentation: ResultTypePresentation,
    val items: List<GlobalSearchResultItem>,
)

private fun GlobalSearchResultItem.groupKey(): String =
    when (this) {
        is GlobalSearchResultItem.AttachmentItem -> "attachments"
        is GlobalSearchResultItem.ContextItem -> "contexts"
        is GlobalSearchResultItem.SubcontextItem -> "subcontexts"
        is GlobalSearchResultItem.GoalItem -> "goals"
        is GlobalSearchResultItem.LinkItem -> "links"
        is GlobalSearchResultItem.ActivityItem -> "activity"
        is GlobalSearchResultItem.InboxItem -> "inbox"
    }

private fun GlobalSearchResultItem.typePresentation(): ResultTypePresentation =
    when (this) {
        is GlobalSearchResultItem.AttachmentItem ->
            ResultTypePresentation(
                label = "Вкладення",
                icon = Icons.Default.Description,
                tone = ResultBadgeTone.Primary,
            )
        is GlobalSearchResultItem.ContextItem ->
            ResultTypePresentation(
                label = "Контекст",
                icon = Icons.Default.AccountTree,
                tone = ResultBadgeTone.Secondary,
            )
        is GlobalSearchResultItem.SubcontextItem ->
            ResultTypePresentation(
                label = "Підконтекст",
                icon = Icons.Default.AccountTree,
                tone = ResultBadgeTone.Secondary,
            )
        is GlobalSearchResultItem.GoalItem ->
            ResultTypePresentation(
                label = "Ціль",
                icon = Icons.Default.Flag,
                tone = ResultBadgeTone.Tertiary,
            )
        is GlobalSearchResultItem.LinkItem ->
            ResultTypePresentation(
                label = "Посилання",
                icon = Icons.Default.Link,
                tone = ResultBadgeTone.Tertiary,
            )
        is GlobalSearchResultItem.ActivityItem ->
            ResultTypePresentation(
                label = "Активність",
                icon = Icons.Default.History,
                tone = ResultBadgeTone.Surface,
            )
        is GlobalSearchResultItem.InboxItem ->
            ResultTypePresentation(
                label = "Inbox",
                icon = Icons.Outlined.MoveToInbox,
                tone = ResultBadgeTone.Surface,
            )
    }

@Composable
private fun ResultTypeBadge(
    presentation: ResultTypePresentation,
    modifier: Modifier = Modifier,
) {
    val (container, content) = resultBadgeColors(presentation.tone)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = container,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.label,
                tint = content,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
            )
        }
    }
}

@Composable
private fun resultBadgeColors(tone: ResultBadgeTone) =
    when (tone) {
        ResultBadgeTone.Primary -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ResultBadgeTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ResultBadgeTone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ResultBadgeTone.Surface ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) to MaterialTheme.colorScheme.onSurfaceVariant
    }

private data class ResultTypePresentation(
    val label: String,
    val icon: ImageVector,
    val tone: ResultBadgeTone,
)

private enum class ResultBadgeTone {
    Primary,
    Secondary,
    Tertiary,
    Surface,
}

@Composable
private fun ResultsCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
) {
    try {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, link.target.toUri())
                context.startActivity(intent)
            }
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    val intent = Intent(Intent.ACTION_VIEW, obsidianUri.toUri())
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Назву Obsidian сховища не встановлено.", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Не вдалося відкрити посилання.", Toast.LENGTH_LONG).show()
    }
}
