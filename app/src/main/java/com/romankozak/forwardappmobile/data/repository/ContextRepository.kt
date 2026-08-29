package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.isDirectHierarchyChildContext
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
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
        private val contextArtifactRepository: ContextArtifactRepository,
        private val listItemRepository: ListItemRepository,
        private val contextStructureDao: ContextStructureDao,
        private val structurePresetDao: StructurePresetDao,
        private val directionDao: DirectionDao,
        private val backlogOrderRepository: BacklogOrderRepository,
        private val aiEventRepository: AiEventRepository,
        // ДОДАНО: Потрібен провайдер для уникнення циклічної залежності
        private val contextMarkerHandlerProvider: Provider<ContextMarkerHandler>,
        private val tagAssociationHandler: TagAssociationHandler,
        private val workspaceWriteThrough: ContextWorkspaceWriteThrough,
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
                backlogOrderRepository.observeAll(),
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
                            backlogOrders = array[1] as List<BacklogOrder>,
                            attachments = array[10] as List<AttachmentWithContext>,
                            reminders = array[2] as List<Reminder>,
                            goals = array[3] as List<Goal>,
                            contexts = array[4] as List<Context>,
                            links = array[5] as List<LinkItemEntity>,
                            notes = array[6] as List<LegacyNoteEntity>,
                            noteDocuments = array[7] as List<NoteDocumentEntity>,
                            musicNotes = array[8] as List<MusicNoteEntity>,
                            checklists = array[9] as List<ChecklistEntity>,
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
                BacklogItemTypeValues.JOURNAL_DOCUMENT ->
                    lookupMaps.noteDocumentsMap[entityId]?.let { BacklogItemContent.JournalDocumentItem(it, this) }
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
                    context,
                    lookupMaps.remindersMap[context.id] ?: emptyList(),
                    this,
                )
            }

        // --- Операції переміщення та логіки ---
        @Transaction
        suspend fun moveContext(
            contextToMove: Context,
            newParentId: String?,
        ) {
            val oldParentId = contextToMove.parentId
            if (oldParentId == newParentId) return

            // Hierarchy ownership lives only on Context.parentId/Context.order.
            // Backlog SUBLIST is reserved for explicit user-created context references.
            // Оновлюємо сам об'єкт контексту.
            val updatedContext =
                contextToMove.copy(
                    parentId = newParentId,
                    updatedAt = System.currentTimeMillis(),
                    version = contextToMove.version + 1,
                    syncedAt = null,
                )
            workspaceWriteThrough.mutate(updatedContext.updatedAt ?: System.currentTimeMillis()) {
                contextDao.update(updatedContext)
            }
            ensureDirectionFrontLinkForParentChangeIfNeeded(
                oldParentId = oldParentId,
                newParentId = newParentId,
                childId = contextToMove.id,
                childName = contextToMove.name,
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
        ) = listItemRepository.deleteLinkByEntityIdAndContextId(eId, cId)

        suspend fun addContextLinkToContext(
            cId: String,
            pId: String,
        ) = listItemRepository.addContextLinkToContext(cId, pId)

        suspend fun restoreListItems(items: List<BacklogItem>) = listItemRepository.restoreListItems(items)

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
            val autoAddToDirectionFront = parentStructure?.enableAutoLinkSubprojects == true
            if (!autoAddToDirectionFront) return 0

            val children = contextDao.getActiveContextsByParentId(normalizedParentId)
            if (children.isEmpty()) return 0

            val existingLinkedIds =
                directionDao
                    .getDirectionItemsForContextSync(normalizedParentId)
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

        // --- Artifacts & Time Metrics ---
        fun getContextArtifactStream(id: String) = contextArtifactRepository.getContextArtifactStream(id)

        suspend fun updateContextArtifact(a: ContextArtifact) = contextArtifactRepository.updateContextArtifact(a)

        suspend fun createContextArtifact(a: ContextArtifact) = contextArtifactRepository.createContextArtifact(a)

        suspend fun calculateContextTimeMetrics(id: String) = contextTimeTrackingRepository.calculateContextTimeMetrics(id)

        suspend fun recalculateAndLogContextTime(id: String) = contextTimeTrackingRepository.recalculateAndLogContextTime(id)

// Усередині класу ContextRepository додайте ці пропущені методи:

        suspend fun toggleContextManagement(
            id: String,
            enabled: Boolean,
        ) = contextLogRepository.addToggleContextManagementLog(id, enabled).also {
            val context = getContextById(id) ?: return@also
            updateContext(context.copy(isContextManagementEnabled = enabled))
        }

        suspend fun updateContextStatus(
            id: String,
            status: String,
            text: String?,
        ) {
            val context = getContextById(id) ?: return
            updateContext(context.copy(contextStatus = status, contextStatusText = text))
            contextLogRepository.addUpdateContextStatusLog(id, status, text)
        }

        suspend fun updateContext(context: Context) {
            val previous = contextDao.getContextById(context.id)
            val now = System.currentTimeMillis()
            // bumpSync повертає копію об'єкта з новою версією та скинутим syncedAt
            val bumped = context.bumpSync(now)

            workspaceWriteThrough.mutate(now) { contextDao.update(bumped) }
            tagAssociationHandler.syncContextTags(bumped, previous?.tags)
            ensureDirectionFrontLinkForParentChangeIfNeeded(
                oldParentId = previous?.parentId,
                newParentId = bumped.parentId,
                childId = bumped.id,
                childName = bumped.name,
            )

            // Оновлюємо відображення в списку нещодавніх проектів
            recentItemsRepository.updateRecentItemDisplayName(context.id, context.name)
        }

        /**
         * Пакетне оновлення списку контекстів.
         * Використовується при масових змінах або сортуванні.
         */
        suspend fun updateContexts(contexts: List<Context>): Int {
            if (contexts.isEmpty()) return 0
            val previousById = contextDao.getContextsByIds(contexts.map { it.id }.distinct()).associateBy { it.id }
            val now = System.currentTimeMillis()
            val bumpedList = contexts.map { it.bumpSync(now) }
            val updated = workspaceWriteThrough.mutate(now) { contextDao.update(bumpedList) }
            bumpedList.forEach { bumped ->
                ensureDirectionFrontLinkForParentChangeIfNeeded(
                    oldParentId = previousById[bumped.id]?.parentId,
                    newParentId = bumped.parentId,
                    childId = bumped.id,
                    childName = bumped.name,
                )
            }
            return updated
        }

        suspend fun addContextComment(
            id: String,
            text: String,
        ) = contextLogRepository.addContextComment(id, text)

        suspend fun updateContextViewMode(
            id: String,
            mode: ContextViewMode,
        ) {
            val context = contextDao.getContextById(id) ?: return
            updateContext(context.copy(defaultViewModeName = mode.name))
        }

        suspend fun deleteContextsAndSubContexts(contexts: List<Context>) {
            // Відфільтровуємо системні контексти, щоб їх не можна було видалити
            val contextsToDelete = contexts.filterNot { SystemContexts.isSystem(ContextId(it.id)) }
            if (contextsToDelete.isEmpty()) return

            val ids = contextsToDelete.map { it.id }
            rebindSharedAttachmentEntitiesBeforeContextDeletion(ids.toSet())
            listItemRepository.deleteItemsForContexts(ids)
            val now = System.currentTimeMillis()
            directionDao.markDeletedByLinkedContextIds(ids, now)
            workspaceWriteThrough.mutate(now) {
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
                BacklogItemTypeValues.JOURNAL_DOCUMENT -> {
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
                    attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                )

            return attachmentRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.id
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
                BacklogItemTypeValues.JOURNAL_DOCUMENT -> noteDocumentRepository.deleteDocument(attachment.entityId)
                BacklogItemTypeValues.MUSIC_NOTE -> musicNoteRepository.delete(attachment.entityId)
                BacklogItemTypeValues.CHECKLIST -> checklistRepository.deleteChecklist(attachment.entityId)
                else -> attachmentRepository.deleteAttachment(attachmentId)
            }
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

        /**
         * Прибирає активні backlog rows, які більше не мають валідної user-facing сутності.
         *
         * SUBLIST, що дублює прямий hierarchy relation
         * (targetContext.parentId == item.contextId), є legacy structural projection.
         * Ієрархія належить Context.parentId/Context.order; такі rows мають бути tombstone,
         * а explicit non-child SUBLIST залишається звичайним backlog reference.
         */
        suspend fun cleanupDanglingAndLegacyStructuralListItems(): Int {
            val allListItems = listItemRepository.getAll()
            val itemsToDelete =
                allListItems
                    .filterNot { it.isDeleted }
                    .filter { item ->
                        val entityId = item.entityId
                        val itemType = item.itemType

                        if (entityId == null || itemType == null) {
                            true
                        } else {
                            when (itemType) {
                                BacklogItemTypeValues.GOAL ->
                                    goalRepository.getGoalById(entityId) == null

                                BacklogItemTypeValues.SUBLIST -> {
                                    val targetContext = contextDao.getContextById(entityId)
                                    targetContext == null ||
                                        targetContext.isDeleted ||
                                        isDirectHierarchyChildContext(
                                            item.contextId,
                                            targetContext.parentId,
                                        )
                                }

                                BacklogItemTypeValues.NOTE_DOCUMENT ->
                                    noteDocumentRepository.getDocumentById(entityId) == null

                                BacklogItemTypeValues.JOURNAL_DOCUMENT ->
                                    noteDocumentRepository.getDocumentById(entityId) == null

                                BacklogItemTypeValues.MUSIC_NOTE ->
                                    musicNoteRepository.getById(entityId) == null

                                BacklogItemTypeValues.CHECKLIST ->
                                    checklistRepository.getChecklistById(entityId) == null

                                else -> false
                            }
                        }
                    }.map { it.id }

            if (itemsToDelete.isNotEmpty()) {
                listItemRepository.deleteListItems(itemsToDelete)
            }
            return itemsToDelete.size
        }

        fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> = contextLogRepository.getContextLogsStream(contextId)

        suspend fun createContextWithId(
            id: String,
            name: String,
            parentId: String?,
            roleCode: String? = null,
        ) {
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
                        enableArtifact = preset?.enableArtifact,
                        enableAdvanced = preset?.enableAdvanced,
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
            val autoAddToDirectionFront = parentStructure?.enableAutoLinkSubprojects == true
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
            val existing = directionDao.getDirectionItemsForContextSync(parentContextId)
            if (existing.any { it.linkedContextId == childContextId && !it.isDeleted }) return

            val now = System.currentTimeMillis()
            if (existing.isNotEmpty()) {
                directionDao.updateAll(
                    existing.map { item ->
                        item.copy(
                            itemOrder = item.itemOrder + 1,
                            updatedAt = now,
                            version = item.version + 1,
                            syncedAt = null,
                        )
                    },
                )
            }

            directionDao.insert(
                DirectionItemEntity(
                    contextId = parentContextId,
                    text = childContextName,
                    linkedContextId = childContextId,
                    itemOrder = 0,
                    updatedAt = now,
                    version = 1,
                ),
            )
        }

        /**
         * Переміщує проєкт в іншу папку.
         * allowSystemMoves дозволяє або забороняє переміщення системних папок (як-от Inbox).
         */
        @androidx.room.Transaction
        suspend fun moveContext(
            contextToMove: Context,
            newParentId: String?,
            allowSystemMoves: Boolean = false, // Додано цей параметр
        ) {
            // 1. Перевірка на системність (якщо не дозволено — ігноруємо)
            val isSystem =
                com.romankozak.forwardappmobile.core.context.SystemContexts.isSystem(
                    com.romankozak.forwardappmobile.core.context.ContextId(contextToMove.id),
                )
            if (isSystem && !allowSystemMoves) return
            if (SystemContexts.isPinnedRoot(ContextId(contextToMove.id)) && newParentId != null) return

            val oldParentId = contextToMove.parentId
            if (oldParentId == newParentId) return

            // Hierarchy ownership lives only on Context.parentId/Context.order.
            // Backlog SUBLIST is reserved for explicit user-created context references.
            // Оновлюємо запис самого контексту в базі.
            val updatedContext =
                contextToMove.copy(
                    parentId = newParentId,
                    updatedAt = System.currentTimeMillis(),
                    version = contextToMove.version + 1,
                    syncedAt = null, // Скидаємо для синхронізації
                )
            workspaceWriteThrough.mutate(updatedContext.updatedAt ?: System.currentTimeMillis()) {
                contextDao.update(updatedContext)
            }
            ensureDirectionFrontLinkForParentChangeIfNeeded(
                oldParentId = oldParentId,
                newParentId = newParentId,
                childId = contextToMove.id,
                childName = contextToMove.name,
            )
        }

        // Додайте в ContextRepository.kt

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

        /**
         * Масове видалення елементів із беклогу контексту.
         * Якщо елемент є вкладенням (нотатка/чекліст), видаляється сама сутність або лінк.
         */
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
                        BacklogItemTypeValues.JOURNAL_DOCUMENT ->
                            noteDocumentRepository.deleteDocument(attachment.entityId)
                        BacklogItemTypeValues.MUSIC_NOTE ->
                            musicNoteRepository.delete(attachment.entityId)
                        BacklogItemTypeValues.CHECKLIST ->
                            checklistRepository.deleteChecklist(attachment.entityId)
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

        // У файлі ContextRepository.kt додайте:
        fun getSubprojectsByParentIdFlow(parentId: String): Flow<List<Context>> {
            return contextDao.getSubprojectsByParentIdFlow(parentId)
        }
    }
