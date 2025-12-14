package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.ProjectLogEntryTypeValues
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import com.romankozak.forwardappmobile.data.repository.SearchRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentWithProject
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.domain.ai.events.ProjectActivatedEvent
import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import javax.inject.Singleton

internal enum class ContextTextAction { ADD, REMOVE }


@Singleton
class ProjectRepository
@Inject
constructor(
    private val projectDao: ProjectDao,
    private val legacyNoteRepository: LegacyNoteRepository,
    private val contextHandlerProvider: Provider<ContextHandler>,
    private val activityRepository: ActivityRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val reminderRepository: ReminderRepository,
    private val projectLogRepository: ProjectLogRepository,
    private val searchRepository: SearchRepository,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val checklistRepository: ChecklistRepository,
    private val attachmentRepository: AttachmentRepository,
    private val goalRepository: GoalRepository,
    private val inboxRepository: InboxRepository,
    private val projectTimeTrackingRepository: ProjectTimeTrackingRepository,
    private val projectArtifactRepository: ProjectArtifactRepository,
    private val listItemRepository: ListItemRepository,
    private val backlogOrderRepository: BacklogOrderRepository,
    private val aiEventRepository: AiEventRepository,
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }
    private val TAG = "NOTE_DOCUMENT_DEBUG"

    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        projectLogRepository.getProjectLogsStream(projectId)

    suspend fun toggleProjectManagement(
        projectId: String,
        isEnabled: Boolean,
    ) {
        val project = getProjectById(projectId) ?: return
        if (project.isProjectManagementEnabled == isEnabled) return

        updateProject(project.copy(isProjectManagementEnabled = isEnabled))
        projectLogRepository.addToggleProjectManagementLog(projectId, isEnabled)
    }

    suspend fun updateProjectStatus(
        projectId: String,
        newStatus: String,
        statusText: String?,
    ) {
        val project = getProjectById(projectId) ?: return
        if (project.projectStatus == newStatus && project.projectStatusText == statusText) return

        updateProject(
            project.copy(
                projectStatus = newStatus,
                projectStatusText = statusText,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        projectLogRepository.addUpdateProjectStatusLog(projectId, newStatus, statusText)
    }

    suspend fun addProjectComment(
        projectId: String,
        comment: String,
    ) {
        projectLogRepository.addProjectComment(projectId, comment)
    }

    suspend fun updateProjectViewMode(
        projectId: String,
        viewMode: ProjectViewMode,
    ) {
        projectDao.updateViewMode(projectId, viewMode.name)
    }

    fun getProjectContentStream(projectId: String): Flow<List<ListItemContent>> {
        return combine(
            listItemRepository.getItemsForProjectStream(projectId),
            backlogOrderRepository.observeAll(),
            reminderRepository.getAllReminders(),
            goalRepository.getAllGoalsFlow(),
            projectDao.getAllProjects(),
            listItemRepository.getAllEntitiesAsFlow(),
            legacyNoteRepository.getAllAsFlow(),
            noteDocumentRepository.getAllDocumentsAsFlow(),
            checklistRepository.getAllChecklistsAsFlow(),
            attachmentRepository.getAttachmentsForProject(projectId),
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            val items = array[0] as List<ListItem>
            @Suppress("UNCHECKED_CAST")
            val backlogOrders = array[1] as List<com.romankozak.forwardappmobile.data.database.models.BacklogOrder>
            @Suppress("UNCHECKED_CAST")
            val reminders = array[2] as List<Reminder>
            @Suppress("UNCHECKED_CAST")
            val goals = array[3] as List<Goal>
            @Suppress("UNCHECKED_CAST")
            val projects = array[4] as List<Project>
            @Suppress("UNCHECKED_CAST")
            val links = array[5] as List<LinkItemEntity>
            @Suppress("UNCHECKED_CAST")
            val notes = array[6] as List<LegacyNoteEntity>
            @Suppress("UNCHECKED_CAST")
            val noteDocuments = array[7] as List<NoteDocumentEntity>
            @Suppress("UNCHECKED_CAST")
            val checklists = array[8] as List<ChecklistEntity>
            @Suppress("UNCHECKED_CAST")
            val attachments = array[9] as List<AttachmentWithProject>
            mapToListItemContent(
                projectId = projectId,
                items = items,
                backlogOrders = backlogOrders,
                attachments = attachments,
                reminders = reminders,
                goals = goals,
                projects = projects,
                links = links,
                notes = notes,
                noteDocuments = noteDocuments,
                checklists = checklists,
            )
        }
    }

    private fun mapToListItemContent(
        projectId: String,
        items: List<ListItem>,
        backlogOrders: List<BacklogOrder>,
        attachments: List<AttachmentWithProject>,
        reminders: List<Reminder>,
        goals: List<Goal>,
        projects: List<Project>,
        links: List<LinkItemEntity>,
        notes: List<LegacyNoteEntity>,
        noteDocuments: List<NoteDocumentEntity>,
        checklists: List<ChecklistEntity>,
    ): List<ListItemContent> {
        val attachmentListItems =
            attachments.map { attachment ->
                val order = attachment.attachmentOrder ?: -attachment.attachment.createdAt
                ListItem(
                    id = attachment.attachment.id,
                    projectId = projectId,
                    itemType = attachment.attachment.attachmentType,
                    entityId = attachment.attachment.entityId,
                    order = order,
                )
            }
        val orderOverrideMap = backlogOrders.associateBy { it.itemId to it.listId }

        val combinedItems = (items + attachmentListItems).sortedWith(
            Comparator<ListItem> { a, b ->
                val keyA = orderOverrideMap[a.entityId to a.projectId]
                val keyB = orderOverrideMap[b.entityId to b.projectId]
                val orderA = keyA?.order ?: a.order
                val orderB = keyB?.order ?: b.order
                if (orderA != orderB) return@Comparator orderA.compareTo(orderB)
                return@Comparator a.id.compareTo(b.id)
            }
        )
        val remindersMap = reminders.groupBy { it.entityId }
        val goalsMap = goals.associateBy { it.id }
        val projectsMap = projects.associateBy { it.id }
        val linksMap = links.associateBy { it.id }
        val notesMap = notes.associateBy { it.id }
        val noteDocumentsMap = noteDocuments.associateBy { it.id }
        val checklistsMap = checklists.associateBy { it.id }

        val backlogItems = combinedItems.mapNotNull { item ->
            when (item.itemType) {
                ListItemTypeValues.GOAL ->
                    goalsMap[item.entityId]?.let { goal ->
                        val itemReminders = remindersMap[goal.id] ?: emptyList()
                        ListItemContent.GoalItem(goal, itemReminders, item)
                    }
                ListItemTypeValues.SUBLIST ->
                    projectsMap[item.entityId]?.let { project ->
                        val itemReminders = remindersMap[project.id] ?: emptyList()
                        ListItemContent.SublistItem(project, itemReminders, item)
                    }
                ListItemTypeValues.LINK_ITEM ->
                    linksMap[item.entityId]?.let { link ->
                        ListItemContent.LinkItem(link, item)
                    }
                ListItemTypeValues.NOTE ->
                    notesMap[item.entityId]?.let { note ->
                        ListItemContent.NoteItem(note, item)
                    }
                ListItemTypeValues.NOTE_DOCUMENT ->
                    noteDocumentsMap[item.entityId]?.let { document ->
                        ListItemContent.NoteDocumentItem(document, item)
                    }
                ListItemTypeValues.CHECKLIST ->
                    checklistsMap[item.entityId]?.let { checklist ->
                        ListItemContent.ChecklistItem(checklist, item)
                    }
                else -> null
            }
        }

        return backlogItems
    }

    @Transaction
    suspend fun addProjectLinkToProject(
        targetProjectId: String,
        currentProjectId: String,
    ): String = listItemRepository.addProjectLinkToProject(targetProjectId, currentProjectId)

    suspend fun moveListItems(
        itemIds: List<String>,
        targetProjectId: String,
    ) = listItemRepository.moveListItems(itemIds, targetProjectId)

    suspend fun deleteListItems(
        projectId: String,
        itemIds: List<String>,
    ) {
        if (itemIds.isEmpty()) return

        val backlogIds = mutableListOf<String>()

        for (itemId in itemIds) {
            val attachment = attachmentRepository.getAttachmentById(itemId)
            if (attachment != null) {
                when (attachment.attachmentType) {
                    ListItemTypeValues.NOTE_DOCUMENT ->
                        noteDocumentRepository.deleteDocument(attachment.entityId)
                    ListItemTypeValues.CHECKLIST ->
                        checklistRepository.deleteChecklist(attachment.entityId)
                    ListItemTypeValues.LINK_ITEM ->
                        attachmentRepository.unlinkAttachmentFromProject(itemId, projectId)
                    else ->
                        attachmentRepository.unlinkAttachmentFromProject(itemId, projectId)
                }
            } else {
                backlogIds += itemId
            }
        }

        if (backlogIds.isNotEmpty()) {
            listItemRepository.deleteListItems(backlogIds)
        }
    }

    suspend fun restoreListItems(items: List<ListItem>) = listItemRepository.restoreListItems(items)

    suspend fun updateListItemsOrder(items: List<ListItem>) = listItemRepository.updateListItemsOrder(items)

    suspend fun updateAttachmentOrders(
        projectId: String,
        updates: List<Pair<String, Long>>,
    ) = attachmentRepository.updateAttachmentOrders(projectId, updates)



    suspend fun doesLinkExist(
        entityId: String,
        projectId: String,
    ): Boolean = listItemRepository.doesLinkExist(entityId, projectId)

    suspend fun deleteLinkByEntityIdAndProjectId(
        entityId: String,
        projectId: String,
    ) = listItemRepository.deleteLinkByEntityIdAndProjectId(entityId, projectId)

    fun getAllProjectsFlow(): Flow<List<Project>> =
        projectDao
            .getAllProjects()
            .map { projects -> projects.map { it.withNormalizedParentId() } }

    suspend fun getProjectById(id: String): Project? =
        projectDao.getProjectById(id)?.withNormalizedParentId()

    fun getProjectByIdFlow(id: String): Flow<Project?> =
        projectDao.getProjectByIdStream(id).map { project -> project?.withNormalizedParentId() }

    private fun Project.withNormalizedParentId(): Project {
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

    suspend fun updateProject(project: Project) {
        val now = System.currentTimeMillis()
        val bumped =
            project.bumpSync(now)
        projectDao.update(bumped)
        recentItemsRepository.updateRecentItemDisplayName(project.id, project.name)
    }

    suspend fun updateProjects(projects: List<Project>): Int =
        if (projects.isNotEmpty()) {
            projectDao.update(projects.map { it.bumpSync() })
        } else {
            0
        }

    @Transaction
    suspend fun deleteProjectsAndSubProjects(projectsToDelete: List<Project>) {
        if (projectsToDelete.isEmpty()) return
        val projectIds = projectsToDelete.map { it.id }
        listItemRepository.deleteItemsForProjects(projectIds)
        val now = System.currentTimeMillis()
        projectsToDelete.forEach { project ->
            projectDao.insert(
                project.softDelete(now),
            )
        }
    }

    suspend fun createProjectWithId(
        id: String,
        name: String,
        parentId: String?,
        roleCode: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val newProject =
            Project(
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
        projectDao.insert(newProject)
        if (parentId != null) {
            listItemRepository.addProjectLinkToProject(id, parentId)
        }
        aiEventRepository.emit(
            ProjectActivatedEvent(
                timestamp = java.time.Instant.ofEpochMilli(now),
                projectId = id,
            )
        )
    }



    @Transaction
    suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
        return searchRepository.searchGlobal(query)
    }


    @Transaction
    suspend fun moveProject(
        projectToMove: Project,
        newParentId: String?,
        allowSystemProjectMoves: Boolean = false,
    ) {
        val projectFromDb = projectDao.getProjectById(projectToMove.id) ?: return
        val oldParentId = projectFromDb.parentId

        if (oldParentId != newParentId) {
            if (oldParentId != null) {
                listItemRepository.deleteLinkByEntityIdAndProjectId(projectToMove.id, oldParentId)
            }
            if (newParentId != null) {
                listItemRepository.addProjectLinkToProject(projectToMove.id, newParentId)
            }

            val oldSiblings =
                (
                    if (oldParentId != null) {
                        projectDao.getProjectsByParentId(oldParentId)
                    } else {
                        projectDao.getTopLevelProjects()
                    }
                ).filter { it.id != projectToMove.id }

            if (oldSiblings.isNotEmpty()) {
                projectDao.update(oldSiblings.mapIndexed { index, project -> project.copy(order = index.toLong()) })
            }
        }

        val newSiblings =
            (
                if (newParentId != null) {
                    projectDao.getProjectsByParentId(newParentId)
                } else {
                    projectDao.getTopLevelProjects()
                }
            ).filter { it.id != projectToMove.id }

        val finalProjectToMove =
            projectToMove.copy(
                parentId = newParentId,
                order = newSiblings.size.toLong(),
                updatedAt = System.currentTimeMillis(),
                syncedAt = null,
                version = projectToMove.version + 1,
            )
        projectDao.update(finalProjectToMove)
    }

    @Transaction
    suspend fun addLinkItemToProjectFromLink(
        projectId: String,
        link: RelatedLink,
    ): String {
        val attachment = attachmentRepository.createLinkAttachment(projectId, link)
        return attachment.id
    }

    suspend fun linkAttachmentToProject(
        attachmentId: String,
        targetProjectId: String,
    ) = attachmentRepository.linkAttachmentToProject(attachmentId, targetProjectId)

    suspend fun ensureAttachmentLinkedToProject(
        attachmentType: String,
        entityId: String,
        targetProjectId: String,
        ownerProjectId: String?,
        createdAt: Long = System.currentTimeMillis(),
        roleCode: String? = null,
        isSystem: Boolean = false,
    ): String =
        attachmentRepository
            .ensureAttachmentLinkedToProject(
                attachmentType = attachmentType,
                entityId = entityId,
                projectId = targetProjectId,
                ownerProjectId = ownerProjectId,
                createdAt = createdAt,
                roleCode = roleCode,
                isSystem = isSystem,
            ).id

    suspend fun unlinkAttachmentFromProject(
        projectId: String,
        attachmentId: String,
    ): Boolean = attachmentRepository.unlinkAttachmentFromProject(attachmentId, projectId)

    suspend fun deleteAttachmentEverywhere(attachmentId: String) {
        val attachment = attachmentRepository.getAttachmentById(attachmentId) ?: return
        when (attachment.attachmentType) {
            ListItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.deleteDocument(attachment.entityId)
            ListItemTypeValues.CHECKLIST -> checklistRepository.deleteChecklist(attachment.entityId)
            else -> attachmentRepository.deleteAttachment(attachmentId)
        }
    }

    suspend fun findProjectIdsByTag(tag: String): List<String> = projectDao.getProjectIdsByTag(tag)

    suspend fun getProjectsByType(projectType: ProjectType): List<Project> = projectDao.getProjectsByType(projectType.name)

    suspend fun getProjectsByReservedGroup(reservedGroup: String): List<Project> = projectDao.getProjectsByReservedGroup(reservedGroup)

    suspend fun getAllProjects(): List<Project> = projectDao.getAll()



    suspend fun deleteItemByEntityId(entityId: String) = listItemRepository.deleteItemByEntityId(entityId)

    suspend fun logProjectTimeSummaryForDate(
        projectId: String,
        dayToLog: Calendar,
    ) = projectTimeTrackingRepository.logProjectTimeSummaryForDate(projectId, dayToLog)

    suspend fun recalculateAndLogProjectTime(projectId: String) = projectTimeTrackingRepository.recalculateAndLogProjectTime(projectId)

    suspend fun calculateProjectTimeMetrics(projectId: String): ProjectTimeMetrics = projectTimeTrackingRepository.calculateProjectTimeMetrics(projectId)



    suspend fun cleanupDanglingListItems() {
        val allListItems = listItemRepository.getAll()
        val itemsToDelete = mutableListOf<String>()

        allListItems.forEach { item ->
            val entityExists = when (item.itemType) {
                ListItemTypeValues.GOAL -> goalRepository.getGoalById(item.entityId) != null
                ListItemTypeValues.SUBLIST -> projectDao.getProjectById(item.entityId) != null
                ListItemTypeValues.LINK_ITEM -> listItemRepository.getLinkItemById(item.entityId) != null
                ListItemTypeValues.NOTE -> legacyNoteRepository.getNoteById(item.entityId) != null
                ListItemTypeValues.NOTE_DOCUMENT -> noteDocumentRepository.getDocumentById(item.entityId) != null
                ListItemTypeValues.CHECKLIST -> checklistRepository.getChecklistById(item.entityId) != null
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

    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?> = projectArtifactRepository.getProjectArtifactStream(projectId)

    suspend fun updateProjectArtifact(artifact: ProjectArtifact) = projectArtifactRepository.updateProjectArtifact(artifact)

    suspend fun createProjectArtifact(artifact: ProjectArtifact) = projectArtifactRepository.createProjectArtifact(artifact)

    suspend fun ensureSubprojectByRole(
        parentProjectId: String,
        roleCode: String,
        title: String
    ): Project {
        val existing = projectDao.findChildByRole(parentProjectId, roleCode)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        createProjectWithId(
            id = newId,
            name = title,
            parentId = parentProjectId,
            roleCode = roleCode,
        )
        return projectDao.getProjectById(newId) ?: Project(
            id = newId,
            name = title,
            parentId = parentProjectId,
            description = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncedAt = null,
            version = 1,
            roleCode = roleCode,
        )
    }

    suspend fun ensureChildProjectListItemsExist(projectId: String) {
        val children = projectDao.getProjectsByParentId(projectId)
        val backlogItems = listItemRepository.getItemsForProjectStream(projectId).first()
        val backlogSubprojectIds = backlogItems.filter { it.itemType == ListItemTypeValues.SUBLIST }.map { it.entityId }.toSet()

        children.forEach { child ->
            if (child.id !in backlogSubprojectIds) {
                listItemRepository.addProjectLinkToProject(child.id, projectId)
            }
        }
    }
}
