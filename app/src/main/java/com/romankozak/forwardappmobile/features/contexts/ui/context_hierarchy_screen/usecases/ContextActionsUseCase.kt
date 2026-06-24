package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextParentLinkDao
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.NO_GROUP_NODE_ID
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.displayParentId
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.findDescendantsForDeletion
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.getDescendantIds
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ContextActionsUseCase
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val contextParentLinkDao: ContextParentLinkDao,
        private val syncRepository: SyncRepository,
        private val settingsRepository: SettingsRepository,
        private val mainBeaconRepository: MainBeaconRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun addNewProject(
            id: String,
            parentId: String?,
            name: String,
            roleCode: String? = null,
        ) = withContext(ioDispatcher) {
            if (name.isBlank()) return@withContext
            contextRepository.createContextWithId(id, name, parentId, roleCode = roleCode)
        }

        suspend fun onDeleteProjectConfirmed(
            project: Context,
            childMap: Map<String, List<Context>>,
        ) = withContext(ioDispatcher) {
            val projectsToDelete = findDescendantsForDeletion(project.id, childMap)
            contextRepository.deleteContextsAndSubContexts(listOf(project) + projectsToDelete)
        }

        fun getMoveProjectRoute(
            project: Context,
            allProjects: List<Context>,
        ): NavTarget.ListChooser {
            val title = "Move '${project.name}'"
            val projectsById = allProjects.associateBy { it.id }
            val childMap =
                allProjects
                    .mapNotNull { context ->
                        context.displayParentId(projectsById)?.let { parentId -> parentId to context }
                    }.groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )
            val descendantIds = getDescendantIds(project.id, childMap).joinToString(",")
            val currentParentId = project.displayParentId(projectsById) ?: "root"
            val disabledIds = "${project.id}${if (descendantIds.isNotEmpty()) ",$descendantIds" else ""}"
            return NavTarget.ListChooser(
                title = title,
                currentParentId = currentParentId,
                disabledIds = disabledIds,
            )
        }

        suspend fun onListChooserResult(
            newParentId: String?,
            projectBeingMovedId: String?,
            allProjects: List<Context>,
        ) = withContext(ioDispatcher) {
            val projectToMoveId = projectBeingMovedId ?: return@withContext
            val projectToMove = allProjects.find { it.id == projectToMoveId } ?: return@withContext
            val finalNewParentId = if (newParentId == "root") null else newParentId
            val projectsById = allProjects.associateBy { it.id }

            if (projectToMove.displayParentId(projectsById) == finalNewParentId) return@withContext
            if (SystemContexts.isPinnedRoot(ContextId(projectToMove.id)) && finalNewParentId != null) {
                return@withContext
            }

            contextRepository.moveContext(projectToMove, finalNewParentId, allowSystemMoves = true)
        }

        suspend fun onProjectReorder(
            fromId: String,
            toId: String,
            position: DropPosition,
            isSearchActive: Boolean,
            allProjects: List<Context>,
        ) = withContext(ioDispatcher) {
            if (fromId == toId || isSearchActive) return@withContext

            val fromProject = allProjects.find { it.id == fromId }
            val toProject = allProjects.find { it.id == toId }

            if (fromProject == null || toProject == null) {
                return@withContext
            }

            val projectsById = allProjects.associateBy { it.id }
            val newParentId = toProject.displayParentId(projectsById)
            if (SystemContexts.isPinnedRoot(ContextId(fromProject.id)) && newParentId != null) {
                return@withContext
            }
            val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            val descendantsOfFrom = getDescendantIds(fromProject.id, childMap)
            if (newParentId == fromProject.id || (newParentId != null && descendantsOfFrom.contains(newParentId))) {
                return@withContext // Prevent cycles
            }

            val now = System.currentTimeMillis()
            val sourceParentId = fromProject.displayParentId(projectsById)
            val sourceSiblings =
                allProjects
                    .filter { it.displayParentId(projectsById) == sourceParentId }
                    .sortedBy { it.order }
            val targetSiblings =
                allProjects
                    .filter { it.displayParentId(projectsById) == newParentId }
                    .sortedBy { it.order }

            val targetList = targetSiblings.filterNot { it.id == fromId }.toMutableList()
            val targetIndex = targetList.indexOfFirst { it.id == toId }
            if (targetIndex == -1) return@withContext

            val insertionIndex =
                when (position) {
                    DropPosition.BEFORE -> targetIndex
                    DropPosition.AFTER -> targetIndex + 1
                }.coerceIn(0, targetList.size)

            val movedProject = fromProject.copy(parentId = newParentId)
            targetList.add(insertionIndex, movedProject)

            val updates = mutableListOf<Context>()

            if (newParentId == sourceParentId) {
                val reordered =
                    targetList.mapIndexed { index, project ->
                        val base = if (project.id == fromId) movedProject else project
                        base.copy(order = index.toLong(), updatedAt = now)
                    }
                updates.addAll(reordered)
            } else {
                val sourceWithout =
                    sourceSiblings
                        .filterNot { it.id == fromId }
                        .mapIndexed { index, project ->
                            project.copy(order = index.toLong(), updatedAt = now)
                        }

                val targetWithOrder =
                    targetList.mapIndexed { index, project ->
                        val base = if (project.id == fromId) movedProject else project
                        base.copy(parentId = newParentId, order = index.toLong(), updatedAt = now)
                    }

                updates.addAll(sourceWithout)
                updates.addAll(targetWithOrder)
            }

            if (updates.isNotEmpty()) {
                contextRepository.updateContexts(updates)
            }
        }

        suspend fun reorderContextSiblings(
            parentContextId: String?,
            orderedContextIds: List<String>,
            allProjects: List<Context>,
        ) = withContext(ioDispatcher) {
            if (orderedContextIds.isEmpty()) return@withContext
            if (parentContextId != null && mainBeaconRepository.getBeaconById(parentContextId) != null) {
                mainBeaconRepository.reorderBeaconContexts(parentContextId, orderedContextIds)
                return@withContext
            }
            val contextsById = allProjects.associateBy { it.id }
            val activeLinks = contextParentLinkDao.getActiveLinks()
            val linkedChildIds =
                parentContextId
                    ?.let { parentId ->
                        activeLinks
                            .filter { it.parentContextId == parentId }
                            .mapTo(hashSetOf()) { it.childContextId }
                    }
                    .orEmpty()
            val now = System.currentTimeMillis()
            val contextUpdates = mutableListOf<Context>()

            orderedContextIds.forEachIndexed { index, contextId ->
                val context = contextsById[contextId] ?: return@forEachIndexed
                if (context.parentId == parentContextId) {
                    contextUpdates += context.copy(order = index.toLong(), updatedAt = now)
                } else if (parentContextId != null && contextId in linkedChildIds) {
                    contextParentLinkDao.updateOrder(
                        parentContextId = parentContextId,
                        childContextId = contextId,
                        order = index.toLong(),
                        updatedAt = now,
                    )
                }
            }

            if (contextUpdates.isNotEmpty()) {
                contextRepository.updateContexts(contextUpdates)
            }
        }

        suspend fun reorderOrientationBeaconSiblings(
            parentNodeId: String,
            orderedBeaconIds: List<String>,
        ) = withContext(ioDispatcher) {
            if (orderedBeaconIds.isEmpty()) return@withContext
            when (parentNodeId) {
                NO_GROUP_NODE_ID -> mainBeaconRepository.reorderBeacons(orderedBeaconIds)
                else -> {
                    val groups = mainBeaconRepository.observeGroups().first()
                    if (groups.any { it.id == parentNodeId }) {
                        mainBeaconRepository.reorderBeaconGroupMembers(parentNodeId, orderedBeaconIds)
                    } else {
                        mainBeaconRepository.reorderBeaconParentChildren(parentNodeId, orderedBeaconIds)
                    }
                }
            }
        }

        suspend fun reorderOrientationGroups(orderedGroupIds: List<String>) =
            withContext(ioDispatcher) {
                mainBeaconRepository.reorderGroups(orderedGroupIds)
            }

        suspend fun addAdditionalParentLinks(
            parentContextId: String,
            childContextIds: Set<String>,
            allProjects: List<Context>,
        ): Int = withContext(ioDispatcher) {
            if (childContextIds.isEmpty()) return@withContext 0
            val contextsById = allProjects.associateBy { it.id }
            if (parentContextId !in contextsById) return@withContext 0

            val activeLinks = contextParentLinkDao.getActiveLinks()
            val existingPairs = activeLinks.mapTo(hashSetOf()) { it.parentContextId to it.childContextId }
            var nextOrder = contextParentLinkDao.getMaxOrderForParent(parentContextId) + 1L
            val acceptedLinks = mutableListOf<ContextParentLink>()

            childContextIds.forEach { childContextId ->
                if (childContextId !in contextsById) return@forEach
                if (childContextId == parentContextId) return@forEach
                if (contextsById[childContextId]?.parentId == parentContextId) return@forEach
                if ((parentContextId to childContextId) in existingPairs) return@forEach

                val candidateLinks =
                    activeLinks + acceptedLinks
                if (wouldCreateParentLinkCycle(
                        parentContextId = parentContextId,
                        childContextId = childContextId,
                        allProjects = allProjects,
                        parentLinks = candidateLinks,
                    )
                ) {
                    return@forEach
                }

                acceptedLinks +=
                    ContextParentLink(
                        parentContextId = parentContextId,
                        childContextId = childContextId,
                        order = nextOrder++,
                        createdAt = System.currentTimeMillis(),
                    )
                existingPairs += parentContextId to childContextId
            }

            if (acceptedLinks.isNotEmpty()) {
                contextParentLinkDao.insertAll(acceptedLinks)
            }
            acceptedLinks.size
        }

        suspend fun exportToFile() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFile() }

        suspend fun exportToFileV2() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFileV2() }

        suspend fun exportAttachments(): Result<String> {
            return withContext(ioDispatcher) { syncRepository.exportAttachmentsToFile() }
        }

        suspend fun onFullImportConfirmed(uri: Uri): Result<String> {
            Timber.tag("DEBUG_IMPORT").d("ProjectActionsUseCase.onFullImportConfirmed is called")
            return withContext(ioDispatcher) { syncRepository.importFullBackupFromFile(uri) }
        }

        suspend fun onFullImportConfirmedV2(uri: Uri): Result<String> {
            Timber.tag("DEBUG_IMPORT").e("ProjectActionsUseCase.onFullImportConfirmedV2 is called")
            return withContext(ioDispatcher) { syncRepository.importFullBackupFromFileV2(uri) }
        }

        suspend fun importAttachments(uri: Uri): Result<String> {
            Timber.tag("SyncRepo_AttachmentsImport").d("ProjectActionsUseCase.importAttachments called with uri=$uri")
            return withContext(ioDispatcher) {
                Timber.tag("SyncRepo_AttachmentsImport").d("About to call syncRepository.importAttachmentsFromFile")
                syncRepository.importAttachmentsFromFile(uri)
            }
        }

        suspend fun onBottomNavExpandedChange(expanded: Boolean) =
            withContext(ioDispatcher) { settingsRepository.saveBottomNavExpanded(expanded) }

        private fun wouldCreateParentLinkCycle(
            parentContextId: String,
            childContextId: String,
            allProjects: List<Context>,
            parentLinks: List<ContextParentLink>,
        ): Boolean {
            val childrenByParentId = mutableMapOf<String, MutableList<String>>()
            allProjects.forEach { context ->
                context.parentId?.let { parentId ->
                    childrenByParentId.getOrPut(parentId) { mutableListOf() } += context.id
                }
            }
            parentLinks
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.parentContextId != parentContextId || it.childContextId != childContextId }
                .forEach { link ->
                    childrenByParentId.getOrPut(link.parentContextId) { mutableListOf() } += link.childContextId
                }

            val pending = ArrayDeque<String>()
            val visited = mutableSetOf<String>()
            pending += childContextId
            while (pending.isNotEmpty()) {
                val currentId = pending.removeFirst()
                if (!visited.add(currentId)) continue
                if (currentId == parentContextId) return true
                childrenByParentId[currentId].orEmpty().forEach(pending::add)
            }
            return false
        }
    }
