package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommand
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommandService
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRolePresetInitializer
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.NO_GROUP_NODE_ID
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
        private val hierarchyOccurrenceCommandService: HierarchyOccurrenceCommandService,
        private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
        private val canonicalWorkspaceRolePresetInitializer: CanonicalWorkspaceRolePresetInitializer,
        private val canonicalDirectionRepository: CanonicalDirectionRepository,
        private val syncRepository: SyncRepository,
        private val settingsRepository: SettingsRepository,
        private val mainBeaconRepository: MainBeaconRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun addNewProject(
            parentId: String?,
            parentPlacementId: PlacementId?,
            name: String,
            roleCode: String? = null,
        ): String? = withContext(ioDispatcher) {
            val normalizedName = name.trim()
            if (normalizedName.isEmpty()) return@withContext null

            val newWorkspaceId =
                canonicalWorkspaceRepository.createWithPrimaryAppearance(
                    nameOverride = normalizedName,
                    descriptionOverride = null,
                    parentWorkspaceId = parentId,
                    parentPlacementId = parentPlacementId,
                    roleCode = roleCode,
                )

            canonicalWorkspaceRolePresetInitializer.apply(
                workspaceId = newWorkspaceId,
                roleCode = roleCode,
            )

            val normalizedParentId =
                parentId?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            if (normalizedParentId != null) {
                val parentDirection = canonicalDirectionRepository.getState(normalizedParentId)
                if (
                    parentDirection != null &&
                    !parentDirection.isDeleted &&
                    parentDirection.lifecycleState ==
                        com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState.ACTIVE &&
                    parentDirection.configuration.autoLinkChildWorkspaces
                ) {
                    canonicalDirectionRepository.createWorkspaceLinkAtFront(
                        workspaceId = normalizedParentId,
                        targetWorkspaceId = newWorkspaceId,
                        label = normalizedName,
                    )
                }
            }

            newWorkspaceId
        }

        suspend fun onDeleteProjectConfirmed(
            projectId: String,
        ) = withContext(ioDispatcher) {
            canonicalWorkspaceRepository.tombstoneSubtree(projectId)
        }

        fun getMoveProjectRoute(
            occurrence: com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef,
            read: CanonicalV2ProductionHierarchyRead,
        ): NavTarget.ListChooser? {
            val source = read.occurrence(occurrence.placementId) ?: return null

            val descendantPlacementIds = buildList {
                val pending = ArrayDeque<PlacementId>()
                read.childrenOf(source.placementId).forEach { pending.addLast(it.placementId) }

                while (pending.isNotEmpty()) {
                    val placementId = pending.removeFirst()
                    add(placementId)
                    read.childrenOf(placementId).forEach { child ->
                        pending.addLast(child.placementId)
                    }
                }
            }

            val disabledIds =
                (listOf(source.placementId) + descendantPlacementIds)
                    .joinToString(",") { it.value }

            return NavTarget.ListChooser(
                title = "Move '${source.title}'",
                currentParentId = source.parentPlacementId?.value ?: "root",
                disabledIds = disabledIds,
            )
        }

        suspend fun onListChooserResult(
            destinationPlacementId: String?,
            sourcePlacementId: String?,
        ) = withContext(ioDispatcher) {
            val placementId = sourcePlacementId?.let(::PlacementId)
                ?: return@withContext

            hierarchyOccurrenceCommandService.move(
                HierarchyOccurrenceCommand.Move(
                    placementId = placementId,
                    newParentPlacementId = destinationPlacementId?.let(::PlacementId),
                ),
            )
        }

        suspend fun reorderContextSiblings(
            parentPlacementId: PlacementId?,
            orderedPlacementIds: List<PlacementId>,
        ) = withContext(ioDispatcher) {
            if (orderedPlacementIds.isEmpty()) return@withContext

            hierarchyOccurrenceCommandService.reorderSiblings(
                HierarchyOccurrenceCommand.ReorderSiblings(
                    parentPlacementId = parentPlacementId,
                    orderedPlacementIds = orderedPlacementIds,
                ),
            )
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

        suspend fun exportToFile() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFile() }

        suspend fun exportToFileV2() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFileV2() }

        suspend fun exportAttachments(): Result<String> {
            return withContext(ioDispatcher) { syncRepository.exportAttachmentsToFile() }
        }

        suspend fun onRestoreImportConfirmed(uri: Uri): Result<String> {
            Timber.tag("DEBUG_IMPORT").d("ProjectActionsUseCase.onRestoreImportConfirmed is called")
            return withContext(ioDispatcher) { syncRepository.importFullBackupFromFile(uri) }
        }

        suspend fun onMergeImportConfirmed(uri: Uri): Result<String> {
            Timber.tag("DEBUG_IMPORT").e("ProjectActionsUseCase.onMergeImportConfirmed is called")
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

    }
