package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun SearchResultsContent(
    args: SearchResultsContentArgs,
    modifier: Modifier = Modifier,
) {
    val renderContext = rememberSearchResultRenderContext(args)
    val groupedResults = rememberGroupedResults(args.results)
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }
    var commandsExpanded by remember(args.query) { mutableStateOf(true) }

    val listItemIndexByResultId =
        remember(args.commandResults, commandsExpanded, groupedResults, groupExpandedState.toMap()) {
            buildListItemIndexByResultId(
                commandResults = args.commandResults,
                commandsExpanded = commandsExpanded,
                groupedResults = groupedResults,
                groupExpandedState = groupExpandedState,
            )
        }

    SearchResultsEffects(
        groupedResults = groupedResults,
        groupExpandedState = groupExpandedState,
        selectedResultUniqueId = args.selectedResultUniqueId,
        listItemIndexByResultId = listItemIndexByResultId,
        listState = args.listState,
    )

    Box(modifier = modifier) {
        SearchResultsList(
            args = args,
            groupedResults = groupedResults,
            renderContext = renderContext,
            groupExpandedState = groupExpandedState,
            commandsExpanded = commandsExpanded,
            onCommandsExpandedChange = { commandsExpanded = it },
        )
        SearchResultsTopOverlay(modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter))
    }
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
): String =
    when {
        selected.size == all.size -> "Типи: Усі"
        selected.size == 1 -> "Типи: ${selected.first().label}"
        else -> "Типи: ${selected.size}"
    }

private fun buildListItemIndexByResultId(
    commandResults: List<OmniboxCommandResult>,
    commandsExpanded: Boolean,
    groupedResults: List<ResultGroup>,
    groupExpandedState: Map<String, Boolean>,
): Map<String, Int> {
    val listItemIndexByResultId = mutableMapOf<String, Int>()
    var lazyIndex = 0
    if (commandResults.isNotEmpty()) {
        lazyIndex += 1
        if (commandsExpanded) {
            lazyIndex += commandResults.size
        }
    }
    groupedResults.forEach { group ->
        lazyIndex += 1
        if (groupExpandedState[group.key] != false) {
            group.items.forEach { item ->
                listItemIndexByResultId[item.uniqueId] = lazyIndex
                lazyIndex += 1
            }
        }
    }
    return listItemIndexByResultId
}

@Composable
private fun rememberSearchResultRenderContext(args: SearchResultsContentArgs): SearchResultRenderContext {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    return remember(
        args.query,
        args.selectedResultUniqueId,
        args.viewModel,
        args.obsidianVaultName,
        args.context,
        haptic,
        formatter,
    ) {
        SearchResultRenderContext(
            query = args.query,
            selectedResultUniqueId = args.selectedResultUniqueId,
            haptic = haptic,
            formatter = formatter,
            viewModel = args.viewModel,
            obsidianVaultName = args.obsidianVaultName,
            context = args.context,
        )
    }
}

@Composable
private fun rememberGroupedResults(results: List<GlobalSearchResultItem>): List<ResultGroup> =
    remember(results) {
        results
            .groupBy { it.groupKey() }
            .map { (key, items) ->
                val presentation = items.first().typePresentation()
                ResultGroup(key = key, presentation = presentation, items = items)
            }
    }

@Composable
private fun SearchResultsEffects(
    groupedResults: List<ResultGroup>,
    groupExpandedState: MutableMap<String, Boolean>,
    selectedResultUniqueId: String?,
    listItemIndexByResultId: Map<String, Int>,
    listState: LazyListState,
) {
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
    LaunchedEffect(selectedResultUniqueId, listItemIndexByResultId) {
        val targetIndex = selectedResultUniqueId?.let { id -> listItemIndexByResultId[id] } ?: return@LaunchedEffect
        if (targetIndex > 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }
}

@Composable
private fun SearchResultsList(
    args: SearchResultsContentArgs,
    groupedResults: List<ResultGroup>,
    renderContext: SearchResultRenderContext,
    groupExpandedState: MutableMap<String, Boolean>,
    commandsExpanded: Boolean,
    onCommandsExpandedChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = args.listState,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        commandResultsSection(
            args = args,
            commandsExpanded = commandsExpanded,
            onCommandsExpandedChange = onCommandsExpandedChange,
        )

        groupedResultsSection(
            groupedResults = groupedResults,
            groupExpandedState = groupExpandedState,
            renderContext = renderContext,
        )

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.commandResultsSection(
    args: SearchResultsContentArgs,
    commandsExpanded: Boolean,
    onCommandsExpandedChange: (Boolean) -> Unit,
) {
    if (args.commandResults.isEmpty()) return

    stickyHeader(key = "header_commands") {
        SearchResultGroupHeader(
            presentation =
                ResultTypePresentation(
                    label = "Дії",
                    icon = Icons.Default.Tune,
                    tone = ResultBadgeTone.Surface,
                ),
            count = args.commandResults.size,
            isExpanded = commandsExpanded,
            onToggle = { onCommandsExpandedChange(!commandsExpanded) },
        )
    }
    if (commandsExpanded) {
        itemsIndexed(
            items = args.commandResults,
            key = { _, item -> "command_${item.id.name}" },
        ) { index, item ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                CommandSearchResultCard(
                    command = item,
                    query = args.query,
                    isSelected = args.selectedCommandIndex == index,
                    accentColor = args.accentColor,
                    onCommandClick = args.onCommandClick,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.groupedResultsSection(
    groupedResults: List<ResultGroup>,
    groupExpandedState: MutableMap<String, Boolean>,
    renderContext: SearchResultRenderContext,
) {
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
                SearchResultItemCard(
                    result = result,
                    index = index,
                    renderContext = renderContext,
                )
            }
        }
    }
}

internal enum class GlobalSearchSort(val label: String) {
    Relevance("Релевантність"),
    Type("Тип"),
    Alphabetical("A-Z"),
}

internal fun resultTitle(item: GlobalSearchResultItem): String =
    when (item) {
        is GlobalSearchResultItem.GoalItem -> item.goal.text
        is GlobalSearchResultItem.LinkItem ->
            item.searchResult.link.linkData.displayName
                ?: item.searchResult.link.linkData.target
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

internal fun GlobalSearchResultItem.groupKey(): String =
    when (this) {
        is GlobalSearchResultItem.AttachmentItem -> "attachments"
        is GlobalSearchResultItem.ContextItem -> "contexts"
        is GlobalSearchResultItem.SubcontextItem -> "subcontexts"
        is GlobalSearchResultItem.GoalItem -> "goals"
        is GlobalSearchResultItem.LinkItem -> "links"
        is GlobalSearchResultItem.ActivityItem -> "activity"
        is GlobalSearchResultItem.InboxItem -> "inbox"
    }

internal fun GlobalSearchResultItem.typePresentation(): ResultTypePresentation =
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
