package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

enum class ContextTextAction { ADD, REMOVE }

@Singleton
class ContextRepository
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val legacyNoteRepository: LegacyNoteRepository,
        private val activityRepository: ActivityRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val reminderRepository: ReminderRepository,
        private val contextLogRepository: ContextLogRepository,
        private val searchRepository: SearchRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val checklistRepository: ChecklistRepository,
        private val attachmentRepository: AttachmentsRepository,
        private val goalRepository: GoalRepository,
        private val contextTimeTrackingRepository: ContextTimeTrackingRepository,
        private val contextArtifactRepository: ContextArtifactRepository,
        private val listItemRepository: ListItemRepository,
        private val backlogOrderRepository: BacklogOrderRepository,
        private val aiEventRepository: AiEventRepository,
        // ДОДАНО: Потрібен провайдер для уникнення циклічної залежності
        private val contextHandlerProvider: Provider<ContextHandler>,
    ) {
        private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }

        // --- Потоки та Дані з Handler ---
        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> get() = contextHandler.contextMarkerToEmojiMap
        val contextNamesFlow: StateFlow<List<String>> get() = contextHandler.contextNamesFlow
        private val internalHandler: ContextHandler by lazy { contextHandlerProvider.get() }

        fun getContextTag(contextName: String): String? = contextHandler.getContextTag(contextName)

        // --- Базові операції з Контекстами ---
        fun getAllContextsFlow(): Flow<List<Context>> =
            contextDao.getAllContextsFlow().map {
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
                contextDao.getAllContextsFlow(),
                listItemRepository.getAllEntitiesAsFlow(),
                legacyNoteRepository.getAllAsFlow(),
                noteDocumentRepository.getAllDocumentsAsFlow(),
                checklistRepository.getAllChecklistsAsFlow(),
                attachmentRepository.getAttachmentsForContext(contextId),
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                mapToListItemContent(
                    contextId = contextId,
                    items = array[0] as List<BacklogItem>,
                    backlogOrders = array[1] as List<BacklogOrder>,
                    attachments = array[9] as List<AttachmentWithContext>,
                    reminders = array[2] as List<Reminder>,
                    goals = array[3] as List<Goal>,
                    contexts = array[4] as List<Context>,
                    links = array[5] as List<LinkItemEntity>,
                    notes = array[6] as List<LegacyNoteEntity>,
                    noteDocuments = array[7] as List<NoteDocumentEntity>,
                    checklists = array[8] as List<ChecklistEntity>,
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
                (items + attachmentBacklogItems).sortedWith { a, b ->
                    val orderA = orderOverrideMap[a.entityId to a.contextId]?.order ?: a.order
                    val orderB = orderOverrideMap[b.entityId to b.contextId]?.order ?: b.order
                    if (orderA != orderB) orderA.compareTo(orderB) else a.id.compareTo(b.id)
                }

            val remindersMap = reminders.groupBy { it.entityId }
            val goalsMap = goals.associateBy { it.id }
            val contextsMap = contexts.associateBy { it.id }
            val linksMap = links.associateBy { it.id }
            val notesMap = notes.associateBy { it.id }
            val noteDocumentsMap = noteDocuments.associateBy { it.id }
            val checklistsMap = checklists.associateBy { it.id }

            return combinedItems.mapNotNull { item ->
                when (item.itemType) {
                    BacklogItemTypeValues.GOAL ->
                        goalsMap[item.entityId]?.let {
                            BacklogItemContent.GoalItem(
                                it,
                                remindersMap[it.id] ?: emptyList(),
                                item,
                            )
                        }
                    BacklogItemTypeValues.SUBLIST ->
                        contextsMap[item.entityId]?.let {
                            BacklogItemContent.SublistItem(it, remindersMap[it.id] ?: emptyList(), item)
                        }
                    BacklogItemTypeValues.LINK_ITEM -> linksMap[item.entityId]?.let { BacklogItemContent.LinkItem(it, item) }
                    BacklogItemTypeValues.NOTE -> notesMap[item.entityId]?.let { BacklogItemContent.NoteItem(it, item) }
                    BacklogItemTypeValues.NOTE_DOCUMENT ->
                        noteDocumentsMap[item.entityId]?.let {
                            BacklogItemContent.NoteDocumentItem(
                                it,
                                item,
                            )
                        }
                    BacklogItemTypeValues.CHECKLIST -> checklistsMap[item.entityId]?.let { BacklogItemContent.ChecklistItem(it, item) }
                    else -> null
                }
            }
        }

        // --- Операції переміщення та логіки ---
        @Transaction
        suspend fun moveContext(
            contextToMove: Context,
            newParentId: String?,
        ) {
            val oldParentId = contextToMove.parentId
            if (oldParentId == newParentId) return

            // 1. Оновлюємо посилання в списках (ListItem)
            if (oldParentId != null) {
                listItemRepository.deleteLinkByEntityIdAndContextId(contextToMove.id, oldParentId)
            }
            if (newParentId != null) {
                listItemRepository.addContextLinkToContext(contextToMove.id, newParentId)
            }

            // 2. Оновлюємо сам об'єкт контексту
            val updatedContext =
                contextToMove.copy(
                    parentId = newParentId,
                    updatedAt = System.currentTimeMillis(),
                    version = contextToMove.version + 1,
                    syncedAt = null,
                )
            contextDao.update(updatedContext)
        }

        // --- Делегати для інших репозиторіїв (ViewModels їх шукають тут) ---
        suspend fun findContextIdsByTag(tag: String) = contextDao.getContextIdsByTag(tag)

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

        suspend fun ensureChildContextListItemsExist(contextId: String) {
            val children = contextDao.getContextsByParentId(contextId)
            children.forEach { child ->
                if (!listItemRepository.doesLinkExist(child.id, contextId)) {
                    listItemRepository.addContextLinkToContext(child.id, contextId)
                }
            }
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
            val now = System.currentTimeMillis()
            // bumpSync повертає копію об'єкта з новою версією та скинутим syncedAt
            val bumped = context.bumpSync(now)

            contextDao.update(bumped)

            // Оновлюємо відображення в списку нещодавніх проектів
            recentItemsRepository.updateRecentItemDisplayName(context.id, context.name)
        }

        /**
         * Пакетне оновлення списку контекстів.
         * Використовується при масових змінах або сортуванні.
         */
        suspend fun updateContexts(contexts: List<Context>): Int {
            if (contexts.isEmpty()) return 0
            val now = System.currentTimeMillis()
            val bumpedList = contexts.map { it.bumpSync(now) }
            return contextDao.update(bumpedList)
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
            listItemRepository.deleteItemsForContexts(ids)
            val now = System.currentTimeMillis()
            contextsToDelete.forEach { contextDao.insert(it.softDelete(now)) }
        }

        suspend fun addLinkItemToContextFromLink(
            contextId: String,
            link: RelatedLink,
        ): String = attachmentRepository.createLinkAttachment(contextId, link)

        suspend fun deleteAttachmentEverywhere(attachmentId: String) {
            val attachment = attachmentRepository.getAttachmentById(attachmentId) ?: return
            when (attachment.attachmentType) {
                BacklogItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.deleteDocument(attachment.entityId)
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
        ): String { // Змінюємо Unit на String
            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType,
                entityId,
                targetContextId,
                ownerContextId,
                createdAt,
                roleCode,
                isSystem,
            )
            return entityId // Повертаємо ID для UI-логіки (підсвічування/скролу)
        }

        /**
         * Видаляє записи в списках (ListItem), для яких фізично більше не існує сутностей (Goal, Note тощо).
         */
        suspend fun cleanupDanglingListItems() {
            val allListItems = listItemRepository.getAll()
            val itemsToDelete =
                allListItems.filter { item ->
                    val entityId = item.entityId
                    val itemType = item.itemType

                    if (entityId == null || itemType == null) {
                        true // If entityId or itemType is null, the item is corrupt and should be deleted.
                    } else {
                        when (itemType) {
                            BacklogItemTypeValues.GOAL -> goalRepository.getGoalById(entityId) == null
                            BacklogItemTypeValues.SUBLIST -> contextDao.getContextById(entityId) == null
                            BacklogItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.getDocumentById(entityId) == null
                            BacklogItemTypeValues.CHECKLIST -> checklistRepository.getChecklistById(entityId) == null
                            else -> false
                        }
                    }
                }.map { it.id }

            if (itemsToDelete.isNotEmpty()) {
                listItemRepository.deleteListItems(itemsToDelete)
            }
        }

        fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> = contextLogRepository.getContextLogsStream(contextId)

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
                    version = 1,
                    roleCode = roleCode,
                )
            contextDao.insert(newContext)

            // Якщо є батько — створюємо зв'язок у списку відображення
            if (parentId != null) {
                listItemRepository.addContextLinkToContext(id, parentId)
            }
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

            val oldParentId = contextToMove.parentId
            if (oldParentId == newParentId) return

            // 2. Оновлюємо лінки в ListItemRepository
            if (oldParentId != null) {
                listItemRepository.deleteLinkByEntityIdAndContextId(contextToMove.id, oldParentId)
            }
            if (newParentId != null) {
                listItemRepository.addContextLinkToContext(contextToMove.id, newParentId)
            }

            // 3. Оновлюємо запис самого контексту в базі
            val updatedContext =
                contextToMove.copy(
                    parentId = newParentId,
                    updatedAt = System.currentTimeMillis(),
                    version = contextToMove.version + 1,
                    syncedAt = null, // Скидаємо для синхронізації
                )
            contextDao.update(updatedContext)
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
            if (existing != null) return existing

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
