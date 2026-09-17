package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.isDirectHierarchyChildContext
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLinkProjectReadModel
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceHierarchyUpdate
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

enum class ContextTextAction { ADD, REMOVE }

internal data class ContextHierarchyUpdate(
    val id: String,
    val parentId: String?,
    val order: Long,
)

internal data class ContextSharedStateUpdate(
    val name: String,
    val description: String?,
    val contextStatus: String,
    val defaultViewModeName: String,
    val isCompleted: Boolean,
)

internal data class ContextSettingsUpdate(
    val name: String,
    val description: String?,
    val relatedLinks: List<RelatedLink>,
    val showCheckboxes: Boolean,
    val isContextManagementEnabled: Boolean,
    val valueImportance: Float,
    val valueImpact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
    val weightEffort: Float,
    val weightCost: Float,
    val weightRisk: Float,
    val rawScore: Float,
    val displayScore: Int,
    val scoringStatus: String,
)

@Singleton
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class ContextRepository
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val contextTagRefDao: ContextTagRefDao,
        private val legacyNoteRepository: LegacyNoteRepository,
        private val activityRepository: ActivityRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val reminderRepository: ReminderRepository,
        private val contextLogRepository: ContextLogRepository,
        private val searchRepository: SearchRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val attachmentRepository: AttachmentsRepository,
        private val goalRepository: GoalRepository,
        private val contextTimeTrackingRepository: ContextTimeTrackingRepository,
        private val listItemRepository: ListItemRepository,
        private val backlogPlacementCommands: BacklogPlacementCommands,
        private val contextStructureDao: ContextStructureDao,
        private val structurePresetDao: StructurePresetDao,
        private val directionRepository: DirectionRepository,
        private val aiEventRepository: AiEventRepository,
        // ДОДАНО: Потрібен провайдер для уникнення циклічної залежності
        private val contextMarkerHandlerProvider: Provider<ContextMarkerHandler>,
        private val tagAssociationHandler: TagAssociationHandler,
        private val workspaceWriteThrough: ContextWorkspaceWriteThrough,
        private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
        private val canonicalWorkspaceTagRepository: CanonicalWorkspaceTagRepository,
        private val systemWorkspaceTagAuthority: SystemWorkspaceTagAuthority,
        private val systemContextCanonicalInboxDirectionAccess: SystemContextCanonicalInboxDirectionAccess,
        private val canonicalKeyProblemsRepository: CanonicalKeyProblemsRepository,
        private val canonicalInboxRepository: CanonicalInboxRepository,
        private val canonicalConnectionsRepository: CanonicalConnectionsRepository,
        private val canonicalBacklogRepository: CanonicalBacklogRepository,
        private val backlogPresentationLifecycle: BacklogPresentationLifecycle,
    ) {
        private val contextMarkerHandler: ContextMarkerHandler by lazy { contextMarkerHandlerProvider.get() }

        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> get() = contextMarkerHandler.contextMarkerToEmojiMap
        val contextMarkerNamesFlow: StateFlow<List<String>> get() = contextMarkerHandler.contextMarkerNamesFlow
        private val internalHandler: ContextMarkerHandler by lazy { contextMarkerHandlerProvider.get() }

        private data class ListItemContentInput(
            val contextId: String,
            val items: List<BacklogItem>,
            val backlogOrders: List<BacklogOrder>,
            val attachments: List<AttachmentWithContext>,
            val reminders: List<Reminder>,
            val goals: List<Goal>,
            val contexts: List<Context>,
            val links: List<LinkItemEntity>,
            val notes: List<LegacyNoteEntity>,
            val noteDocuments: List<NoteDocumentEntity>,
            val musicNotes: List<MusicNoteEntity>,
            val checklists: List<ChecklistEntity>,
        )

        private data class AttachmentLinkRequest(
            val attachmentType: String,
            val entityId: String,
            val targetContextId: String,
            val ownerContextId: String?,
            val createdAt: Long,
            val roleCode: String?,
            val isSystem: Boolean,
        )

        private data class BacklogLookupMaps(
            val remindersMap: Map<String, List<Reminder>>,
            val goalsMap: Map<String, Goal>,
            val contextsMap: Map<String, Context>,
            val linksMap: Map<String, LinkItemEntity>,
            val notesMap: Map<String, LegacyNoteEntity>,
            val noteDocumentsMap: Map<String, NoteDocumentEntity>,
            val musicNotesMap: Map<String, MusicNoteEntity>,
            val checklistsMap: Map<String, ChecklistEntity>,
        )

        fun getContextTag(contextName: String): String? = contextMarkerHandler.getContextTag(contextName)

        // --- Базові операції з Контекстами ---
        fun getAllContextsFlow(): Flow<List<Context>> =
            contextDao.getAllContexts().map {
                    list ->
                list.map { it.withNormalizedParentId() }
            }

        suspend fun getContextById(id: String): Context? = contextDao.getContextById(id)?.withNormalizedParentId()

        fun getContextByIdFlow(id: String): Flow<Context?> = contextDao.getContextByIdStream(id).map { it?.withNormalizedParentId() }

        private fun Context.withNormalizedParentId(): Context {
            val normalized = parentId?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            return if (normalized != parentId) copy(parentId = normalized) else this
        }

        fun getContextContentStream(contextId: String): Flow<List<BacklogItemContent>> {
            return combine(
                listItemRepository.getItemsForContextStream(contextId),
                reminderRepository.getAllReminders(),
                goalRepository.getAllGoalsFlow(),
                contextDao.getAllContexts(),
                listItemRepository.getAllEntitiesAsFlow(),
                legacyNoteRepository.getAllAsFlow(),
                noteDocumentRepository.getAllDocumentsAsFlow(),
                musicNoteRepository.getAllMusicNotesAsFlow(),
                checklistRepository.getAllChecklistsAsFlow(),
                attachmentRepository.getAttachmentsForContext(contextId),
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                mapToListItemContent(
                    input =
                        ListItemContentInput(
                            contextId = contextId,
                            items = array[0] as List<BacklogItem>,
                            backlogOrders = emptyList(),
                            attachments = array[9] as List<AttachmentWithContext>,
                            reminders = array[1] as List<Reminder>,
                            goals = array[2] as List<Goal>,
                            contexts = array[3] as List<Context>,
                            links = array[4] as List<LinkItemEntity>,
                            notes = array[5] as List<LegacyNoteEntity>,
                            noteDocuments = array[6] as List<NoteDocumentEntity>,
                            musicNotes = array[7] as List<MusicNoteEntity>,
                            checklists = array[8] as List<ChecklistEntity>,
                        ),
                )
            }
        }

        fun getAttachmentsForContextStream(contextId: String): Flow<List<AttachmentWithContext>> {
            return attachmentRepository.getAttachmentsForContext(contextId)
        }

        fun getAttachmentLibraryItemsFlow(): Flow<List<AttachmentLibraryQueryResult>> {
            return attachmentRepository.getAttachmentLibraryItems()
        }

        private fun mapToListItemContent(
            input: ListItemContentInput,
        ): List<BacklogItemContent> {
            val attachmentBacklogItems =
                input.attachments.map { attachment ->
                    val order = attachment.attachmentOrder ?: -attachment.attachment.createdAt
                    BacklogItem(
                        id = attachment.attachment.id,
                        contextId = input.contextId,
                        itemType = attachment.attachment.attachmentType,
                        entityId = attachment.attachment.entityId,
                        order = order,
                    )
                }
            val orderOverrideMap = input.backlogOrders.associateBy { it.itemId to it.listId }

            val combinedItems =
                (input.items + attachmentBacklogItems).sortedWith { a, b ->
                    val orderA = orderOverrideMap[a.entityId to a.contextId]?.order ?: a.order
                    val orderB = orderOverrideMap[b.entityId to b.contextId]?.order ?: b.order
                    if (orderA != orderB) orderA.compareTo(orderB) else a.id.compareTo(b.id)
                }

            val lookupMaps =
                BacklogLookupMaps(
                    remindersMap = input.reminders.groupBy { it.entityId },
                    goalsMap = input.goals.associateBy { it.id },
                    contextsMap = input.contexts.associateBy { it.id },
                    linksMap = input.links.associateBy { it.id },
                    notesMap = input.notes.associateBy { it.id },
                    noteDocumentsMap = input.noteDocuments.associateBy { it.id },
                    musicNotesMap = input.musicNotes.associateBy { it.id },
                    checklistsMap = input.checklists.associateBy { it.id },
                )

            return combinedItems.mapNotNull { item ->
                item.toBacklogItemContent(lookupMaps)
            }
        }

        private fun BacklogItem.toBacklogItemContent(
            lookupMaps: BacklogLookupMaps,
        ): BacklogItemContent? =
            when (itemType) {
                BacklogItemTypeValues.GOAL -> toGoalBacklogItemContent(lookupMaps)
                BacklogItemTypeValues.SUBLIST -> toSublistBacklogItemContent(lookupMaps)
                BacklogItemTypeValues.LINK_ITEM -> lookupMaps.linksMap[entityId]?.let { BacklogItemContent.LinkItem(it, this) }
                BacklogItemTypeValues.NOTE -> lookupMaps.notesMap[entityId]?.let { BacklogItemContent.NoteItem(it, this) }
                BacklogItemTypeValues.NOTE_DOCUMENT ->
                    lookupMaps.noteDocumentsMap[entityId]?.let { BacklogItemContent.NoteDocumentItem(it, this) }
                BacklogItemTypeValues.MUSIC_NOTE ->
                    lookupMaps.musicNotesMap[entityId]?.let { BacklogItemContent.MusicNoteItem(it, this) }
                BacklogItemTypeValues.CHECKLIST ->
                    lookupMaps.checklistsMap[entityId]?.let { BacklogItemContent.ChecklistItem(it, this) }
                else -> null
            }

        private fun BacklogItem.toGoalBacklogItemContent(
            lookupMaps: BacklogLookupMaps,
        ): BacklogItemContent.GoalItem? =
            lookupMaps.goalsMap[entityId]?.let { goal ->
                BacklogItemContent.GoalItem(goal, lookupMaps.remindersMap[goal.id] ?: emptyList(), this)
            }

        private fun BacklogItem.toSublistBacklogItemContent(
            lookupMaps: BacklogLookupMaps,
        ): BacklogItemContent.ContextLinkItem? =
            lookupMaps.contextsMap[entityId]?.let { context ->
                BacklogItemContent.ContextLinkItem(
                    project = ContextLinkProjectReadModel.fromLegacyContext(context),
                    reminders = lookupMaps.remindersMap[context.id] ?: emptyList(),
                    backlogItem = this,
                    legacyProject = context,
                )
            }

        // --- Операції переміщення та логіки ---
        @Transaction
        suspend fun moveContextById(
            contextId: String,
            newParentId: String?,
            allowSystemMoves: Boolean = false,
        ) {
            val stableId = ContextId(contextId)
            val isSystem = SystemContexts.isSystem(stableId)
            if (isSystem) {
                if (!allowSystemMoves) return
                if (SystemContexts.isPinnedRoot(stableId) && newParentId != null) return
                canonicalWorkspaceRepository.movePreservingOrder(
                    id = contextId,
                    newParentWorkspaceId = newParentId,
                )
                return
            }

            val persisted = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            val oldParentId = persisted.parentId
            if (oldParentId == newParentId) return

            // Hierarchy ownership lives only on Context.parentId/Context.order.
            // Backlog SUBLIST is reserved for explicit user-created context references.
            val mutationNow = System.currentTimeMillis()
            val updatedContext = persisted.copy(parentId = newParentId).bumpSync(mutationNow)
            val persistedUpdate =
                workspaceWriteThrough.mutate(
                    now = mutationNow,
                    mutation = {
                        contextDao.update(updatedContext)
                        updatedContext
                    },
                )
            ensureDirectionFrontLinkForParentChangeIfNeeded(
                oldParentId = oldParentId,
                newParentId = persistedUpdate.parentId,
                childId = persistedUpdate.id,
                childName = persistedUpdate.name,
            )
        }

        // --- Делегати для інших репозиторіїв (ViewModels їх шукають тут) ---
        suspend fun findContextIdsByTag(tag: String) =
            tagAssociationHandler
                .normalizeTags(listOf(tag))
                .firstOrNull()
                ?.let { normalized -> contextTagRefDao.findContextIdsByTag(normalized) }
                ?: emptyList()

        suspend fun doesLinkToContextExist(
            eId: String,
            cId: String,
        ) = listItemRepository.doesLinkExist(eId, cId)

        suspend fun deleteLinkByEntityIdAndContextId(
            eId: String,
            cId: String,
        ) = backlogPlacementCommands.setContextBackedTargetVisible(
            contextId = cId,
            itemType = BacklogItemTypeValues.SUBLIST,
            entityId = eId,
            visible = false,
        )

        suspend fun addContextLinkToContext(
            cId: String,
            pId: String,
        ) = backlogPlacementCommands.addContextLinkToContextBacked(
            targetContextId = cId,
            currentContextId = pId,
        )

        suspend fun restoreListItems(items: List<BacklogItem>) =
            backlogPresentationLifecycle.restore(items)

        suspend fun deleteGoal(id: String) = goalRepository.deleteGoal(id)

        suspend fun copyGoalsToContext(
            ids: List<String>,
            target: String,
        ) = goalRepository.copyGoalsToContext(ids, target)

        suspend fun findContextIdForGoal(id: String) = goalRepository.findContextIdForGoal(id)

        suspend fun getAllGoals() = goalRepository.getAllGoals()



        /**
         * Safety-net sync: при відкритті контексту гарантує, що активні дочірні контексти
         * мають посилання у front списку direction (якщо флаг авто-додавання увімкнено).
         */
        suspend fun ensureDirectionFrontLinksForExistingChildren(parentContextId: String): Int {
            val normalizedParentId = normalizeParentId(parentContextId) ?: return 0
            val parentStructure = contextStructureDao.getStructureByContext(normalizedParentId)
            val autoAddToDirectionFront =
                systemContextCanonicalInboxDirectionAccess.directionAutoLinkEnabled(normalizedParentId)
                    ?: (parentStructure?.enableAutoLinkSubprojects == true)
            if (!autoAddToDirectionFront) return 0

            val children = contextDao.getActiveContextsByParentId(normalizedParentId)
            if (children.isEmpty()) return 0

            val existingLinkedIds =
                directionRepository.getDirectionItemsForContextSync(normalizedParentId)
                    .mapNotNull { it.linkedContextId }
                    .toMutableSet()
            var added = 0
            for (child in children) {
                if (child.id in existingLinkedIds) continue
                addChildContextToDirectionFront(
                    parentContextId = normalizedParentId,
                    childContextId = child.id,
                    childContextName = child.name,
                )
                existingLinkedIds += child.id
                added += 1
            }
            return added
        }

        // --- Time Metrics ---



        suspend fun calculateContextTimeMetrics(id: String) = contextTimeTrackingRepository.calculateContextTimeMetrics(id)

        suspend fun recalculateAndLogContextTime(id: String) = contextTimeTrackingRepository.recalculateAndLogContextTime(id)

// Усередині класу ContextRepository додайте ці пропущені методи:

        suspend fun toggleContextManagement(
            id: String,
            enabled: Boolean,
        ) {
            val context = contextDao.getContextById(id)?.takeUnless { it.isDeleted } ?: return
            if (!SystemContexts.isSystem(ContextId(id))) {
                persistContextUpdate(
                    context = context.copy(isContextManagementEnabled = enabled),
                    previous = context,
                )
            }
            contextLogRepository.addToggleContextManagementLog(id, enabled)
        }

        suspend fun updateContextStatus(
            id: String,
            status: String,
            text: String?,
        ) {
            val context = contextDao.getContextById(id)?.takeUnless { it.isDeleted } ?: return
            if (!SystemContexts.isSystem(ContextId(id))) {
                persistContextUpdate(
                    context = context.copy(contextStatus = status, contextStatusText = text),
                    previous = context,
                )
            }
            contextLogRepository.addUpdateContextStatusLog(id, status, text)
        }

        suspend fun updateContextPresentation(
            contextId: String,
            name: String,
            description: String?,
        ) {
            if (SystemContexts.isSystem(ContextId(contextId))) {
                canonicalWorkspaceRepository.updateNameAndDescription(
                    id = contextId,
                    nameOverride = name,
                    descriptionOverride = description,
                )
                return
            }

            val current =
                contextDao.getContextById(contextId)
                    ?.takeUnless { it.isDeleted }
                    ?: return
            persistContextUpdate(
                context = current.copy(name = name, description = description),
                previous = current,
            )
        }

        internal suspend fun updateContextSharedState(
            contextId: String,
            update: ContextSharedStateUpdate,
        ) {
            if (SystemContexts.isSystem(ContextId(contextId))) return

            val current = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            persistContextUpdate(
                context =
                    current.copy(
                        name = update.name,
                        description = update.description,
                        contextStatus = update.contextStatus,
                        defaultViewModeName = update.defaultViewModeName,
                        isCompleted = update.isCompleted,
                    ),
                previous = current,
            )
        }

        internal suspend fun updateContextSettings(
            contextId: String,
            update: ContextSettingsUpdate,
        ) {
            if (SystemContexts.isSystem(ContextId(contextId))) return

            val current = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            persistContextUpdate(
                context =
                    current.copy(
                        name = update.name,
                        description = update.description,
                        relatedLinks = update.relatedLinks,
                        showCheckboxes = update.showCheckboxes,
                        isContextManagementEnabled = update.isContextManagementEnabled,
                        valueImportance = update.valueImportance,
                        valueImpact = update.valueImpact,
                        effort = update.effort,
                        cost = update.cost,
                        risk = update.risk,
                        weightEffort = update.weightEffort,
                        weightCost = update.weightCost,
                        weightRisk = update.weightRisk,
                        rawScore = update.rawScore,
                        displayScore = update.displayScore,
                        scoringStatus = update.scoringStatus,
                    ),
                previous = current,
            )
        }

        suspend fun updateContextCompleted(
            contextId: String,
            completed: Boolean,
        ) {
            if (SystemContexts.isSystem(ContextId(contextId))) return

            val current = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            if (current.isCompleted == completed) return
            persistContextUpdate(
                context = current.copy(isCompleted = completed),
                previous = current,
            )
        }

        suspend fun toggleContextAttachmentsExpanded(contextId: String) {
            if (SystemContexts.isSystem(ContextId(contextId))) return

            val current = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            persistContextUpdate(
                context = current.copy(isAttachmentsExpanded = !current.isAttachmentsExpanded),
                previous = current,
            )
        }

        suspend fun updateContextRole(
            contextId: String,
            roleCode: String?,
        ) {
            if (SystemContexts.isSystem(ContextId(contextId))) {
                canonicalWorkspaceRepository.updateRole(
                    id = contextId,
                    roleCode = roleCode,
                )
                return
            }

            val current = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            if (current.roleCode == roleCode) return
            persistContextUpdate(
                context = current.copy(roleCode = roleCode),
                previous = current,
            )
        }

        suspend fun updateContextTags(
            contextId: String,
            tags: List<String>,
        ) {
            if (SystemContexts.isSystem(ContextId(contextId))) {
                canonicalWorkspaceTagRepository.replaceTags(
                    workspaceId = contextId,
                    tags = tags,
                )
                return
            }

            val context = contextDao.getContextById(contextId)?.takeUnless { it.isDeleted } ?: return
            persistContextUpdate(
                context = context.copy(tags = tags),
                previous = context,
            )
        }

        private suspend fun persistContextUpdate(
            context: Context,
            previous: Context,
        ) {
            val now = System.currentTimeMillis()
            val bumped = context.bumpSync(now)

            val persisted =
                workspaceWriteThrough.mutate(
                    now = now,
                    mutation = {
                        val tagAuthoritative =
                            systemWorkspaceTagAuthority.reconcileBeforeContextWrite(
                                context = bumped,
                                tagsWereExplicitlyChanged = context.tags != previous.tags,
                                now = now,
                            )
                        contextDao.update(tagAuthoritative)
                        tagAuthoritative
                    },
                )

            tagAssociationHandler.syncContextTags(persisted, previous.tags)
            ensureDirectionFrontLinkForParentChangeIfNeeded(
                oldParentId = previous.parentId,
                newParentId = persisted.parentId,
                childId = persisted.id,
                childName = persisted.name,
            )

            recentItemsRepository.updateRecentItemDisplayName(
                persisted.id,
                persisted.name,
            )
        }

        internal suspend fun applyHierarchyUpdates(updates: List<ContextHierarchyUpdate>): Int {
            if (updates.isEmpty()) return 0

            val systemUpdates =
                updates.filter { SystemContexts.isSystem(ContextId(it.id)) }
            canonicalWorkspaceRepository.updateHierarchyBatch(
                systemUpdates.map { update ->
                    CanonicalWorkspaceHierarchyUpdate(
                        id = update.id,
                        parentWorkspaceId = update.parentId,
                        workspaceOrder = update.order,
                    )
                },
            )

            val ordinaryUpdates =
                updates.filterNot { SystemContexts.isSystem(ContextId(it.id)) }
            if (ordinaryUpdates.isEmpty()) return systemUpdates.size

            val currentById =
                contextDao
                    .getContextsByIds(ordinaryUpdates.map { it.id })
                    .associateBy { it.id }
            val writableUpdates =
                ordinaryUpdates.mapNotNull { update ->
                    currentById[update.id]
                        ?.takeUnless { it.isDeleted }
                        ?.let { current -> update to current }
                }
            if (writableUpdates.isEmpty()) return systemUpdates.size

            val now = System.currentTimeMillis()
            val persistedUpdates =
                writableUpdates.map { (update, current) ->
                    current.copy(
                        parentId = update.parentId,
                        order = update.order,
                    ).bumpSync(now)
                }
            val updated =
                workspaceWriteThrough.mutate(
                    now = now,
                    mutation = {
                        contextDao.update(persistedUpdates)
                    },
                )

            persistedUpdates.forEach { persisted ->
                val previous = requireNotNull(currentById[persisted.id])
                ensureDirectionFrontLinkForParentChangeIfNeeded(
                    oldParentId = previous.parentId,
                    newParentId = persisted.parentId,
                    childId = persisted.id,
                    childName = persisted.name,
                )
            }
            return systemUpdates.size + updated
        }

        suspend fun addContextComment(
            id: String,
            text: String,
        ) = contextLogRepository.addContextComment(id, text)

        suspend fun updateContextViewMode(
            id: String,
            mode: ContextViewMode,
        ) {
            val context = contextDao.getContextById(id)?.takeUnless { it.isDeleted } ?: return
            if (SystemContexts.isSystem(ContextId(id))) return
            persistContextUpdate(
                context = context.copy(defaultViewModeName = mode.name),
                previous = context,
            )
        }

        suspend fun deleteContextsByIds(contextIds: Collection<String>) {
            // A retired Context remains readable as a compatibility shell, but
            // legacy lifecycle commands must not mutate it after canonical cutover.
            val candidateIds =
                contextIds
                    .asSequence()
                    .distinct()
                    .filterNot { SystemContexts.isSystem(ContextId(it)) }
                    .toList()
            if (candidateIds.isEmpty()) return

            val contextsToDelete =
                contextDao
                    .getContextsByIds(candidateIds)
                    .filterNot { it.isDeleted }
            if (contextsToDelete.isEmpty()) return

            val ids = contextsToDelete.map { it.id }
            rebindSharedAttachmentEntitiesBeforeContextDeletion(ids.toSet())
            val now = System.currentTimeMillis()
            workspaceWriteThrough.mutate(now) {
                directionRepository.deleteWorkspaceLinksTargeting(ids, now)
                directionRepository.deleteDirectionsOwnedByWorkspaces(ids, now)
                canonicalKeyProblemsRepository.tombstoneOwnedContentForWorkspaces(ids, now)
                canonicalInboxRepository.tombstoneOwnedContentForWorkspaces(ids, now)
                canonicalConnectionsRepository.tombstoneOwnedContentForWorkspaces(ids, now)
                canonicalBacklogRepository.tombstoneOwnedContentForWorkspaces(ids, now)
                contextLogRepository.tombstoneOwnedContentForWorkspaces(ids, now)
                contextsToDelete.forEach { contextDao.insert(it.softDelete(now)) }
            }
        }

        private suspend fun rebindSharedAttachmentEntitiesBeforeContextDeletion(deletingContextIds: Set<String>) {
            if (deletingContextIds.isEmpty()) return

            val activeContextIds = getActiveContextIdsExcluding(deletingContextIds)
            if (activeContextIds.isEmpty()) return

            val linksByAttachmentId =
                attachmentRepository
                    .getAllAttachmentLinks()
                    .first()
                    .groupBy { it.attachmentId }
            if (linksByAttachmentId.isEmpty()) return

            val now = System.currentTimeMillis()
            linksByAttachmentId.forEach { (attachmentId, links) ->
                val attachment = attachmentRepository.getAttachmentById(attachmentId) ?: return@forEach
                val ownerContextId = attachment.ownerContextId ?: return@forEach
                if (ownerContextId !in deletingContextIds) return@forEach

                val fallbackContextId = links.map { it.contextId }.firstOrNull { it in activeContextIds } ?: return@forEach
                rebindAttachmentEntityOwnerContext(
                    attachmentType = attachment.attachmentType,
                    entityId = attachment.entityId,
                    fallbackContextId = fallbackContextId,
                    now = now,
                )
            }
        }

        private suspend fun getActiveContextIdsExcluding(deletingContextIds: Set<String>): Set<String> =
            contextDao
                .getAll()
                .asSequence()
                .filter { !it.isDeleted && it.id !in deletingContextIds }
                .map { it.id }
                .toSet()

        private suspend fun rebindAttachmentEntityOwnerContext(
            attachmentType: String,
            entityId: String,
            fallbackContextId: String,
            now: Long,
        ) {
            when (attachmentType) {
                BacklogItemTypeValues.NOTE_DOCUMENT -> {
                    val document = noteDocumentRepository.getDocumentById(entityId) ?: return
                    if (document.contextId != fallbackContextId) {
                        noteDocumentRepository.updateDocument(
                            document.copy(contextId = fallbackContextId, updatedAt = now),
                        )
                    }
                }

                BacklogItemTypeValues.MUSIC_NOTE -> {
                    val musicNote = musicNoteRepository.getById(entityId) ?: return
                    if (musicNote.contextId != fallbackContextId) {
                        musicNoteRepository.update(musicNote.copy(contextId = fallbackContextId, updatedAt = now))
                    }
                }

                BacklogItemTypeValues.CHECKLIST -> {
                    val checklist = checklistRepository.getChecklistById(entityId) ?: return
                    if (checklist.contextId != fallbackContextId) {
                        checklistRepository.updateChecklist(
                            checklist.copy(contextId = fallbackContextId, updatedAt = now),
                        )
                    }
                }
            }
        }

        suspend fun addLinkItemToContextFromLink(
            contextId: String,
            link: RelatedLink,
        ): String = attachmentRepository.createLinkAttachment(contextId, link)

        suspend fun addConnectionNoteToContext(
            contextId: String,
            text: String,
        ): String {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return ""

            val title =
                trimmed
                    .lineSequence()
                    .firstOrNull()
                    ?.trim()
                    ?.take(64)
                    ?.ifBlank { "Note" }
                    ?: "Note"

            val documentId =
                noteDocumentRepository.createDocument(
                    name = title,
                    contextId = contextId,
                    content = trimmed,
                )

            return attachmentRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                ?: documentId
        }

        suspend fun linkAttachmentToContext(
            attachmentId: String,
            contextId: String,
        ) {
            attachmentRepository.linkAttachmentToContext(attachmentId, contextId)
        }

        suspend fun findAttachmentIdByEntity(
            attachmentType: String,
            entityId: String,
        ): String? = attachmentRepository.findAttachmentByEntity(attachmentType, entityId)?.id

        suspend fun deleteAttachmentEverywhere(attachmentId: String) {
            val attachment = attachmentRepository.getAttachmentById(attachmentId) ?: return
            when (attachment.attachmentType) {
                BacklogItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.deleteDocument(attachment.entityId)
                BacklogItemTypeValues.MUSIC_NOTE -> musicNoteRepository.delete(attachment.entityId)
                BacklogItemTypeValues.CHECKLIST -> checklistRepository.deleteChecklist(attachment.entityId)
                else -> attachmentRepository.deleteAttachment(attachmentId)
            }
        }

        /**
         * LinkItem identity is independent from both Attachment id and Backlog
         * placement id. Remove every runtime presentation through the domain id.
         */
        suspend fun deleteLinkItemEverywhere(linkItemId: String) {
            attachmentRepository
                .findAttachmentByEntity(
                    attachmentType = BacklogItemTypeValues.LINK_ITEM,
                    entityId = linkItemId,
                )
                ?.let { attachment ->
                    deleteAttachmentEverywhere(attachment.id)
                }

            backlogPlacementCommands.tombstoneContextBackedTarget(
                itemType = BacklogItemTypeValues.LINK_ITEM,
                entityId = linkItemId,
            )
        }

        suspend fun updateAttachmentOrders(
            contextId: String,
            updates: List<Pair<String, Long>>,
        ) {
            attachmentRepository.updateAttachmentOrders(contextId, updates.toMap())
        }

        /**
         * Гарантує, що вкладення (наприклад, перенесена нотатка) прив'язане до цільового контексту.
         */
// У файлі ContextRepository.kt змініть метод на такий:

        suspend fun ensureAttachmentLinkedToContext(
            attachmentType: String,
            entityId: String,
            targetContextId: String,
            ownerContextId: String?,
            createdAt: Long = System.currentTimeMillis(),
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): String {
            return ensureAttachmentLinkedToContext(
                AttachmentLinkRequest(
                    attachmentType = attachmentType,
                    entityId = entityId,
                    targetContextId = targetContextId,
                    ownerContextId = ownerContextId,
                    createdAt = createdAt,
                    roleCode = roleCode,
                    isSystem = isSystem,
                ),
            )
        }

        private suspend fun ensureAttachmentLinkedToContext(
            request: AttachmentLinkRequest,
        ): String {
            attachmentRepository.ensureAttachmentLinkedToContext(
                request.attachmentType,
                request.entityId,
                request.targetContextId,
                request.ownerContextId,
                request.createdAt,
                request.roleCode,
                request.isSystem,
            )
            return request.entityId
        }

        /** Post-cutover startup repair. Retained legacy list_items are not runtime input. */
        suspend fun cleanupDanglingAndLegacyStructuralListItems(): Int =
            canonicalBacklogRepository.tombstoneDanglingAndStructuralEntries()

        fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> = contextLogRepository.getContextLogsStream(contextId)

        suspend fun createContextWithId(
            id: String,
            name: String,
            parentId: String?,
            roleCode: String? = null,
        ) {
            require(!SystemContexts.isSystem(ContextId(id))) {
                "Reserved System Context shells cannot be created: $id"
            }
            val now = System.currentTimeMillis()
            val normalizedRoleCode = roleCode?.trim()?.takeIf { it.isNotBlank() }
            val preset = normalizedRoleCode?.let { structurePresetDao.getByCode(it) }
            val newContext =
                Context(
                    id = id,
                    name = name,
                    parentId = parentId,
                    description = "",
                    createdAt = now,
                    updatedAt = now,
                    version = 1,
                    roleCode = normalizedRoleCode,
                )
            workspaceWriteThrough.mutate(now) {
                contextDao.insert(newContext)
                contextStructureDao.insertStructure(
                    ContextConfiguration(
                        id = UUID.randomUUID().toString(),
                        contextId = id,
                        basePresetCode = normalizedRoleCode,
                        enableInbox = preset?.enableInbox,
                        enableLog = preset?.enableLog,
                        enableAdvanced = null,
                        enableDashboard = preset?.enableDashboard,
                        enableBacklog = preset?.enableBacklog,
                        enableAttachments = preset?.enableAttachments,
                        enableAutoLinkSubprojects = preset?.enableAutoLinkSubprojects ?: true,
                    ),
                )
            }
            tagAssociationHandler.syncContextTags(newContext)

            ensureChildContextDirectionFrontLinkIfEnabled(
                parentContextId = parentId,
                childContextId = id,
                childContextName = name,
            )
        }

        private suspend fun ensureDirectionFrontLinkForParentChangeIfNeeded(
            oldParentId: String?,
            newParentId: String?,
            childId: String,
            childName: String,
        ) {
            val normalizedOld = normalizeParentId(oldParentId)
            val normalizedNew = normalizeParentId(newParentId)
            if (normalizedOld == normalizedNew) return
            ensureChildContextDirectionFrontLinkIfEnabled(
                parentContextId = normalizedNew,
                childContextId = childId,
                childContextName = childName,
            )
        }

        private fun normalizeParentId(parentId: String?): String? =
            parentId?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

        private suspend fun ensureChildContextDirectionFrontLinkIfEnabled(
            parentContextId: String?,
            childContextId: String,
            childContextName: String,
        ) {
            val parentId = normalizeParentId(parentContextId) ?: return
            val parentStructure = contextStructureDao.getStructureByContext(parentId)
            val autoAddToDirectionFront =
                systemContextCanonicalInboxDirectionAccess.directionAutoLinkEnabled(parentId)
                    ?: (parentStructure?.enableAutoLinkSubprojects == true)
            if (!autoAddToDirectionFront) return
            addChildContextToDirectionFront(
                parentContextId = parentId,
                childContextId = childContextId,
                childContextName = childContextName,
            )
        }

        private suspend fun addChildContextToDirectionFront(
            parentContextId: String,
            childContextId: String,
            childContextName: String,
        ) {
            directionRepository.addDirectionLinkedAtFront(
                parentContextId = parentContextId,
                childContextId = childContextId,
                childContextName = childContextName,
            )
        }

        /**
         * Логування підсумків часу для контексту (використовується в MainActivity)
         */
        suspend fun logContextTimeSummaryForDate(
            contextId: String,
            dayToLog: java.util.Calendar,
        ) = contextTimeTrackingRepository.logContextTimeSummaryForDate(contextId, dayToLog)

        /**
         * Глобальний пошук по всьому додатку
         */
        suspend fun searchGlobal(query: String) = searchRepository.searchGlobal(query)

        /**
         * Створення підконтексту за роллю (потрібно для пресетів структури)
         */
        suspend fun ensureSubcontextByRole(
            parentContextId: String,
            roleCode: String,
            title: String,
        ): Context {
            val existing = contextDao.findChildByRole(parentContextId, roleCode)
            if (existing != null) {
                ensureChildContextDirectionFrontLinkIfEnabled(
                    parentContextId = parentContextId,
                    childContextId = existing.id,
                    childContextName = existing.name,
                )
                return existing
            }

            val newId = java.util.UUID.randomUUID().toString()
            createContextWithId(id = newId, name = title, parentId = parentContextId, roleCode = roleCode)
            return contextDao.getContextById(newId) ?: throw IllegalStateException("Failed to create context")
        }

        /**
         * Відв'язування вкладення від конкретного контексту
         */
        suspend fun unlinkAttachmentFromContext(
            contextId: String,
            attachmentId: String,
        ) {
            attachmentRepository.unlinkAttachmentFromContext(attachmentId, contextId)
        }

        /** Removes presentations without deleting externally-owned target content. */
        suspend fun deleteListItemsFromContext(
            contextId: String,
            itemIds: List<String>,
        ) = backlogPresentationLifecycle.remove(contextId, itemIds)

        // У файлі ContextRepository.kt додайте:
        fun getSubprojectsByParentIdFlow(parentId: String): Flow<List<Context>> {
            return contextDao.getSubprojectsByParentIdFlow(parentId)
        }
    }
