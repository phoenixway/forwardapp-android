package com.romankozak.forwardappmobile.data.repository

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalAttachmentSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

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
            -> 0
            is GlobalSearchResultItem.GoalItem -> 1
            is GlobalSearchResultItem.AttachmentItem -> 2
            else -> 3
        }

@Singleton
class SearchRepository
    @Inject
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
            val goalResults =
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
            val linkResults =
                linkItemDao.searchLinksGlobal(query).map {
                    GlobalSearchResultItem.LinkItem(it)
                }
            val subprojectResults =
                contextDao.searchSubprojectsGlobal(query).map {
                    GlobalSearchResultItem.SubcontextItem(it)
                }
            val projectResults =
                contextDao.searchContextsGlobal(query).map {
                    GlobalSearchResultItem.ContextItem(it)
                }
            val activityResults =
                activityQuery
                    ?.let { safeQuery ->
                        activityRepository.searchActivities(safeQuery).map {
                            GlobalSearchResultItem.ActivityItem(it)
                        }
                    }
                    ?: emptyList()
            val inboxResults =
                inboxRecordDao.searchInboxRecordsGlobal(query).map {
                    GlobalSearchResultItem.InboxItem(it)
                }
            val attachmentResults =
                attachmentsRepository
                    .getAttachmentLibraryItems()
                    .first()
                    .mapNotNull { result ->
                        val title =
                            when {
                                !result.noteName.isNullOrBlank() -> result.noteName
                                !result.musicNoteName.isNullOrBlank() -> result.musicNoteName
                                !result.checklistName.isNullOrBlank() -> result.checklistName
                                !result.scriptName.isNullOrBlank() -> result.scriptName
                                !result.contextName.isNullOrBlank() -> result.contextName
                                else -> extractLinkTitle(result.linkDisplayName)
                            } ?: return@mapNotNull null

                        val subtitle =
                            when {
                                !result.linkDisplayName.isNullOrBlank() -> extractLinkSubtitle(result.linkDisplayName)
                                else -> result.contextName
                            }

                        val matches =
                            sanitizedQuery.isBlank() ||
                                title.contains(sanitizedQuery, ignoreCase = true) ||
                                (subtitle?.contains(sanitizedQuery, ignoreCase = true) == true) ||
                                (result.contextName?.contains(sanitizedQuery, ignoreCase = true) == true)

                        if (!matches) return@mapNotNull null

                        GlobalSearchResultItem.AttachmentItem(
                            GlobalAttachmentSearchResult(
                                attachmentId = result.id,
                                entityId = result.entityId,
                                attachmentType = result.attachmentType,
                                ownerContextId = result.ownerContextId,
                                title = title,
                                subtitle = subtitle,
                                contextName = result.contextName,
                                updatedAt = result.linkCreatedAt ?: result.noteUpdatedAt ?: result.contextUpdatedAt ?: result.attachmentUpdatedAt,
                            ),
                        )
                    }

            val combinedResults = (goalResults + linkResults + subprojectResults + projectResults + activityResults + inboxResults + attachmentResults)

            return combinedResults.sortedWith(
                compareBy<GlobalSearchResultItem> { it.typeOrder }
                    .thenByDescending { it.timestamp },
            )
        }

        private fun extractLinkTitle(linkDisplayName: String?): String? {
            if (linkDisplayName.isNullOrBlank()) return null
            val relatedLink =
                runCatching { Gson().fromJson(linkDisplayName, RelatedLink::class.java) }
                    .getOrNull()
                    ?: return null
            return relatedLink.displayName ?: relatedLink.target
        }

        private fun extractLinkSubtitle(linkDisplayName: String?): String? {
            if (linkDisplayName.isNullOrBlank()) return null
            val relatedLink =
                runCatching { Gson().fromJson(linkDisplayName, RelatedLink::class.java) }
                    .getOrNull()
                    ?: return null
            return when (relatedLink.type) {
                LinkType.URL, LinkType.OBSIDIAN -> relatedLink.target
                LinkType.CONTEXT -> relatedLink.displayName ?: relatedLink.target
                null -> relatedLink.target
            }
        }
    }
