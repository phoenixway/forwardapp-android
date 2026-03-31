package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import java.text.SimpleDateFormat
import java.util.Date

internal data class SearchResultsContentArgs(
    val commandResults: List<OmniboxCommandResult>,
    val selectedCommandIndex: Int?,
    val onCommandClick: (OmniboxCommandId) -> Unit,
    val accentColor: Color,
    val results: List<GlobalSearchResultItem>,
    val query: String,
    val viewModel: GlobalSearchViewModel,
    val obsidianVaultName: String,
    val context: Context,
    val listState: LazyListState,
    val selectedResultUniqueId: String?,
)

internal data class SearchResultRenderContext(
    val query: String,
    val selectedResultUniqueId: String?,
    val haptic: HapticFeedback,
    val formatter: SimpleDateFormat,
    val viewModel: GlobalSearchViewModel,
    val obsidianVaultName: String,
    val context: Context,
)

@Composable
internal fun SearchResultItemCard(
    result: GlobalSearchResultItem,
    index: Int,
    renderContext: SearchResultRenderContext,
) {
    AnimatedVisibility(
        visible = true,
        enter =
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                initialOffsetY = { it / 2 },
            ) + fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = index * 25)),
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            val typePresentation = result.typePresentation()
            UnifiedSearchResultCard(
                spec = buildSearchResultCardSpec(result, typePresentation, renderContext),
            )
            ResultTypeBadge(
                presentation = typePresentation,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp, end = 6.dp),
            )
        }
    }
}

@Composable
internal fun SearchResultsTopOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    Color.Transparent,
                                ),
                        ),
                ),
    )
}

private fun buildSearchResultCardSpec(
    result: GlobalSearchResultItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec =
    when (result) {
        is GlobalSearchResultItem.GoalItem -> goalCardSpec(result, typePresentation, renderContext)
        is GlobalSearchResultItem.LinkItem -> linkCardSpec(result, typePresentation, renderContext)
        is GlobalSearchResultItem.SubcontextItem -> subcontextCardSpec(result, typePresentation, renderContext)
        is GlobalSearchResultItem.ContextItem -> contextCardSpec(result, typePresentation, renderContext)
        is GlobalSearchResultItem.ActivityItem -> activityCardSpec(result, typePresentation, renderContext)
        is GlobalSearchResultItem.InboxItem -> inboxCardSpec(result, typePresentation, renderContext)
        is GlobalSearchResultItem.AttachmentItem -> attachmentCardSpec(result, typePresentation, renderContext)
    }

private fun goalCardSpec(
    result: GlobalSearchResultItem.GoalItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec =
    SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = result.goal.text,
        subtitle = result.goal.description,
        supporting = result.pathSegments.joinToString(" -> ").ifBlank { result.projectName },
        onClick = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.navigateToProjectForResult(
                result.backlogItem.contextId,
                result.projectName,
            )
        },
        secondaryActionIcon = Icons.Default.Navigation,
        secondaryActionDescription = "Відкрити в навігації",
        onSecondaryAction = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.goBackToRevealProject(result.backlogItem.contextId)
        },
    )

private fun linkCardSpec(
    result: GlobalSearchResultItem.LinkItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec {
    val searchResult = result.searchResult
    val linkData = searchResult.link.linkData
    val secondaryAction =
        when (linkData.type) {
            LinkType.CONTEXT -> {
                {
                    renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    renderContext.viewModel.onDataResultOpened(result.uniqueId)
                    renderContext.viewModel.navigateToProjectForResult(linkData.target, null)
                }
            }
            LinkType.NOTE_DOCUMENT,
            LinkType.CHECKLIST,
            LinkType.MUSIC_NOTE,
            LinkType.URL,
            LinkType.OBSIDIAN,
            -> {
                {
                    renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    renderContext.viewModel.onDataResultOpened(result.uniqueId)
                    handleRelatedLinkClick(
                        link = linkData,
                        obsidianVaultName = renderContext.obsidianVaultName,
                        context = renderContext.context,
                    )
                }
            }
            null -> null
        }
    return SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = linkData.displayName ?: linkData.target,
        subtitle = linkData.target,
        supporting = "Контекст: ${searchResult.contextName}",
        onClick = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.navigateToProjectForResult(
                searchResult.contextId,
                searchResult.contextName,
            )
        },
        secondaryActionIcon = if (secondaryAction != null) Icons.AutoMirrored.Filled.OpenInNew else null,
        secondaryActionDescription = "Додаткова дія",
        onSecondaryAction = secondaryAction,
    )
}

private fun subcontextCardSpec(
    result: GlobalSearchResultItem.SubcontextItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec {
    val subproject = result.searchResult.subcontext
    return SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = subproject.name,
        subtitle = "Батьківський контекст: ${result.searchResult.parentContextName}",
        supporting = result.searchResult.pathSegments.joinToString(" -> "),
        onClick = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.navigateToProjectForResult(subproject.id, subproject.name)
        },
        secondaryActionIcon = Icons.Default.Navigation,
        secondaryActionDescription = "Відкрити в навігації",
        onSecondaryAction = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.goBackToRevealProject(subproject.id)
        },
    )
}

private fun contextCardSpec(
    result: GlobalSearchResultItem.ContextItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec {
    val project = result.searchResult.context
    return SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = project.name,
        subtitle = project.description,
        supporting = result.searchResult.pathSegments.joinToString(" -> "),
        onClick = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.navigateToProjectForResult(project.id, project.name)
        },
        secondaryActionIcon = Icons.Default.Navigation,
        secondaryActionDescription = "Відкрити в навігації",
        onSecondaryAction = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.goBackToRevealProject(project.id)
        },
    )
}

private fun activityCardSpec(
    result: GlobalSearchResultItem.ActivityItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec =
    SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = result.record.text,
        subtitle = result.record.noteText,
        supporting = "Трекер: ${renderContext.formatter.format(Date(result.record.createdAt))}",
        onClick = {
            val contextId = result.record.contextId
            if (!contextId.isNullOrBlank()) {
                renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                renderContext.viewModel.onDataResultOpened(result.uniqueId)
                renderContext.viewModel.navigateToProjectForResult(contextId, null)
            }
        },
        secondaryActionIcon = null,
        secondaryActionDescription = null,
        onSecondaryAction = null,
    )

private fun inboxCardSpec(
    result: GlobalSearchResultItem.InboxItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec =
    SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = result.record.text,
        subtitle = null,
        supporting = "Inbox: ${renderContext.formatter.format(Date(result.record.createdAt))}",
        onClick = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.navigateToProjectForResult(result.record.contextId, null)
        },
        secondaryActionIcon = Icons.Default.ChevronRight,
        secondaryActionDescription = "Відкрити контекст",
        onSecondaryAction = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            renderContext.viewModel.onDataResultOpened(result.uniqueId)
            renderContext.viewModel.navigateToProjectForResult(result.record.contextId, null)
        },
    )

private fun attachmentCardSpec(
    result: GlobalSearchResultItem.AttachmentItem,
    typePresentation: ResultTypePresentation,
    renderContext: SearchResultRenderContext,
): SearchResultCardSpec =
    SearchResultCardSpec(
        presentation = typePresentation,
        isSelected = result.uniqueId == renderContext.selectedResultUniqueId,
        query = renderContext.query,
        title = result.searchResult.title,
        subtitle = result.searchResult.subtitle,
        supporting = "Контекст: ${result.searchResult.contextName ?: "не вказано"}",
        onClick = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val contextId = result.searchResult.ownerContextId
            if (!contextId.isNullOrBlank()) {
                renderContext.viewModel.onDataResultOpened(result.uniqueId)
                renderContext.viewModel.navigateToProjectForResult(
                    contextId,
                    result.searchResult.contextName,
                )
            }
        },
        secondaryActionIcon = Icons.Default.ChevronRight,
        secondaryActionDescription = "Відкрити контекст",
        onSecondaryAction = {
            renderContext.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val contextId = result.searchResult.ownerContextId
            if (!contextId.isNullOrBlank()) {
                renderContext.viewModel.onDataResultOpened(result.uniqueId)
                renderContext.viewModel.navigateToProjectForResult(
                    contextId,
                    result.searchResult.contextName,
                )
            }
        },
    )
