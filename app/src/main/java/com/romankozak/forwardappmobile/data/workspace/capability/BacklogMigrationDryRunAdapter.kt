package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationBindings
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationPlan
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationPlanner
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogOwnerWorkspaceState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogTargetState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyBacklogItemSource
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyBacklogOrderSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class BacklogMigrationDryRunIssueCode {
    CAPABILITY_ID_COLLISION,
    INVALID_EXISTING_CAPABILITY_ID,
    CONTEXT_BACKED_CANONICAL_DESTINATION_PRESENT,
}

data class BacklogMigrationDryRunIssue(
    val code: BacklogMigrationDryRunIssueCode,
    val workspaceId: String?,
    val sourceId: String?,
    val detail: String,
)

data class BacklogMigrationDryRunReport(
    val plan: BacklogMigrationPlan,
    val expectedCapabilityInstanceIdByWorkspaceId: Map<String, String>,
    val preflightIssues: List<BacklogMigrationDryRunIssue>,
) {
    val canApply: Boolean
        get() = preflightIssues.isEmpty() && plan.canApply

    val isFullyAccounted: Boolean
        get() = canApply && plan.isFullyAccounted
}

/**
 * Stage-4 BACKLOG preflight.
 *
 * This adapter deliberately performs no migration or repair. It snapshots one
 * Room transaction, converts that snapshot into the shared frozen planner
 * contract, and reports anything that prevents the later atomic cutover.
 */
@Singleton
class BacklogMigrationDryRunAdapter
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        /**
         * One-way compatibility boundary for a pre-cutover full backup.
         *
         * The imported legacy rows are only planner evidence. They never become
         * runtime authority, and any ambiguity aborts the surrounding restore transaction.
         */
        suspend fun materializeLegacyFullBackup(): BacklogMigrationDryRunReport =
            database.withTransaction {
                val report = dryRun()
                require(report.canApply && report.isFullyAccounted) {
                    buildString {
                        append("Legacy BACKLOG full-backup fallback was rejected")
                        report.preflightIssues.forEach { append("\n${it.code}: ${it.detail}") }
                        report.plan.issues.forEach { append("\n${it.code}: ${it.detail}") }
                    }
                }

                val workspaces = database.workspaceDao().getAll().associateBy { it.id }
                val capabilities = database.orientationDao().getAllWorkspaceCapabilities()
                val existingLogical =
                    capabilities
                        .filter {
                            it.capabilityType == BACKLOG_CAPABILITY_TYPE &&
                                it.instanceKey == DEFAULT_INSTANCE_KEY
                        }
                        .associateBy { it.workspaceId }
                val fallbackOrder = (capabilities.maxOfOrNull { it.capabilityOrder } ?: -1L) + 1L
                val now = System.currentTimeMillis()
                val missingCapabilities =
                    report.expectedCapabilityInstanceIdByWorkspaceId.mapNotNull { (workspaceId, id) ->
                        if (existingLogical[workspaceId] != null) return@mapNotNull null
                        val workspace = requireNotNull(workspaces[workspaceId]) {
                            "Legacy BACKLOG fallback lost Workspace $workspaceId"
                        }
                        WorkspaceCapabilityInstanceEntity(
                            id = id,
                            workspaceId = workspaceId,
                            capabilityType = BACKLOG_CAPABILITY_TYPE,
                            instanceKey = DEFAULT_INSTANCE_KEY,
                            capabilityOrder = fallbackOrder,
                            state = "DISABLED",
                            configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                            configuration = BacklogCapabilityConfigurationCodec.encodeDefault(),
                            createdAt = workspace.createdAt,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        )
                    }
                if (missingCapabilities.isNotEmpty()) {
                    database.orientationDao().upsertWorkspaceCapabilities(missingCapabilities)
                }

                database.workspaceBacklogEntryDao().upsert(
                    report.plan.entries.map { entry ->
                        WorkspaceBacklogEntryEntity(
                            id = entry.id,
                            workspaceId = entry.workspaceId,
                            capabilityInstanceId = entry.capabilityInstanceId,
                            targetKind = entry.target.kind.name,
                            targetId = entry.target.id,
                            entryOrder = entry.order,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt,
                            syncedAt = null,
                            isDeleted = entry.isDeleted,
                            version = entry.version,
                        )
                    },
                )
                report
            }

        suspend fun dryRun(): BacklogMigrationDryRunReport =
            database.withTransaction {
                val legacyItems = database.listItemDao().getAllRaw()
                val legacyOrders = database.backlogOrderDao().getAllRaw()

                val workspaces = database.workspaceDao().getAll()
                val capabilities = database.orientationDao().getAllWorkspaceCapabilities()
                val mappings = database.orientationDao().getAllLegacyMappings()
                val managedSubjects = database.orientationDao().getAllManagedSubjects()
                val orientations = database.orientationDao().getAllOrientations()

                val linkItems = database.linkItemDao().getAllRaw()
                val legacyNotes = database.legacyNoteDao().getAllRaw()
                val documents = database.noteDocumentDao().getAllDocumentsRaw()
                val checklists = database.checklistDao().getAllChecklistsRaw()
                val musicNotes = database.musicNoteDao().getAll()

                val existingEntries = database.workspaceBacklogEntryDao().getAll()

                val provenContextBacked =
                    workspaces.filter { workspace ->
                        workspace.provenance == CONTEXT_BACKED_PROVENANCE &&
                            !workspace.sourceContextId.isNullOrBlank() &&
                            workspace.id == workspace.sourceContextId
                    }

                val workspaceIdByContextId =
                    provenContextBacked.associate { workspace ->
                        requireNotNull(workspace.sourceContextId) to workspace.id
                    }

                val ownerWorkspaceStateById =
                    provenContextBacked.associate { workspace ->
                        workspace.id to
                            BacklogOwnerWorkspaceState(
                                isDeleted = workspace.isDeleted,
                            )
                    }

                val preflightIssues = mutableListOf<BacklogMigrationDryRunIssue>()

                val expectedCapabilityIds =
                    expectedBacklogCapabilityIds(
                        workspaceIds =
                            provenContextBacked
                                .asSequence()
                                .filterNot { it.isDeleted }
                                .map { it.id }
                                .toList(),
                        capabilities = capabilities,
                        issues = preflightIssues,
                    )

                val contextBackedWorkspaceIds =
                    provenContextBacked.mapTo(hashSetOf()) { it.id }

                existingEntries
                    .filter { it.workspaceId in contextBackedWorkspaceIds }
                    .forEach { entry ->
                        preflightIssues +=
                            BacklogMigrationDryRunIssue(
                                code =
                                    BacklogMigrationDryRunIssueCode
                                        .CONTEXT_BACKED_CANONICAL_DESTINATION_PRESENT,
                                workspaceId = entry.workspaceId,
                                sourceId = entry.id,
                                detail =
                                    "Context-backed Workspace already contains a canonical " +
                                        "BACKLOG entry before the atomic authority cutover",
                            )
                    }

                val orientationIdByGoalId =
                    mappings
                        .asSequence()
                        .filter { mapping ->
                            mapping.sourceType == GOAL_SOURCE_TYPE &&
                                !mapping.isDeleted &&
                                mapping.state == CUT_OVER_MAPPING_STATE
                        }
                        .associate { mapping ->
                            mapping.sourceId to mapping.subjectId
                        }

                val targetStateByRef =
                    buildTargetStates(
                        workspaces = workspaces,
                        managedSubjects = managedSubjects,
                        orientationSubjectIds =
                            orientations.mapTo(hashSetOf()) { it.subjectId },
                        linkItems = linkItems,
                        legacyNotes = legacyNotes,
                        documents = documents,
                        checklists = checklists,
                        musicNotes = musicNotes,
                    )

                val bindings =
                    BacklogMigrationBindings(
                        workspaceIdByContextId = workspaceIdByContextId,
                        ownerWorkspaceStateById = ownerWorkspaceStateById,
                        capabilityInstanceIdByWorkspaceId = expectedCapabilityIds,
                        orientationIdByGoalId = orientationIdByGoalId,
                        targetStateByRef = targetStateByRef,
                        parentWorkspaceIdByWorkspaceId =
                            workspaces.associate { workspace ->
                                workspace.id to workspace.parentWorkspaceId
                            },
                        existingCanonicalIds =
                            existingEntries.mapTo(hashSetOf()) { it.id },
                    )

                val plan =
                    BacklogMigrationPlanner.plan(
                        items =
                            legacyItems.map { item ->
                                LegacyBacklogItemSource(
                                    id = item.id,
                                    contextId = item.contextId,
                                    itemType = item.itemType,
                                    entityId = item.entityId,
                                    associationOwnerContextId = item.associationOwnerContextId,
                                    associationTag = item.associationTag,
                                    order = item.order,
                                    updatedAt = item.updatedAt,
                                    syncedAt = item.syncedAt,
                                    isDeleted = item.isDeleted,
                                    version = item.version,
                                )
                            },
                        orders =
                            legacyOrders.map { order ->
                                LegacyBacklogOrderSource(
                                    id = order.id,
                                    listId = order.listId,
                                    itemId = order.itemId,
                                    order = order.order,
                                    orderVersion = order.orderVersion,
                                    updatedAt = order.updatedAt,
                                    syncedAt = order.syncedAt,
                                    isDeleted = order.isDeleted,
                                )
                            },
                        bindings = bindings,
                    )

                BacklogMigrationDryRunReport(
                    plan = plan,
                    expectedCapabilityInstanceIdByWorkspaceId = expectedCapabilityIds,
                    preflightIssues = preflightIssues.distinct(),
                )
            }

        private fun expectedBacklogCapabilityIds(
            workspaceIds: List<String>,
            capabilities: List<WorkspaceCapabilityInstanceEntity>,
            issues: MutableList<BacklogMigrationDryRunIssue>,
        ): Map<String, String> {
            val existingByLogical =
                capabilities
                    .filter { capability ->
                        capability.capabilityType == BACKLOG_CAPABILITY_TYPE &&
                            capability.instanceKey == DEFAULT_INSTANCE_KEY
                    }
                    .associateBy { it.workspaceId }

            val allCapabilitiesById = capabilities.associateBy { it.id }
            val result = linkedMapOf<String, String>()

            workspaceIds
                .distinct()
                .sorted()
                .forEach { workspaceId ->
                    val existing = existingByLogical[workspaceId]
                    if (existing != null) {
                        if (existing.id.isBlank()) {
                            issues +=
                                BacklogMigrationDryRunIssue(
                                    code =
                                        BacklogMigrationDryRunIssueCode
                                            .INVALID_EXISTING_CAPABILITY_ID,
                                    workspaceId = workspaceId,
                                    sourceId = null,
                                    detail =
                                        "Existing logical BACKLOG capability instance has a blank id",
                                )
                        } else {
                            result[workspaceId] = existing.id
                        }
                        return@forEach
                    }

                    val expectedId = stableBacklogCapabilityId(workspaceId)
                    val collision = allCapabilitiesById[expectedId]
                    if (collision != null) {
                        issues +=
                            BacklogMigrationDryRunIssue(
                                code =
                                    BacklogMigrationDryRunIssueCode
                                        .CAPABILITY_ID_COLLISION,
                                workspaceId = workspaceId,
                                sourceId = collision.id,
                                detail =
                                    "Deterministic BACKLOG capability id is already occupied by " +
                                        "${collision.workspaceId}:${collision.capabilityType}:" +
                                        collision.instanceKey,
                            )
                    } else {
                        result[workspaceId] = expectedId
                    }
                }

            return result
        }

        private fun buildTargetStates(
            workspaces:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities.orientation
                        .WorkspaceEntity
                >,
            managedSubjects:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities.orientation
                        .ManagedSubjectEntity
                >,
            orientationSubjectIds: Set<String>,
            linkItems:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities
                        .LinkItemEntity
                >,
            legacyNotes:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities
                        .LegacyNoteEntity
                >,
            documents:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities
                        .NoteDocumentEntity
                >,
            checklists:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities
                        .ChecklistEntity
                >,
            musicNotes:
                List<
                    com.romankozak.forwardappmobile.core.data.models.entities
                        .MusicNoteEntity
                >,
        ): Map<WorkspaceBacklogTargetRef, BacklogTargetState> {
            val result = linkedMapOf<WorkspaceBacklogTargetRef, BacklogTargetState>()

            workspaces.forEach { workspace ->
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.WORKSPACE,
                        workspace.id,
                    ),
                ] = BacklogTargetState(isDeleted = workspace.isDeleted)
            }

            managedSubjects
                .filter { subject ->
                    subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                        subject.id in orientationSubjectIds
                }
                .forEach { subject ->
                    result[
                        WorkspaceBacklogTargetRef(
                            WorkspaceBacklogTargetKind.ORIENTATION,
                            subject.id,
                        ),
                    ] = BacklogTargetState(isDeleted = subject.isDeleted)
                }

            linkItems.forEach { item ->
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.LINK_ITEM,
                        item.id,
                    ),
                ] = BacklogTargetState(isDeleted = item.isDeleted)
            }

            legacyNotes.forEach { note ->
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.LEGACY_NOTE,
                        note.id,
                    ),
                ] = BacklogTargetState(isDeleted = note.isDeleted)
            }

            documents.forEach { document ->
                val state = BacklogTargetState(isDeleted = document.isDeleted)
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
                        document.id,
                    ),
                ] = state
            }

            checklists.forEach { checklist ->
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.CHECKLIST,
                        checklist.id,
                    ),
                ] = BacklogTargetState(isDeleted = checklist.isDeleted)
            }

            musicNotes.forEach { music ->
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.MUSIC_NOTE,
                        music.id,
                    ),
                ] = BacklogTargetState(isDeleted = music.isDeleted)
            }

            return result
        }

        private fun stableBacklogCapabilityId(workspaceId: String): String =
            LegacySubjectUuid
                .uuidV5(
                    CAPABILITY_NAMESPACE,
                    "WORKSPACE:CAPABILITY:$workspaceId:$BACKLOG_CAPABILITY_TYPE:$DEFAULT_INSTANCE_KEY",
                ).toString()

        private companion object {
            const val CONTEXT_BACKED_PROVENANCE = "CONTEXT_BACKED"
            const val BACKLOG_CAPABILITY_TYPE = "BACKLOG"
            const val DEFAULT_INSTANCE_KEY = "default"
            const val GOAL_SOURCE_TYPE = "GOAL"
            const val CUT_OVER_MAPPING_STATE = "CUT_OVER"

            val CAPABILITY_NAMESPACE: UUID =
                UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID)
        }
    }
