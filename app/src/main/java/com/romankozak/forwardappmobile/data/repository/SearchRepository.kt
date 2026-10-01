package com.romankozak.forwardappmobile.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalAttachmentSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalContextSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchContextPresentation
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSubcontextSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyPresentationProvenance
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyReadSnapshotAssembler
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2TargetNavigationPolicy
import com.romankozak.forwardappmobile.data.hierarchy.toCanonicalV2WorkspacePresentation
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.hierarchyPlacementSiblingComparator
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.domain.search.StructuredSearchQuery
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TYPE_ORDER_CONTEXT = 0
private const val TYPE_ORDER_GOAL = 1
private const val TYPE_ORDER_ATTACHMENT = 2
private const val TYPE_ORDER_OTHER = 3

private data class SearchContextPresentationNode(
    val presentation: GlobalSearchContextPresentation,
    val isDeleted: Boolean,
    val hasPersistedContext: Boolean,
    val legacyContextUpdatedAt: Long?,
)

internal class SearchContextPresentationSnapshot(
    presentations: List<ContextPresentation>,
    rawContexts: List<Context>,
    canonicalWorkspaceUpdatedAtById: Map<String, Long>,
    private val canonicalV2Read: CanonicalV2ProductionHierarchyRead,
) {
    private val rawContextsById = rawContexts.associateBy(Context::id)
    private val nodesById =
        presentations
            .mapNotNull { presentation ->
                presentation.toSearchNode(
                    rawContext = rawContextsById[presentation.id],
                    canonicalWorkspaceUpdatedAt = canonicalWorkspaceUpdatedAtById[presentation.id],
                )
            }.associateBy { it.presentation.id }

    fun presentation(contextId: String): GlobalSearchContextPresentation? {
        val presentation = nodesById[contextId]?.presentation ?: return null
        val occurrence = canonicalV2Read.navigationOccurrence(
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, contextId),
            CanonicalV2TargetNavigationPolicy.FIRST_VISIBLE,
        )
        val parent = occurrence?.let { canonicalV2Read.parentOccurrence(it.placementId) }
        return presentation.copy(
            parentId = parent?.takeIf { it.target.type == HierarchyTargetType.WORKSPACE }?.target?.id,
        )
    }

    fun legacyContextUpdatedAt(contextId: String): Long? =
        nodesById[contextId]?.legacyContextUpdatedAt

    fun hasPersistedContext(contextId: String): Boolean =
        nodesById[contextId]?.hasPersistedContext == true

    suspend fun pathSegments(contextId: String): List<String>? {
        val occurrence = canonicalV2Read.navigationOccurrence(
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, contextId),
            CanonicalV2TargetNavigationPolicy.FIRST_VISIBLE,
        ) ?: return null
        val breadcrumbs =
            canonicalV2Read
                .breadcrumbsToOccurrence(occurrence.placementId)
                .filter { it.placementId != null }
        return breadcrumbs.takeIf { it.isNotEmpty() }?.map { it.title }
    }

    suspend fun searchContexts(query: String): List<GlobalContextSearchResult> {
        val normalizedQuery = normalizeSearchQuery(query)
        val results = mutableListOf<GlobalContextSearchResult>()
        for (node in nodesById.values) {
            val presentation = presentation(node.presentation.id) ?: continue
            val matches =
                normalizedQuery.isBlank() ||
                    presentation.name.contains(normalizedQuery, ignoreCase = true) ||
                    presentation.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
            if (!matches) continue
            val path = pathSegments(presentation.id) ?: continue
            results +=
                GlobalContextSearchResult(
                    presentation = presentation,
                pathSegments = path,
                matchedTags =
                    if (normalizedQuery.isBlank()) {
                        emptyList()
                    } else {
                        presentation.tags
                            .map(String::trim)
                            .filter { it.isNotBlank() && it.contains(normalizedQuery, ignoreCase = true) }
                            .distinct()
                    },
                )
        }
        return results
    }

    suspend fun searchSubcontexts(query: String): List<GlobalSubcontextSearchResult> {
        val normalizedQuery = normalizeSearchQuery(query)
        val results = mutableListOf<GlobalSubcontextSearchResult>()
        for (node in nodesById.values) {
            val presentation = presentation(node.presentation.id) ?: continue
            val occurrence =
                canonicalV2Read.navigationOccurrence(
                    HierarchyTargetRef(HierarchyTargetType.WORKSPACE, presentation.id),
                    CanonicalV2TargetNavigationPolicy.FIRST_VISIBLE,
                ) ?: continue
            val parentOccurrence =
                canonicalV2Read.parentOccurrence(occurrence.placementId)
                    ?.takeIf { it.target.type == HierarchyTargetType.WORKSPACE }
                    ?: continue
            val parent =
                SearchPathNode(
                    id = parentOccurrence.target.id,
                    name = parentOccurrence.title,
                )
            if (node.isDeleted || nodesById[parent.id]?.isDeleted == true) continue
            if (
                normalizedQuery.isNotBlank() &&
                !presentation.name.contains(normalizedQuery, ignoreCase = true)
            ) {
                continue
            }
            val path = pathSegments(presentation.id) ?: continue
            results +=
                GlobalSubcontextSearchResult(
                    presentation = presentation,
                    parentContextId = parent.id,
                    parentContextName = parent.name,
                    pathSegments = path,
                )
        }
        return results
    }

}

private data class SearchPathNode(
    val id: String,
    val name: String,
)

private fun ContextPresentation.toSearchNode(
    rawContext: Context?,
    canonicalWorkspaceUpdatedAt: Long?,
): SearchContextPresentationNode? {
    val rankingTimestamp =
        rawContext?.updatedAt
            ?: rawContext?.createdAt
            ?: canonicalWorkspaceUpdatedAt
            ?: return null
    return SearchContextPresentationNode(
        presentation =
            GlobalSearchContextPresentation(
                id = id,
                name = name,
                description = description,
                // Search ancestry is resolved from the selected H1 occurrence
                // in SearchContextPresentationSnapshot.presentation().
                parentId = null,
                tags = tags.orEmpty(),
                rankingTimestamp = rankingTimestamp,
            ),
        isDeleted = rawContext?.isDeleted ?: false,
        hasPersistedContext = rawContext != null,
        legacyContextUpdatedAt = rawContext?.updatedAt,
    )
}

private fun normalizeSearchQuery(query: String): String =
    query.removePrefix("%").removeSuffix("%").trim()

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
        private val listItemRepository: ListItemRepository,
        private val linkItemDao: LinkItemDao,
        private val activityRepository: ActivityRepository,
        private val inboxRecordDao: InboxRecordDao,
        private val attachmentsRepository: AttachmentsRepository,
        private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
        private val workspaceDao: WorkspaceDao,
        private val database: AppDatabase,
        private val canonicalV2ReadAssembler: CanonicalV2HierarchyReadSnapshotAssembler,
    ) {
        /** Test seam: production always assembles from persisted canonical V2 state. */
        internal var canonicalV2SearchReadProvider: (suspend (List<ContextPresentation>) ->
            CanonicalV2ProductionHierarchyRead)? = null

        suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
            val structuredQuery = StructuredSearchQuery.parse(query)
            val contextPresentation = loadContextPresentation()
            if (structuredQuery.hasTags) {
                return searchStructured(structuredQuery, contextPresentation)
            }
            val sanitizedQuery = query.removePrefix("%").removeSuffix("%").trim()
            val activityQuery = buildSafeActivityFtsQuery(query)
            val combinedResults =
                buildGoalResults(query, contextPresentation) +
                    buildLinkResults(query, contextPresentation) +
                    buildSubcontextResults(query, contextPresentation) +
                    buildContextResults(query, contextPresentation) +
                    buildActivityResults(activityQuery) +
                    buildInboxResults(query) +
                    buildAttachmentResults(sanitizedQuery, contextPresentation)

            return combinedResults.sortedWith(
                compareBy<GlobalSearchResultItem> { it.typeOrder }
                    .thenByDescending { it.timestamp },
            )
        }

        private suspend fun searchStructured(
            query: StructuredSearchQuery,
            contextPresentation: SearchContextPresentationSnapshot,
        ): List<GlobalSearchResultItem> {
            val allQuery = "%%"
            val candidates =
                buildGoalResults(allQuery, contextPresentation) +
                    buildLinkResults(allQuery, contextPresentation) +
                    buildSubcontextResults(allQuery, contextPresentation) +
                    buildContextResults(allQuery, contextPresentation) +
                    buildAllActivityResults() +
                    buildInboxResults(allQuery) +
                    buildAttachmentResults("", contextPresentation)

            return candidates
                .asSequence()
                .filter { item -> query.matches(item.searchableTexts()) }
                .map { item -> item.withMatchedTags(query.matchedTags(item.searchableTexts())) }
                .distinctBy { it.uniqueId }
                .sortedWith(
                    compareByDescending<GlobalSearchResultItem> { it.matchedTags.size }
                        .thenBy { it.typeOrder }
                        .thenByDescending { it.timestamp },
                )
                .toList()
        }

        private suspend fun buildGoalResults(
            query: String,
            contextPresentation: SearchContextPresentationSnapshot,
        ): List<GlobalSearchResultItem.GoalItem> {
            val results = mutableListOf<GlobalSearchResultItem.GoalItem>()
            for (searchResult in goalDao.searchGoalsGlobal(query)) {
                val context = contextPresentation.presentation(searchResult.contextId) ?: continue
                val path = contextPresentation.pathSegments(context.id) ?: continue
                val listItem =
                    listItemRepository.getRuntimeItemForEntityInContext(
                        entityId = searchResult.goal.id,
                        itemType = BacklogItemTypeValues.GOAL,
                        contextId = searchResult.contextId,
                    )
                if (listItem != null) {
                    results +=
                        GlobalSearchResultItem.GoalItem(
                            goal = searchResult.goal,
                            backlogItem = listItem,
                            projectName = context.name,
                            pathSegments = path,
                        )
                }
            }
            return results
        }

        private suspend fun buildLinkResults(
            query: String,
            contextPresentation: SearchContextPresentationSnapshot,
        ): List<GlobalSearchResultItem.LinkItem> {
            val results = mutableListOf<GlobalSearchResultItem.LinkItem>()
            for (searchResult in linkItemDao.searchLinksGlobal(query)) {
                val context = contextPresentation.presentation(searchResult.contextId) ?: continue
                val path = contextPresentation.pathSegments(context.id) ?: continue
                results +=
                    GlobalSearchResultItem.LinkItem(
                        searchResult.copy(
                            contextName = context.name,
                            pathSegments = path,
                        ),
                    )
            }
            return results
        }

        private suspend fun buildSubcontextResults(
            query: String,
            contextPresentation: SearchContextPresentationSnapshot,
        ): List<GlobalSearchResultItem.SubcontextItem> =
            contextPresentation.searchSubcontexts(query).map(GlobalSearchResultItem::SubcontextItem)

        private suspend fun buildContextResults(
            query: String,
            contextPresentation: SearchContextPresentationSnapshot,
        ): List<GlobalSearchResultItem.ContextItem> =
            contextPresentation.searchContexts(query).map(GlobalSearchResultItem::ContextItem)

        private suspend fun buildActivityResults(activityQuery: String?): List<GlobalSearchResultItem.ActivityItem> =
            activityQuery
                ?.let { safeQuery ->
                    activityRepository.searchActivities(safeQuery).map { GlobalSearchResultItem.ActivityItem(it) }
                }
                ?: emptyList()

        private suspend fun buildAllActivityResults(): List<GlobalSearchResultItem.ActivityItem> =
            activityRepository.getAllActivitiesForSearch().map { GlobalSearchResultItem.ActivityItem(it) }

        private suspend fun buildInboxResults(query: String): List<GlobalSearchResultItem.InboxItem> =
            inboxRecordDao.searchInboxRecordsGlobal(query).map { GlobalSearchResultItem.InboxItem(it) }

        private suspend fun buildAttachmentResults(
            sanitizedQuery: String,
            contextPresentation: SearchContextPresentationSnapshot,
        ): List<GlobalSearchResultItem.AttachmentItem> =
            attachmentsRepository
                .getAttachmentLibraryItems()
                .first()
                .mapNotNull { result ->
                    val ownerContextId = result.ownerContextId
                    val presentedContext = ownerContextId?.let(contextPresentation::presentation)
                    val presentedResult =
                        when {
                            presentedContext != null &&
                                contextPresentation.hasPersistedContext(presentedContext.id) ->
                                result.copy(
                                    contextName = presentedContext.name,
                                    contextUpdatedAt =
                                        contextPresentation.legacyContextUpdatedAt(presentedContext.id),
                                )
                            presentedContext != null ->
                                // A shell-free promoted System owner still has a
                                // canonical display label, but no Context-history
                                // timestamp may be projected into this result.
                                result.copy(
                                    contextName = presentedContext.name,
                                    contextUpdatedAt = null,
                                )
                            ownerContextId != null && SystemContexts.isSystem(ContextId(ownerContextId)) ->
                                result.copy(contextName = null, contextUpdatedAt = null)
                            else -> result
                        }
                    buildAttachmentSearchResult(presentedResult, sanitizedQuery)
                }

        private suspend fun loadContextPresentation(): SearchContextPresentationSnapshot {
            val rawContexts = contextDao.getAllRaw()
            val workspaces = workspaceDao.getAll()
            val presentations =
                systemWorkspacePresentationContextProjector.projectPresentationUniverse(rawContexts)
            val canonicalV2Read =
                canonicalV2SearchReadProvider?.invoke(presentations)
                    ?: loadCanonicalV2SearchRead(presentations)
            return SearchContextPresentationSnapshot(
                presentations = presentations,
                rawContexts = rawContexts,
                canonicalWorkspaceUpdatedAtById =
                    workspaces.associate { workspace -> workspace.id to workspace.updatedAt },
                canonicalV2Read = canonicalV2Read,
            )
        }

        private suspend fun loadCanonicalV2SearchRead(
            presentations: List<ContextPresentation>,
        ): CanonicalV2ProductionHierarchyRead = database.withTransaction {
            val placements =
                database.hierarchyPlacementDao().getLiveHierarchy(HierarchyId.GENERAL.value)
                    .map { it.toHierarchyPlacementStrict() }
                    .sortedWith(hierarchyPlacementSiblingComparator)
            canonicalV2ReadAssembler.assemble(
                placements = placements,
                admittedWorkspacePresentations =
                    presentations.map { it.toCanonicalV2WorkspacePresentation() },
                managedSubjects = database.orientationDao().getAllManagedSubjects(),
                legacySubjectMappings = database.orientationDao().getAllLegacyMappings(),
                relations = database.orientationDao().getAllOrientationRelations(),
                groupScopes =
                    database.hierarchyPlacementGroupScopeDao()
                        .getLiveForHierarchy(HierarchyId.GENERAL.value),
                legacyGroups = database.mainBeaconDao().getAllGroupsSync(),
                presentationProvenance = CanonicalV2HierarchyPresentationProvenance(
                    linkedAppearancePlacementIds =
                        database.hierarchyPlacementLinkedAppearanceDao()
                            .getLiveForHierarchy(HierarchyId.GENERAL.value)
                            .mapTo(linkedSetOf()) { PlacementId(it.placementId) },
                ),
            )
        }

        private fun buildAttachmentSearchResult(
            result: AttachmentLibraryQueryResult,
            sanitizedQuery: String,
        ): GlobalSearchResultItem.AttachmentItem? {
            val title = resolveAttachmentTitle(result) ?: return null
            val subtitle = resolveAttachmentSubtitle(result)
            val searchText = resolveAttachmentSearchText(result)
            val matches = attachmentMatchesQuery(result, title, subtitle, searchText, sanitizedQuery)
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
                    searchText = searchText,
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

        private fun resolveAttachmentSearchText(result: AttachmentLibraryQueryResult): String? =
            listOfNotNull(
                result.noteContent,
                result.musicNoteContent,
                result.checklistContent,
                result.scriptDescription,
                result.scriptContent,
            ).filter { it.isNotBlank() }
                .joinToString("\n")
                .ifBlank { null }

        private fun attachmentMatchesQuery(
            result: AttachmentLibraryQueryResult,
            title: String,
            subtitle: String?,
            searchText: String?,
            sanitizedQuery: String,
        ): Boolean =
            sanitizedQuery.isBlank() ||
                title.contains(sanitizedQuery, ignoreCase = true) ||
                (subtitle?.contains(sanitizedQuery, ignoreCase = true) == true) ||
                (result.contextName?.contains(sanitizedQuery, ignoreCase = true) == true) ||
                (searchText?.contains(sanitizedQuery, ignoreCase = true) == true)

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
                    LinkType.NOTE_DOCUMENT, LinkType.CHECKLIST, LinkType.MUSIC_NOTE -> link.displayName ?: link.target
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

        private fun GlobalSearchResultItem.searchableTexts(): List<String> =
            when (this) {
                is GlobalSearchResultItem.GoalItem ->
                    listOf(
                        goal.text,
                        goal.description.orEmpty(),
                        projectName,
                        pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.LinkItem ->
                    listOf(
                        searchResult.link.linkData.displayName.orEmpty(),
                        searchResult.link.linkData.target,
                        searchResult.contextName,
                        searchResult.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.SubcontextItem ->
                    listOf(
                        searchResult.presentation.name,
                        searchResult.presentation.description.orEmpty(),
                        searchResult.presentation.tags.joinToString(" "),
                        searchResult.parentContextName,
                        searchResult.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.ContextItem ->
                    listOf(
                        searchResult.presentation.name,
                        searchResult.presentation.description.orEmpty(),
                        searchResult.presentation.tags.joinToString(" "),
                        searchResult.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.ActivityItem ->
                    listOf(record.text, record.noteText.orEmpty(), record.rawNoteText.orEmpty())
                is GlobalSearchResultItem.InboxItem -> listOf(record.text)
                is GlobalSearchResultItem.AttachmentItem ->
                    listOf(
                        searchResult.title,
                        searchResult.subtitle.orEmpty(),
                        searchResult.contextName.orEmpty(),
                        searchResult.searchText.orEmpty(),
                    )
            }

        private fun GlobalSearchResultItem.withMatchedTags(tags: List<String>): GlobalSearchResultItem =
            when (this) {
                is GlobalSearchResultItem.GoalItem -> copy(matchedTags = tags)
                is GlobalSearchResultItem.LinkItem -> copy(matchedTags = tags)
                is GlobalSearchResultItem.SubcontextItem -> copy(matchedTags = tags)
                is GlobalSearchResultItem.ContextItem ->
                    copy(
                        searchResult = searchResult.copy(matchedTags = tags),
                        matchedTags = tags,
                    )
                is GlobalSearchResultItem.ActivityItem -> copy(matchedTags = tags)
                is GlobalSearchResultItem.InboxItem -> copy(matchedTags = tags)
                is GlobalSearchResultItem.AttachmentItem -> copy(matchedTags = tags)
            }
    }
