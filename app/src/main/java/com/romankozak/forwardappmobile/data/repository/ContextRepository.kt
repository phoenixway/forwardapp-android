package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.ContextLog
import com.romankozak.forwardappmobile.core.data.models.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.Reminder
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

enum class ContextTextAction {
  ADD,
  REMOVE,
} // Тепер він доступний всюди

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
) {

  fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> =
      contextLogRepository.getContextLogsStream(contextId)

  suspend fun toggleContextManagement(contextId: String, isEnabled: Boolean) {
    val context = getContextById(contextId) ?: return
    if (context.isContextManagementEnabled == isEnabled) return
    updateContext(context.copy(isContextManagementEnabled = isEnabled))
    contextLogRepository.addToggleContextManagementLog(contextId, isEnabled)
  }

  suspend fun updateContextStatus(contextId: String, newStatus: String, statusText: String?) {
    val context = getContextById(contextId) ?: return
    if (context.contextStatus == newStatus && context.contextStatusText == statusText) return
    updateContext(context.copy(contextStatus = newStatus, contextStatusText = statusText))
    contextLogRepository.addUpdateContextStatusLog(contextId, newStatus, statusText)
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
      val items = array[0] as List<BacklogItem>
      val backlogOrders = array[1] as List<BacklogOrder>
      val reminders = array[2] as List<Reminder>
      val goals = array[3] as List<Goal>
      val contexts = array[4] as List<Context>
      val links = array[5] as List<LinkItemEntity>
      val notes = array[6] as List<LegacyNoteEntity>
      val noteDocuments = array[7] as List<NoteDocumentEntity>
      val checklists = array[8] as List<ChecklistEntity>
      val attachments = array[9] as List<AttachmentWithContext>

      mapToListItemContent(
          contextId,
          items,
          backlogOrders,
          attachments,
          reminders,
          goals,
          contexts,
          links,
          notes,
          noteDocuments,
          checklists,
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
              BacklogItemContent.GoalItem(it, remindersMap[it.id] ?: emptyList(), item)
            }
        BacklogItemTypeValues.SUBLIST ->
            contextsMap[item.entityId]?.let {
              BacklogItemContent.SublistItem(it, remindersMap[it.id] ?: emptyList(), item)
            }
        BacklogItemTypeValues.LINK_ITEM ->
            linksMap[item.entityId]?.let { BacklogItemContent.LinkItem(it, item) }
        BacklogItemTypeValues.NOTE ->
            notesMap[item.entityId]?.let { BacklogItemContent.NoteItem(it, item) }
        BacklogItemTypeValues.NOTE_DOCUMENT ->
            noteDocumentsMap[item.entityId]?.let { BacklogItemContent.NoteDocumentItem(it, item) }
        BacklogItemTypeValues.CHECKLIST ->
            checklistsMap[item.entityId]?.let { BacklogItemContent.ChecklistItem(it, item) }
        else -> null
      }
    }
  }

  suspend fun deleteListItemsFromContext(contextId: String, itemIds: List<String>) {
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
          else -> attachmentRepository.unlinkAttachmentFromContext(attachment.id, contextId)
        }
      } else {
        backlogIds += itemId
      }
    }
    if (backlogIds.isNotEmpty()) listItemRepository.deleteListItems(backlogIds)
  }

  suspend fun updateAttachmentOrders(contextId: String, updates: List<Pair<String, Long>>) {
    // Конвертуємо List<Pair> у Map<String, Long> для репозиторію
    attachmentRepository.updateAttachmentOrders(contextId, updates.toMap())
  }

  suspend fun updateContext(context: Context) {
    val now = System.currentTimeMillis()
    // bumpSync має повертати Context, а не Unit
    val bumped = context.bumpSync(now)
    contextDao.update(bumped)
    recentItemsRepository.updateRecentItemDisplayName(context.id, context.name)
  }

  suspend fun updateContexts(contexts: List<Context>): Int {
    if (contexts.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val bumpedList = contexts.map { it.bumpSync(now) }
    return contextDao.update(bumpedList)
  }

  @Transaction
  suspend fun deleteContextsAndSubContexts(contextsToDelete: List<Context>) {
    val nonSystem = contextsToDelete.filter { !SystemContexts.isSystem(ContextId(it.id)) }
    if (nonSystem.isEmpty()) return

    val contextIds = nonSystem.map { it.id }
    listItemRepository.deleteItemsForContexts(contextIds)
    val now = System.currentTimeMillis()
    nonSystem.forEach { contextDao.insert(it.softDelete(now)) }
  }

  suspend fun addLinkItemToContextFromLink(contextId: String, link: RelatedLink): String {
    // createLinkAttachment вже повертає String (ID), не потрібно викликати .id
    return attachmentRepository.createLinkAttachment(contextId, link)
  }

  suspend fun ensureAttachmentLinkedToContext(
      attachmentType: String,
      entityId: String,
      targetContextId: String,
      ownerContextId: String?,
      createdAt: Long = System.currentTimeMillis(),
      roleCode: String? = null,
      isSystem: Boolean = false,
  ): String {
    // ensureAttachmentLinkedToContext тепер повертає ID як String
    attachmentRepository.ensureAttachmentLinkedToContext(
        attachmentType,
        entityId,
        targetContextId,
        ownerContextId,
        createdAt,
        roleCode,
        isSystem,
    )
    // Якщо метод повертає Unit, нам потрібно знайти ID або змінити інтерфейс.
    // Припускаємо, що ми оновили інтерфейс, щоб він повертав ID.
    return entityId
  }

  suspend fun unlinkAttachmentFromContext(contextId: String, attachmentId: String) {
    attachmentRepository.unlinkAttachmentFromContext(attachmentId, contextId)
  }

  fun getAllContextsFlow(): Flow<List<Context>> =
      contextDao
          .getAllContextsFlow() // Використовуємо Flow версію
          .map { contexts -> contexts.map { it.withNormalizedParentId() } }

  suspend fun getContextById(id: String): Context? =
      contextDao.getContextById(id)?.withNormalizedParentId()

  private fun Context.withNormalizedParentId(): Context {
    val normalized =
        parentId?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    return if (normalized != parentId) copy(parentId = normalized) else this
  }

  suspend fun searchGlobal(query: String) = searchRepository.searchGlobal(query)

  suspend fun logContextTimeSummaryForDate(contextId: String, dayToLog: Calendar) =
      contextTimeTrackingRepository.logContextTimeSummaryForDate(contextId, dayToLog)

  suspend fun recalculateAndLogContextTime(contextId: String) =
      contextTimeTrackingRepository.recalculateAndLogContextTime(contextId)

  suspend fun calculateContextTimeMetrics(contextId: String) =
      contextTimeTrackingRepository.calculateContextTimeMetrics(contextId)

  suspend fun cleanupDanglingListItems() {
    val allListItems = listItemRepository.getAll()
    val itemsToDelete =
        allListItems
            .filter { item ->
              when (item.itemType) {
                BacklogItemTypeValues.GOAL -> goalRepository.getGoalById(item.entityId) == null
                BacklogItemTypeValues.SUBLIST -> contextDao.getContextById(item.entityId) == null
                BacklogItemTypeValues.NOTE_DOCUMENT ->
                    noteDocumentRepository.getDocumentById(item.entityId) == null
                BacklogItemTypeValues.CHECKLIST ->
                    checklistRepository.getChecklistById(item.entityId) == null
                else -> false
              }
            }
            .map { it.id }
    if (itemsToDelete.isNotEmpty()) listItemRepository.deleteListItems(itemsToDelete)
  }

  fun getContextArtifactStream(contextId: String) =
      contextArtifactRepository.getContextArtifactStream(contextId)

  suspend fun updateContextArtifact(artifact: ContextArtifact) =
      contextArtifactRepository.updateContextArtifact(artifact)

  suspend fun createContextArtifact(artifact: ContextArtifact) =
      contextArtifactRepository.createContextArtifact(artifact)

  suspend fun updateContextViewMode(contextId: String, viewMode: ContextViewMode) {
    contextDao.updateViewMode(contextId, viewMode.name)
  }

  suspend fun addContextComment(contextId: String, comment: String) {
    contextLogRepository.addContextComment(contextId, comment)
  }

  suspend fun findContextIdsByTag(tag: String): List<String> = contextDao.getContextIdsByTag(tag)

  suspend fun doesLinkToContextExist(entityId: String, contextId: String) =
      listItemRepository.doesLinkExist(entityId, contextId)

  suspend fun deleteLinkByEntityIdAndContextId(entityId: String, contextId: String) =
      listItemRepository.deleteLinkByEntityIdAndContextId(entityId, contextId)

    /**
     * Створює новий контекст із заданим ID.
     * Винесено в окремий метод, бо використовується і в UI, і в системних пресетах.
     */
    suspend fun createContextWithId(
        id: String,
        name: String,
        parentId: String?,
        roleCode: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val newContext = Context(
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

        // Якщо є батьківський контекст, створюємо логічний зв'язок у списку
        if (parentId != null) {
            listItemRepository.addContextLinkToContext(id, parentId)
        }
    }

    /**
     * Перевіряє наявність підконтексту за роллю. Якщо немає — створює його.
     */
    suspend fun ensureSubcontextByRole(
        parentContextId: String,
        roleCode: String,
        title: String,
    ): Context {
        // 1. Шукаємо в базі
        val existing = contextDao.findChildByRole(parentContextId, roleCode)
        if (existing != null) return existing

        // 2. Якщо не знайшли — створюємо новий
        val newId = UUID.randomUUID().toString()
        createContextWithId(
            id = newId,
            name = title,
            parentId = parentContextId,
            roleCode = roleCode
        )

        // 3. Повертаємо щойно створений об'єкт
        return contextDao.getContextById(newId) ?: throw IllegalStateException("Failed to create context")
    }
}
