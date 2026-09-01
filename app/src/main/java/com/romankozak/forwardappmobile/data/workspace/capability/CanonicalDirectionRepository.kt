package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import java.util.UUID
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDirectionEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalDirectionRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val instanceStore: CanonicalCapabilityInstanceStore,
        private val entryDao: WorkspaceDirectionEntryDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val orientationRepository: CanonicalOrientationRepository,
    ) {
        private val entryEditor =
            CanonicalDirectionEntryEditor(
                database = database,
                entryDao = entryDao,
                workspaceDao = workspaceDao,
                orientationDao = orientationDao,
                orientationRepository = orientationRepository,
            )
        private val entryLifecycle = CanonicalDirectionEntryLifecycle(database, entryDao)

        suspend fun enable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): String = instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.disable(SPEC, workspaceId, now)

        suspend fun archive(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.restore(SPEC, workspaceId, now)

        /**
         * Deletes capability-instance metadata only. DIRECTION content remains
         * persisted so a later re-enable can recover the same typed entries.
         */
        suspend fun delete(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.delete(SPEC, workspaceId, now)

        fun observeItems(workspaceId: String): Flow<List<CanonicalDirectionItem>> =
            combine(
                entryDao.observeLiveForWorkspace(workspaceId),
                orientationDao.observeManagedSubjects(),
            ) { entries, subjects ->
                toReadItems(entries, subjects)
            }

        suspend fun getItems(workspaceId: String): List<CanonicalDirectionItem> =
            toReadItems(
                entries = entryDao.getLiveForWorkspace(workspaceId),
                subjects = orientationDao.getAllManagedSubjects(),
            )

        suspend fun getItemsByIds(ids: List<String>): List<CanonicalDirectionItem> {
            if (ids.isEmpty()) return emptyList()
            return toReadItems(
                entries = entryDao.getByIds(ids).filterNot { it.isDeleted },
                subjects = orientationDao.getAllManagedSubjects(),
            )
        }

        suspend fun createSemanticDirection(
            workspaceId: String,
            title: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalized = title.trim()
                require(normalized.isNotEmpty()) { "Direction title must not be blank" }

                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val assessment = emptyApplicableAssessment()
                val orientationId = UUID.randomUUID().toString()

                orientationRepository.saveOrientation(
                    subject = ManagedSubject(
                        id = orientationId,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        subjectType = ManagedSubjectType.ORIENTATION,
                        title = normalized,
                        description = null,
                    ),
                    orientation = OrientationNode(
                        subjectId = orientationId,
                        kind = OrientationKind.DIRECTION,
                        lifecycle = null,
                        lifecycleOrigin = ValueOrigin.UNSET,
                        assessment = assessment,
                    ),
                    revision = OrientationAssessmentRevision(
                        id = UUID.randomUUID().toString(),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        orientationId = orientationId,
                        effectiveFrom = now,
                        recordedAt = now,
                        source = AssessmentRevisionSource.USER,
                        reason = "Created from DIRECTION capability",
                        assessment = assessment,
                    ),
                )

                val entryId = UUID.randomUUID().toString()
                entryDao.upsert(
                    listOf(
                        WorkspaceDirectionEntryEntity(
                            id = entryId,
                            workspaceId = workspaceId,
                            capabilityInstanceId = capability.id,
                            orientationId = orientationId,
                            targetWorkspaceId = null,
                            labelOverride = null,
                            entryOrder = nextOrder(workspaceId),
                            provenance = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )
                entryId
            }

        suspend fun createWorkspaceLink(
            workspaceId: String,
            targetWorkspaceId: String,
            label: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalized = label.trim()
                require(normalized.isNotEmpty()) { "Direction link label must not be blank" }
                require(targetWorkspaceId.isNotBlank()) { "Target Workspace id must not be blank" }
                require(workspaceId != targetWorkspaceId) {
                    "Direction cannot target its owning Workspace"
                }

                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val target =
                    requireNotNull(workspaceDao.getById(targetWorkspaceId)) {
                        "Target Workspace does not exist"
                    }
                require(!target.isDeleted) { "Target Workspace is deleted" }

                val entryId = UUID.randomUUID().toString()
                entryDao.upsert(
                    listOf(
                        WorkspaceDirectionEntryEntity(
                            id = entryId,
                            workspaceId = workspaceId,
                            capabilityInstanceId = capability.id,
                            orientationId = null,
                            targetWorkspaceId = targetWorkspaceId,
                            labelOverride = normalized,
                            entryOrder = nextOrder(workspaceId),
                            provenance = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )
                entryId
            }

        /**
         * Creates a Workspace-navigation Direction at the front of the list.
         *
         * This preserves the legacy auto-link behavior: the new entry starts at order 0
         * and every existing live entry moves one position back exactly once.
         */
        suspend fun createWorkspaceLinkAtFront(
            workspaceId: String,
            targetWorkspaceId: String,
            label: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalized = label.trim()
                require(normalized.isNotEmpty()) { "Direction link label must not be blank" }
                require(targetWorkspaceId.isNotBlank()) { "Target Workspace id must not be blank" }
                require(workspaceId != targetWorkspaceId) {
                    "Direction cannot target its owning Workspace"
                }

                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val target =
                    requireNotNull(workspaceDao.getById(targetWorkspaceId)) {
                        "Target Workspace does not exist"
                    }
                require(!target.isDeleted) { "Target Workspace is deleted" }

                val current = entryDao.getLiveForWorkspace(workspaceId)
                require(
                    current.all {
                        it.hasCanonicalDirectionProvenance()
                    },
                ) {
                    "Direction entries contain unsupported provenance"
                }

                val shifted =
                    current.map { entry ->
                        entry.bumpDirectionVersion(now).copy(entryOrder = entry.entryOrder + 1L)
                    }
                if (shifted.isNotEmpty()) {
                    entryDao.upsert(shifted)
                }

                val entryId = UUID.randomUUID().toString()
                entryDao.upsert(
                    listOf(
                        WorkspaceDirectionEntryEntity(
                            id = entryId,
                            workspaceId = workspaceId,
                            capabilityInstanceId = capability.id,
                            orientationId = null,
                            targetWorkspaceId = targetWorkspaceId,
                            labelOverride = normalized,
                            entryOrder = 0L,
                            provenance = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )
                entryId
            }

        /**
         * Changes the target of an existing Workspace-navigation Direction without
         * changing placement identity, label ownership, or order.
         */
        suspend fun retargetWorkspaceLink(
            entryId: String,
            targetWorkspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = entryEditor.retargetWorkspaceLink(entryId, targetWorkspaceId, now)

        suspend fun rename(
            entryId: String,
            text: String,
            now: Long = System.currentTimeMillis(),
        ) = entryEditor.rename(entryId, text, now)

        suspend fun reorder(
            workspaceId: String,
            orderedEntryIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                instanceStore.requireActiveInstance(SPEC, workspaceId)
                require(orderedEntryIds.size == orderedEntryIds.distinct().size) {
                    "Direction reorder contains duplicate ids"
                }

                val current = entryDao.getLiveForWorkspace(workspaceId)
                require(current.all {
                    it.hasCanonicalDirectionProvenance()
                }) {
                    "Direction entries contain unsupported provenance"
                }
                require(orderedEntryIds.toSet() == current.map { it.id }.toSet()) {
                    "Direction reorder must contain every active entry exactly once"
                }

                val byId = current.associateBy { it.id }
                val changes =
                    orderedEntryIds.mapIndexedNotNull { index, id ->
                        val entry = byId.getValue(id)
                        val newOrder = index + 1L
                        if (entry.entryOrder == newOrder) {
                            null
                        } else {
                            entry.bumpDirectionVersion(now).copy(entryOrder = newOrder)
                        }
                    }

                if (changes.isNotEmpty()) {
                    entryDao.upsert(changes)
                }
            }
        }


        /**
         * Converts one semantic Direction placement into a Workspace-navigation placement.
         *
         * The placement identity is preserved. The former Orientation is intentionally not
         * tombstoned here because WorkspaceDirectionEntry does not own Orientation lifecycle.
         */
        suspend fun convertSemanticToWorkspaceLink(
            entryId: String,
            targetWorkspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = entryEditor.convertSemanticToWorkspaceLink(entryId, targetWorkspaceId, now)

        /**
         * Converts one Workspace-navigation placement back into a semantic Direction.
         *
         * A fresh Orientation identity is created from the visible link label. We deliberately
         * do not guess that a previously detached Orientation is still the semantic owner.
         */
        suspend fun convertWorkspaceLinkToSemantic(
            entryId: String,
            now: Long = System.currentTimeMillis(),
        ) = entryEditor.convertWorkspaceLinkToSemantic(entryId, now)

        suspend fun tombstone(
            entryId: String,
            now: Long = System.currentTimeMillis(),
        ) = entryLifecycle.tombstone(entryId, now)

        suspend fun tombstoneMany(
            entryIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) = entryLifecycle.tombstoneMany(entryIds, now)

        /** Owner deletion bypasses the normal active-capability authoring guard. */
        suspend fun tombstoneOwnedEntriesForWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int = entryLifecycle.tombstoneOwnedEntriesForWorkspaces(workspaceIds, now)

        /**
         * Tombstones live Workspace-navigation Directions that target any supplied Workspace.
         *
         * This is the canonical replacement for the pre-cutover
         * linked-Context cleanup path.
         */
        suspend fun tombstoneWorkspaceLinksTargeting(
            targetWorkspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int = entryLifecycle.tombstoneWorkspaceLinksTargeting(targetWorkspaceIds, now)

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.DIRECTION,
                    configurationCodec = DirectionCapabilityConfigurationCodec,
                    workspaceAuthority =
                        CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }

        private suspend fun nextOrder(workspaceId: String): Long =
            (entryDao.getLiveForWorkspace(workspaceId).maxOfOrNull { it.entryOrder } ?: 0L) + 1L

        private fun toReadItems(
            entries: List<WorkspaceDirectionEntryEntity>,
            subjects: List<ManagedSubjectEntity>,
        ): List<CanonicalDirectionItem> {
            val subjectById = subjects.associateBy { it.id }

            return entries
                .filterNot { it.isDeleted }
                .sortedWith(compareBy<WorkspaceDirectionEntryEntity> { it.entryOrder }.thenBy { it.id })
                .map { entry ->
                    when {
                        entry.orientationId != null && entry.targetWorkspaceId == null -> {
                            val subject =
                                requireNotNull(subjectById[entry.orientationId]) {
                                    "Direction entry ${entry.id} references missing Orientation subject"
                                }
                            require(!subject.isDeleted) {
                                "Direction entry ${entry.id} references deleted Orientation subject"
                            }
                            CanonicalDirectionItem(
                                id = entry.id,
                                workspaceId = entry.workspaceId,
                                capabilityInstanceId = entry.capabilityInstanceId,
                                kind = CanonicalDirectionItemKind.SEMANTIC,
                                text = subject.title,
                                orientationId = entry.orientationId,
                                targetWorkspaceId = null,
                                order = entry.entryOrder,
                                version = entry.version,
                            )
                        }

                        entry.orientationId == null && entry.targetWorkspaceId != null ->
                            CanonicalDirectionItem(
                                id = entry.id,
                                workspaceId = entry.workspaceId,
                                capabilityInstanceId = entry.capabilityInstanceId,
                                kind = CanonicalDirectionItemKind.WORKSPACE_LINK,
                                text = entry.labelOverride.orEmpty(),
                                orientationId = null,
                                targetWorkspaceId = entry.targetWorkspaceId,
                                order = entry.entryOrder,
                                version = entry.version,
                            )

                        else -> error("Direction entry ${entry.id} has invalid target shape")
                    }
                }
        }
    }

enum class CanonicalDirectionItemKind {
    SEMANTIC,
    WORKSPACE_LINK,
}

data class CanonicalDirectionItem(
    val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val kind: CanonicalDirectionItemKind,
    val text: String,
    val orientationId: String?,
    val targetWorkspaceId: String?,
    val order: Long,
    val version: Long,
)
