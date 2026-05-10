package com.romankozak.forwardappmobile.data.repository

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalAttachmentSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalContextSearchRow
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalContextSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TYPE_ORDER_CONTEXT = 0
private const val TYPE_ORDER_GOAL = 1
private const val TYPE_ORDER_ATTACHMENT = 2
private const val TYPE_ORDER_OTHER = 3

internal fun buildSafeActivityFtsQuery(query: String): String? {
    val sanitizedQuery = query.removePrefix("%").removeSuffix("%").trim()
    if (sanitizedQuery.isBlank()) return null

    val tokens =
        Regex("[\\p{L}\\p{N}_]+")
            .findAll(sanitizedQuery)
            .map { match -> "\"${match.value}\"" }
            .toList()

    return tokens.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

private val GlobalSearchResultItem.typeOrder: Int
    get() =
        when (this) {
            is GlobalSearchResultItem.ContextItem,
            is GlobalSearchResultItem.SubcontextItem,
            -> TYPE_ORDER_CONTEXT
            is GlobalSearchResultItem.GoalItem -> TYPE_ORDER_GOAL
            is GlobalSearchResultItem.AttachmentItem -> TYPE_ORDER_ATTACHMENT
            else -> TYPE_ORDER_OTHER
        }

@Singleton
class SearchRepository
    @Inject
    @Suppress("LongParameterList")
    constructor(
        private val goalDao: GoalDao,
        private val contextDao: ContextDao,
        private val listItemDao: ListItemDao,
        private val linkItemDao: LinkItemDao,
        private val activityRepository: ActivityRepository,
        private val inboxRecordDao: InboxRecordDao,
        private val attachmentsRepository: AttachmentsRepository,
    ) {
        suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
            val sanitizedQuery = query.removePrefix("%").removeSuffix("%").trim()
            val activityQuery = buildSafeActivityFtsQuery(query)
            val combinedResults =
                buildGoalResults(query) +
                    buildLinkResults(query) +
                    buildSubcontextResults(query) +
                    buildContextResults(query) +
                    buildActivityResults(activityQuery) +
                    buildInboxResults(query) +
                    buildAttachmentResults(sanitizedQuery)

            return combinedResults.sortedWith(
                compareBy<GlobalSearchResultItem> { it.typeOrder }
                    .thenByDescending { it.timestamp },
            )
        }

        private suspend fun buildGoalResults(query: String): List<GlobalSearchResultItem.GoalItem> =
            goalDao.searchGoalsGlobal(query).mapNotNull { searchResult ->
                val listItem = listItemDao.getListItemByEntityId(searchResult.goal.id)
                listItem?.let {
                    GlobalSearchResultItem.GoalItem(
                        goal = searchResult.goal,
                        backlogItem = it,
                        projectName = searchResult.contextName,
                        pathSegments = searchResult.pathSegments,
                    )
                }
            }

        private suspend fun buildLinkResults(query: String): List<GlobalSearchResultItem.LinkItem> =
            linkItemDao.searchLinksGlobal(query).map { GlobalSearchResultItem.LinkItem(it) }

        private suspend fun buildSubcontextResults(query: String): List<GlobalSearchResultItem.SubcontextItem> =
            contextDao.searchSubprojectsGlobal(query).map { GlobalSearchResultItem.SubcontextItem(it) }

        private suspend fun buildContextResults(query: String): List<GlobalSearchResultItem.ContextItem> =
            contextDao.searchContextsGlobal(query).map { searchRow ->
                GlobalSearchResultItem.ContextItem(
                    searchRow.toSearchResult(query),
                )
            }

        private suspend fun buildActivityResults(activityQuery: String?): List<GlobalSearchResultItem.ActivityItem> =
            activityQuery
                ?.let { safeQuery ->
                    activityRepository.searchActivities(safeQuery).map { GlobalSearchResultItem.ActivityItem(it) }
                }
                ?: emptyList()

        private suspend fun buildInboxResults(query: String): List<GlobalSearchResultItem.InboxItem> =
            inboxRecordDao.searchInboxRecordsGlobal(query).map { GlobalSearchResultItem.InboxItem(it) }

        private suspend fun buildAttachmentResults(
            sanitizedQuery: String,
        ): List<GlobalSearchResultItem.AttachmentItem> =
            attachmentsRepository
                .getAttachmentLibraryItems()
                .first()
                .mapNotNull { result -> buildAttachmentSearchResult(result, sanitizedQuery) }

        private fun buildAttachmentSearchResult(
            result: AttachmentLibraryQueryResult,
            sanitizedQuery: String,
        ): GlobalSearchResultItem.AttachmentItem? {
            val title = resolveAttachmentTitle(result) ?: return null
            val subtitle = resolveAttachmentSubtitle(result)
            val matches = attachmentMatchesQuery(result, title, subtitle, sanitizedQuery)
            if (!matches) return null

            return GlobalSearchResultItem.AttachmentItem(
                GlobalAttachmentSearchResult(
                    attachmentId = result.id,
                    entityId = result.entityId,
                    attachmentType = result.attachmentType,
                    ownerContextId = result.ownerContextId,
                    title = title,
                    subtitle = subtitle,
                    contextName = result.contextName,
                    updatedAt =
                        result.linkCreatedAt
                            ?: result.noteUpdatedAt
                            ?: result.contextUpdatedAt
                            ?: result.attachmentUpdatedAt,
                ),
            )
        }

        private fun resolveAttachmentTitle(
            result: AttachmentLibraryQueryResult,
        ): String? =
            when {
                !result.noteName.isNullOrBlank() -> result.noteName
                !result.musicNoteName.isNullOrBlank() -> result.musicNoteName
                !result.checklistName.isNullOrBlank() -> result.checklistName
                !result.scriptName.isNullOrBlank() -> result.scriptName
                !result.contextName.isNullOrBlank() -> result.contextName
                else -> extractLinkTitle(result.linkDisplayName)
            }

        private fun resolveAttachmentSubtitle(
            result: AttachmentLibraryQueryResult,
        ): String? =
            if (!result.linkDisplayName.isNullOrBlank()) {
                extractLinkSubtitle(result.linkDisplayName)
            } else {
                result.contextName
            }

        private fun attachmentMatchesQuery(
            result: AttachmentLibraryQueryResult,
            title: String,
            subtitle: String?,
            sanitizedQuery: String,
        ): Boolean =
            sanitizedQuery.isBlank() ||
                title.contains(sanitizedQuery, ignoreCase = true) ||
                (subtitle?.contains(sanitizedQuery, ignoreCase = true) == true) ||
                (result.contextName?.contains(sanitizedQuery, ignoreCase = true) == true)

        private fun extractLinkTitle(linkDisplayName: String?): String? {
            val relatedLink =
                parseRelatedLink(linkDisplayName)
            return relatedLink?.displayName ?: relatedLink?.target
        }

        private fun extractLinkSubtitle(linkDisplayName: String?): String? {
            val relatedLink =
                parseRelatedLink(linkDisplayName)
            return relatedLink?.let { link ->
                when (link.type) {
                    LinkType.URL, LinkType.OBSIDIAN -> link.target
                    LinkType.CONTEXT -> link.displayName ?: link.target
                    LinkType.NOTE_DOCUMENT, LinkType.JOURNAL_DOCUMENT, LinkType.CHECKLIST, LinkType.MUSIC_NOTE -> link.displayName ?: link.target
                    null -> link.target
                }
            }
        }

        private fun parseRelatedLink(linkDisplayName: String?): RelatedLink? =
            linkDisplayName
                ?.takeIf { it.isNotBlank() }
                ?.let { safeLinkDisplayName ->
                    runCatching { Gson().fromJson(safeLinkDisplayName, RelatedLink::class.java) }.getOrNull()
                }

        private fun GlobalContextSearchRow.toSearchResult(query: String): GlobalContextSearchResult {
            val normalizedQuery = query.removePrefix("%").removeSuffix("%").trim()
            if (normalizedQuery.isBlank()) {
                return GlobalContextSearchResult(
                    context = context,
                    pathSegments = pathSegments,
                    matchedTags = emptyList(),
                )
            }
            val matchedTags =
                context.tags
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.contains(normalizedQuery, ignoreCase = true) }
                    .distinct()
            return GlobalContextSearchResult(
                context = context,
                pathSegments = pathSegments,
                matchedTags = matchedTags,
            )
        }
    }
