package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.domain.ai.events.ProjectActivatedEvent
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextArtifact
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextConfiguration
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextTimeMetrics
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.data.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItemContent
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.ContextId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal enum class ContextTextAction { ADD, REMOVE }

@Singleton
class ContextRepository
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val legacyNoteRepository: LegacyNoteRepository,
        private val contextHandlerProvider: Provider<ContextHandler>,
        private val activityRepository: ActivityRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val reminderRepository: ReminderRepository,
        private val contextLogRepository: ContextLogRepository,
        private val searchRepository: SearchRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val checklistRepository: ChecklistRepository,
        private val attachmentRepository: AttachmentRepository,
        private val goalRepository: GoalRepository,
        private val inboxRepository: InboxRepository,
        private val contextTimeTrackingRepository: ContextTimeTrackingRepository,
        private val contextArtifactRepository: ContextArtifactRepository,
        private val listItemRepository: ListItemRepository,
        private val backlogOrderRepository: BacklogOrderRepository,
        private val aiEventRepository: AiEventRepository,
    ) {
        private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }
        private val TAG = "NOTE_DOCUMENT_DEBUG"

        fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> = contextLogRepository.getContextLogsStream(contextId)

        suspend fun toggleContextManagement(
            contextId: String,
            isEnabled: Boolean,
        ) {
            val context = getContextById(contextId) ?: return
            if (context.isContextManagementEnabled == isEnabled) return

            updateContext(context.copy(isContextManagementEnabled = isEnabled))
            contextLogRepository.addToggleContextManagementLog(contextId, isEnabled)
        }

        suspend fun updateContextStatus(
            contextId: String,
            newStatus: String,
            statusText: String?,
        ) {
            val context = getContextById(contextId) ?: return
            if (context.contextStatus == newStatus && context.contextStatusText == statusText) return

            updateContext(
                context.copy(
                    contextStatus = newStatus,
                    contextStatusText = statusText,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            contextLogRepository.addUpdateContextStatusLog(contextId, newStatus, statusText)
        }

        suspend fun addContextComment(
            contextId: String,
            comment: String,
        ) {
            contextLogRepository.addContextComment(contextId, comment)
        }

        suspend fun updateContextViewMode(
            contextId: String,
            viewMode: ContextViewMode,
        ) {
            contextDao.updateViewMode(contextId, viewMode.name)
        }

        fun getContextContentStream(contextId: String): Flow<List<BacklogItemContent>> {
            return combine(
                listItemRepository.getItemsForContextStream(contextId),
                backlogOrderRepository.observeAll(),
                reminderRepository.getAllReminders(),
                goalRepository.getAllGoalsFlow(),
                contextDao.getAllContexts(),
                listItemRepository.getAllEntitiesAsFlow(),
                legacyNoteRepository.getAllAsFlow(),
                noteDocumentRepository.getAllDocumentsAsFlow(),
                checklistRepository.getAllChecklistsAsFlow(),
                attachmentRepository.getAttachmentsForContext(contextId),
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                val items = array[0] as List<BacklogItem>

                @Suppress("UNCHECKED_CAST")
                val backlogOrders = array[1] as List<BacklogOrder>

                @Suppress("UNCHECKED_CAST")
                val reminders = array[2] as List<Reminder>

                @Suppress("UNCHECKED_CAST")
                val goals = array[3] as List<Goal>

                @Suppress("UNCHECKED_CAST")
                val contexts = array[4] as List<Context>

                @Suppress("UNCHECKED_CAST")
                val links = array[5] as List<LinkItemEntity>

                @Suppress("UNCHECKED_CAST")
                val notes = array[6] as List<LegacyNoteEntity>

                @Suppress("UNCHECKED_CAST")
                val noteDocuments = array[7] as List<NoteDocumentEntity>

                @Suppress("UNCHECKED_CAST")
                val checklists = array[8] as List<ChecklistEntity>

                @Suppress("UNCHECKED_CAST")
                val attachments = array[9] as List<AttachmentWithContext>
                mapToListItemContent(
                    contextId = contextId,
                    items = items,
                    backlogOrders = backlogOrders,
                    attachments = attachments,
                    reminders = reminders,
                    goals = goals,
                    contexts = contexts,
                    links = links,
                    notes = notes,
                    noteDocuments = noteDocuments,
                    checklists = checklists,
                )
            }
        }

        private fun mapToListItemContent(
            contextId: String,
            items: List<BacklogItem>,
            backlogOrders: List<BacklogOrder>,
            attachments: List<AttachmentWithContext>,
            reminders: List<Reminder>,
            goals: List<Goal>,
            contexts: List<Context>,
            links: List<LinkItemEntity>,
            notes: List<LegacyNoteEntity>,
            noteDocuments: List<NoteDocumentEntity>,
            checklists: List<ChecklistEntity>,
        ): List<BacklogItemContent> {
            val attachmentBacklogItems =
                attachments.map { attachment ->
                    val order = attachment.attachmentOrder ?: -attachment.attachment.createdAt
                    BacklogItem(
                        id = attachment.attachment.id,
                        contextId = contextId,
                        itemType = attachment.attachment.attachmentType,
                        entityId = attachment.attachment.entityId,
                        order = order,
                    )
                }
            val orderOverrideMap = backlogOrders.associateBy { it.itemId to it.listId }

            val combinedItems =
                (items + attachmentBacklogItems).sortedWith(
                    Comparator<BacklogItem> { a, b ->
                        val keyA = orderOverrideMap[a.entityId to a.contextId]
                        val keyB = orderOverrideMap[b.entityId to b.contextId]
                        val orderA = keyA?.order ?: a.order
                        val orderB = keyB?.order ?: b.order
                        if (orderA != orderB) return@Comparator orderA.compareTo(orderB)
                        return@Comparator a.id.compareTo(b.id)
                    },
                )
            val remindersMap = reminders.groupBy { it.entityId }
            val goalsMap = goals.associateBy { it.id }
            val contextsMap = contexts.associateBy { it.id }
            val linksMap = links.associateBy { it.id }
            val notesMap = notes.associateBy { it.id }
            val noteDocumentsMap = noteDocuments.associateBy { it.id }
            val checklistsMap = checklists.associateBy { it.id }

            val backlogItems =
                combinedItems.mapNotNull { item ->
                    when (item.itemType) {
                        BacklogItemTypeValues.GOAL ->
                            goalsMap[item.entityId]?.let { goal ->
                                val itemReminders = remindersMap[goal.id] ?: emptyList()
                                BacklogItemContent.GoalItem(goal, itemReminders, item)
                            }
                        BacklogItemTypeValues.SUBLIST ->
                            contextsMap[item.entityId]?.let { context ->
                                val itemReminders = remindersMap[context.id] ?: emptyList()
                                BacklogItemContent.SublistItem(context, itemReminders, item)
                            }
                        BacklogItemTypeValues.LINK_ITEM ->
                            linksMap[item.entityId]?.let { link ->
                                BacklogItemContent.LinkItem(link, item)
                            }
                        BacklogItemTypeValues.NOTE ->
                            notesMap[item.entityId]?.let { note ->
                                BacklogItemContent.NoteItem(note, item)
                            }
                        BacklogItemTypeValues.NOTE_DOCUMENT ->
                            noteDocumentsMap[item.entityId]?.let { document ->
                                BacklogItemContent.NoteDocumentItem(document, item)
                            }
                        BacklogItemTypeValues.CHECKLIST ->
                            checklistsMap[item.entityId]?.let { checklist ->
                                BacklogItemContent.ChecklistItem(checklist, item)
                            }
                        else -> null
                    }
                }

            return backlogItems
        }

        @Transaction
        suspend fun addContextLinkToContext(
            targetContextId: String,
            currentContextId: String,
        ): String = listItemRepository.addContextLinkToContext(targetContextId, currentContextId)

        suspend fun moveListItemsToContext(
            itemIds: List<String>,
            targetContextId: String,
        ) = listItemRepository.moveListItemsToContext(itemIds, targetContextId)

        suspend fun deleteListItemsFromContext(
            contextId: String,
            itemIds: List<String>,
        ) {
            if (itemIds.isEmpty()) return

            val backlogIds = mutableListOf<String>()

            for (itemId in itemIds) {
                val attachment = attachmentRepository.getAttachmentById(itemId)
                if (attachment != null) {
                    when (attachment.attachmentType) {
                        BacklogItemTypeValues.NOTE_DOCUMENT ->
                            noteDocumentRepository.deleteDocument(attachment.entityId)
                        BacklogItemTypeValues.CHECKLIST ->
                            checklistRepository.deleteChecklist(attachment.entityId)
                        BacklogItemTypeValues.LINK_ITEM ->
                            attachmentRepository.unlinkAttachmentFromContext(attachment.id, contextId)
                        else ->
                            attachmentRepository.unlinkAttachmentFromContext(attachment.id, contextId)
                    }
                } else {
                    backlogIds += itemId
                }
            }

            if (backlogIds.isNotEmpty()) {
                listItemRepository.deleteListItems(backlogIds)
            }
        }

        suspend fun restoreListItems(items: List<BacklogItem>) = listItemRepository.restoreListItems(items)

        suspend fun updateListItemsOrder(items: List<BacklogItem>) = listItemRepository.updateListItemsOrder(items)

        suspend fun updateAttachmentOrders(
            contextId: String,
            updates: List<Pair<String, Long>>,
        ) {
            attachmentRepository.updateAttachmentOrders(contextId, updates)
        }

        suspend fun doesLinkToContextExist(
            entityId: String,
            contextId: String,
        ): Boolean = listItemRepository.doesLinkExist(entityId, contextId)

        suspend fun deleteLinkByEntityIdAndContextId(
            entityId: String,
            contextId: String,
        ) = listItemRepository.deleteLinkByEntityIdAndContextId(entityId, contextId)

        fun getAllContextsFlow(): Flow<List<Context>> =
            contextDao
                .getAllContexts()
                .map { contexts -> contexts.map { it.withNormalizedParentId() } }

        suspend fun getContextById(id: String): Context? = contextDao.getContextById(id)?.withNormalizedParentId()

        fun getContextByIdFlow(id: String): Flow<Context?> =
            contextDao.getContextByIdStream(id).map { context -> context?.withNormalizedParentId() }

        private fun Context.withNormalizedParentId(): Context {
            val normalizedParentId =
                parentId
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

            return if (normalizedParentId != parentId) {
                copy(parentId = normalizedParentId)
            } else {
                this
            }
        }

        suspend fun updateContext(context: Context) {
            val now = System.currentTimeMillis()
            val bumped =
                context.bumpSync(now)
            contextDao.update(bumped)
            recentItemsRepository.updateRecentItemDisplayName(context.id, context.name)
        }

        suspend fun updateContexts(contexts: List<Context>): Int =
            if (contexts.isNotEmpty()) {
                contextDao.update(contexts.map { it.bumpSync() })
            } else {
                0
            }

        @Transaction
        suspend fun deleteContextsAndSubContexts(contextsToDelete: List<Context>) {
            if (contextsToDelete.isEmpty()) return

            val nonSystemContextsToDelete = contextsToDelete.filter { context ->
                !SystemContexts.isSystem(ContextId(context.id))
            }

            if (nonSystemContextsToDelete.isEmpty()) {
                val systemContextIds = contextsToDelete.filter { SystemContexts.isSystem(ContextId(it.id)) }.map { it.id }
                throw IllegalArgumentException("Cannot delete system contexts: $systemContextIds. No non-system contexts provided for deletion.")
            }

            val contextIds = nonSystemContextsToDelete.map { it.id }
            listItemRepository.deleteItemsForContexts(contextIds)
            val now = System.currentTimeMillis()
            nonSystemContextsToDelete.forEach { context ->
                contextDao.insert(
                    context.softDelete(now),
                )
            }
        }

        suspend fun createContextWithId(
            id: String,
            name: String,
            parentId: String?,
            roleCode: String? = null,
        ) {
            val now = System.currentTimeMillis()
            val newContext =
                Context(
                    id = id,
                    name = name,
                    parentId = parentId,
                    description = "",
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                    roleCode = roleCode,
                )
            contextDao.insert(newContext)
            if (parentId != null) {
                listItemRepository.addContextLinkToContext(id, parentId)
            }
            aiEventRepository.emit(
                ProjectActivatedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    contextId = id,
                ),
            )
        }

        @Transaction
        suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
            return searchRepository.searchGlobal(query)
        }

        @Transaction
        suspend fun moveContext(
            contextToMove: Context,
            newParentId: String?,
            allowSystemContextMoves: Boolean = false,
        ) {
            val contextFromDb = contextDao.getContextById(contextToMove.id) ?: return
            val oldParentId = contextFromDb.parentId

            if (oldParentId != newParentId) {
                if (oldParentId != null) {
                    listItemRepository.deleteLinkByEntityIdAndContextId(contextToMove.id, oldParentId)
                }
                if (newParentId != null) {
                    listItemRepository.addContextLinkToContext(contextToMove.id, newParentId)
                }

                val oldSiblings =
                    (
                        if (oldParentId != null) {
                            contextDao.getContextsByParentId(oldParentId)
                        } else {
                            contextDao.getTopLevelContexts()
                        }
                    ).filter { it.id != contextToMove.id }

                if (oldSiblings.isNotEmpty()) {
                    contextDao.update(oldSiblings.mapIndexed { index, context -> context.copy(order = index.toLong()) })
                }
            }

            val newSiblings =
                (
                    if (newParentId != null) {
                        contextDao.getContextsByParentId(newParentId)
                    } else {
                        contextDao.getTopLevelContexts()
                    }
                ).filter { it.id != contextToMove.id }

            val finalContextToMove =
                contextToMove.copy(
                    parentId = newParentId,
                    order = newSiblings.size.toLong(),
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = contextToMove.version + 1,
                )
            contextDao.update(finalContextToMove)
        }

        @Transaction
        suspend fun addLinkItemToContextFromLink(
            contextId: String,
            link: RelatedLink,
        ): String {
            val attachment = attachmentRepository.createLinkAttachment(contextId, link)
            return attachment.id
        }

        suspend fun linkAttachmentToContext(
            attachmentId: String,
            targetContextId: String,
        ) {
            attachmentRepository.linkAttachmentToContext(attachmentId, targetContextId)
        }

        suspend fun ensureAttachmentLinkedToContext(
            attachmentType: String,
            entityId: String,
            targetContextId: String,
            ownerContextId: String?,
            createdAt: Long = System.currentTimeMillis(),
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): String =
            attachmentRepository
                .ensureAttachmentLinkedToContext(
                    attachmentType = attachmentType,
                    entityId = entityId,
                    contextId = targetContextId,
                    ownerContextId = ownerContextId,
                    createdAt = createdAt,
                    roleCode = roleCode,
                    isSystem = isSystem,
                ).id

        suspend fun unlinkAttachmentFromContext(
            contextId: String,
            attachmentId: String,
        ): Boolean = attachmentRepository.unlinkAttachmentFromContext(attachmentId, contextId)

        suspend fun deleteAttachmentEverywhere(attachmentId: String) {
            val attachment = attachmentRepository.getAttachmentById(attachmentId) ?: return
            when (attachment.attachmentType) {
                BacklogItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.deleteDocument(attachment.entityId)
                BacklogItemTypeValues.CHECKLIST -> checklistRepository.deleteChecklist(attachment.entityId)
                else -> this.attachmentRepository.deleteAttachment(attachmentId)
            }
        }

        suspend fun findContextIdsByTag(tag: String): List<String> = contextDao.getContextIdsByTag(tag)

        suspend fun getAllContexts(): List<Context> = contextDao.getAll()

        suspend fun deleteItemByEntityId(entityId: String) = listItemRepository.deleteItemByEntityId(entityId)

        suspend fun logContextTimeSummaryForDate(
            contextId: String,
            dayToLog: Calendar,
        ) = contextTimeTrackingRepository.logContextTimeSummaryForDate(contextId, dayToLog)

        suspend fun recalculateAndLogContextTime(contextId: String) = contextTimeTrackingRepository.recalculateAndLogContextTime(contextId)

        suspend fun calculateContextTimeMetrics(contextId: String): ContextTimeMetrics =
            contextTimeTrackingRepository.calculateContextTimeMetrics(
                contextId,
            )

        suspend fun cleanupDanglingListItems() {
            val allListItems = listItemRepository.getAll()
            val itemsToDelete = mutableListOf<String>()

            allListItems.forEach { item ->
                val entityExists =
                    when (item.itemType) {
                        BacklogItemTypeValues.GOAL -> goalRepository.getGoalById(item.entityId) != null
                        BacklogItemTypeValues.SUBLIST -> contextDao.getContextById(item.entityId) != null
                        BacklogItemTypeValues.LINK_ITEM -> listItemRepository.getLinkItemById(item.entityId) != null
                        BacklogItemTypeValues.NOTE -> legacyNoteRepository.getNoteById(item.entityId) != null
                        BacklogItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.getDocumentById(item.entityId) != null
                        BacklogItemTypeValues.CHECKLIST -> checklistRepository.getChecklistById(item.entityId) != null
                        else -> true // Assume unknown types are valid to avoid deleting them
                    }
                if (!entityExists) {
                    itemsToDelete.add(item.id)
                }
            }

            if (itemsToDelete.isNotEmpty()) {
                listItemRepository.deleteListItems(itemsToDelete)
                Log.d("DB_CLEANUP", "Deleted ${itemsToDelete.size} dangling ListItem records.")
            }
        }

        fun getContextArtifactStream(contextId: String): Flow<ContextArtifact?> =
            contextArtifactRepository.getContextArtifactStream(
                contextId,
            )

        suspend fun updateContextArtifact(artifact: ContextArtifact) = contextArtifactRepository.updateContextArtifact(artifact)

        suspend fun createContextArtifact(artifact: ContextArtifact) = contextArtifactRepository.createContextArtifact(artifact)

        suspend fun ensureSubcontextByRole(
            parentContextId: String,
            roleCode: String,
            title: String,
        ): Context {
            val existing = contextDao.findChildByRole(parentContextId, roleCode)
            if (existing != null) return existing
            val newId = UUID.randomUUID().toString()
            createContextWithId(
                id = newId,
                name = title,
                parentId = parentContextId,
                roleCode = roleCode,
            )
            return contextDao.getContextById(newId) ?: Context(
                id = newId,
                name = title,
                parentId = parentContextId,
                description = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncedAt = null,
                version = 1,
                roleCode = roleCode,
            )
        }

        suspend fun ensureChildContextListItemsExist(contextId: String) {
            val children = contextDao.getContextsByParentId(contextId)
            val backlogItems = listItemRepository.getItemsForContextStream(contextId).first()
            val backlogSubprojectIds = backlogItems.filter { it.itemType == BacklogItemTypeValues.SUBLIST }.map { it.entityId }.toSet()

            children.forEach { child ->
                if (child.id !in backlogSubprojectIds) {
                    listItemRepository.addContextLinkToContext(child.id, contextId)
                }
            }
        }
    }
