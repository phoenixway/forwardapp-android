package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.findDescendantsForDeletion
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.getDescendantIds
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import timber.log.Timber

class ContextActionsUseCase
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val syncRepository: SyncRepository,
        private val settingsRepository: SettingsRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun addNewProject(
            id: String,
            parentId: String?,
            name: String,
            allProjects: List<Context>,
        ) = withContext(ioDispatcher) {
            if (name.isBlank()) return@withContext
            contextRepository.createContextWithId(id, name, parentId)
            if (parentId != null) {
                val parentProject = allProjects.find { it.id == parentId }
                if (parentProject != null && !parentProject.isExpanded) {
                    contextRepository.updateContext(parentProject.copy(isExpanded = true))
                }
            }
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
            val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            val descendantIds = getDescendantIds(project.id, childMap).joinToString(",")
            val currentParentId = project.parentId ?: "root"
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

            if (projectToMove.parentId == finalNewParentId) return@withContext

            val allowSystemMoves = settingsRepository.allowSystemProjectMovesFlow.first()
            contextRepository.moveContext(projectToMove, finalNewParentId, allowSystemMoves)

            if (finalNewParentId != null) {
                val parentProject = allProjects.find { it.id == finalNewParentId }
                if (parentProject != null && !parentProject.isExpanded) {
                    contextRepository.updateContext(parentProject.copy(isExpanded = true))
                }
            }
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

            val newParentId = toProject.parentId
            val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            val descendantsOfFrom = getDescendantIds(fromProject.id, childMap)
            if (newParentId == fromProject.id || (newParentId != null && descendantsOfFrom.contains(newParentId))) {
                return@withContext // Prevent cycles
            }

            val now = System.currentTimeMillis()
            val sourceParentId = fromProject.parentId
            val sourceSiblings =
                allProjects
                    .filter { it.parentId == sourceParentId }
                    .sortedBy { it.order }
            val targetSiblings =
                allProjects
                    .filter { it.parentId == newParentId }
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

                if (newParentId != null) {
                    val newParent = allProjects.find { it.id == newParentId }
                    if (newParent != null && !newParent.isExpanded) {
                        updates.add(newParent.copy(isExpanded = true, updatedAt = now))
                    }
                }
            }

            if (updates.isNotEmpty()) {
                contextRepository.updateContexts(updates)
            }
        }

        suspend fun collapseAllProjects(allProjects: List<Context>) =
            withContext(ioDispatcher) {
                val projectsToCollapse =
                    allProjects
                        .filter { it.isExpanded }
                        .map { it.copy(isExpanded = false) }
                if (projectsToCollapse.isNotEmpty()) {
                    contextRepository.updateContexts(projectsToCollapse)
                }
            }

        suspend fun onToggleExpanded(project: Context) =
            withContext(ioDispatcher) {
                contextRepository.updateContext(project.copy(isExpanded = !project.isExpanded))
            }

        suspend fun exportToFile() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFile() }

        suspend fun exportToFileV2() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFileV2() }

        suspend fun exportAttachments(): Result<String> {
            return withContext(ioDispatcher) { syncRepository.exportAttachmentsToFile() }
        }

        suspend fun onFullImportConfirmed(uri: Uri): Result<String> {
            Log.e("GEMINI_DEBUG", "ProjectActionsUseCase.onFullImportConfirmed is called")
            return withContext(ioDispatcher) { syncRepository.importFullBackupFromFile(uri) }
        }

        suspend fun onFullImportConfirmedV2(uri: Uri): Result<String> {
            Timber.tag("DEBUG_IMPORT").e("ProjectActionsUseCase.onFullImportConfirmedV2 is called")
            return withContext(ioDispatcher) { syncRepository.importFullBackupFromFileV2(uri) }
        }

        suspend fun importAttachments(uri: Uri): Result<String> {
            Log.d("SyncRepo_AttachmentsImport", "ProjectActionsUseCase.importAttachments called with uri=$uri")
            return withContext(ioDispatcher) {
                Log.d("SyncRepo_AttachmentsImport", "About to call syncRepository.importAttachmentsFromFile")
                syncRepository.importAttachmentsFromFile(uri)
            }
        }

        suspend fun onBottomNavExpandedChange(expanded: Boolean) =
            withContext(ioDispatcher) { settingsRepository.saveBottomNavExpanded(expanded) }
    }
