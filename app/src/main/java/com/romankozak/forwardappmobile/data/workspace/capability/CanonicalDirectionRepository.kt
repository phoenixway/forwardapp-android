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
                        entry.bump(now).copy(entryOrder = entry.entryOrder + 1L)
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
        ) {
            database.withTransaction {
                require(targetWorkspaceId.isNotBlank()) { "Target Workspace id must not be blank" }

                val entry = requireMutableCanonicalEntry(entryId)
                require(entry.orientationId == null && entry.targetWorkspaceId != null) {
                    "Only a Workspace link can be retargeted"
                }
                require(entry.workspaceId != targetWorkspaceId) {
                    "Direction cannot target its owning Workspace"
                }

                val target =
                    requireNotNull(workspaceDao.getById(targetWorkspaceId)) {
                        "Target Workspace does not exist"
                    }
                require(!target.isDeleted) { "Target Workspace is deleted" }

                if (entry.targetWorkspaceId != targetWorkspaceId) {
                    entryDao.upsert(
                        listOf(
                            entry.bump(now).copy(
                                targetWorkspaceId = targetWorkspaceId,
                            ),
                        ),
                    )
                }
            }
        }


        suspend fun rename(
            entryId: String,
            text: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val normalized = text.trim()
                require(normalized.isNotEmpty()) { "Direction text must not be blank" }

                val entry = requireMutableCanonicalEntry(entryId)

                when {
                    entry.orientationId != null && entry.targetWorkspaceId == null -> {
                        val orientationId = requireNotNull(entry.orientationId)
                        val subject =
                            requireNotNull(orientationDao.getManagedSubject(orientationId)) {
                                "Direction Orientation subject does not exist"
                            }
                        require(
                            subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                                !subject.isDeleted,
                        ) {
                            "Direction Orientation subject is not active"
                        }
                        require(
                            orientationDao.getAllOrientations().any {
                                it.subjectId == orientationId &&
                                    it.kind == OrientationKind.DIRECTION.name
                            },
                        ) {
                            "Direction entry does not reference a DIRECTION Orientation"
                        }

                        if (subject.title != normalized) {
                            orientationDao.upsertManagedSubjects(
                                listOf(
                                    subject.copy(
                                        title = normalized,
                                        updatedAt = now,
                                        syncedAt = null,
                                        version = subject.version + 1L,
                                    ),
                                ),
                            )
                        }
                    }

                    entry.orientationId == null && entry.targetWorkspaceId != null -> {
                        if (entry.labelOverride != normalized) {
                            entryDao.upsert(
                                listOf(
                                    entry.bump(now).copy(labelOverride = normalized),
                                ),
                            )
                        }
                    }

                    else -> error("Direction entry has invalid target shape")
                }
            }
        }

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
                            entry.bump(now).copy(entryOrder = newOrder)
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
        ) {
            database.withTransaction {
                require(targetWorkspaceId.isNotBlank()) { "Target Workspace id must not be blank" }

                val entry = requireMutableCanonicalEntry(entryId)
                require(entry.orientationId != null && entry.targetWorkspaceId == null) {
                    "Only a semantic Direction can be converted to a Workspace link"
                }
                require(entry.workspaceId != targetWorkspaceId) {
                    "Direction cannot target its owning Workspace"
                }

                val orientationId = requireNotNull(entry.orientationId)
                val subject =
                    requireNotNull(orientationDao.getManagedSubject(orientationId)) {
                        "Direction Orientation subject does not exist"
                    }
                require(
                    subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                        !subject.isDeleted,
                ) {
                    "Direction Orientation subject is not active"
                }
                require(
                    orientationDao.getAllOrientations().any {
                        it.subjectId == orientationId &&
                            it.kind == OrientationKind.DIRECTION.name
                    },
                ) {
                    "Direction Orientation has invalid kind"
                }

                val target =
                    requireNotNull(workspaceDao.getById(targetWorkspaceId)) {
                        "Target Workspace does not exist"
                    }
                require(!target.isDeleted) { "Target Workspace is deleted" }

                val label = subject.title.trim()
                require(label.isNotEmpty()) { "Direction title must not be blank" }

                entryDao.upsert(
                    listOf(
                        entry.copy(
                            orientationId = null,
                            targetWorkspaceId = targetWorkspaceId,
                            labelOverride = label,
                            updatedAt = now,
                            syncedAt = null,
                            version = entry.version + 1L,
                        ),
                    ),
                )
            }
        }

        /**
         * Converts one Workspace-navigation placement back into a semantic Direction.
         *
         * A fresh Orientation identity is created from the visible link label. We deliberately
         * do not guess that a previously detached Orientation is still the semantic owner.
         */
        suspend fun convertWorkspaceLinkToSemantic(
            entryId: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val entry = requireMutableCanonicalEntry(entryId)
                require(entry.orientationId == null && entry.targetWorkspaceId != null) {
                    "Only a Workspace link can be converted to a semantic Direction"
                }

                val title = entry.labelOverride?.trim().orEmpty()
                require(title.isNotEmpty()) { "Direction link label must not be blank" }

                val assessment = emptyApplicableAssessment()
                val orientationId = UUID.randomUUID().toString()

                orientationRepository.saveOrientation(
                    subject =
                        ManagedSubject(
                            id = orientationId,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            subjectType = ManagedSubjectType.ORIENTATION,
                            title = title,
                            description = null,
                        ),
                    orientation =
                        OrientationNode(
                            subjectId = orientationId,
                            kind = OrientationKind.DIRECTION,
                            lifecycle = null,
                            lifecycleOrigin = ValueOrigin.UNSET,
                            assessment = assessment,
                        ),
                    revision =
                        OrientationAssessmentRevision(
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
                            reason = "Converted from DIRECTION Workspace link",
                            assessment = assessment,
                        ),
                )

                entryDao.upsert(
                    listOf(
                        entry.copy(
                            orientationId = orientationId,
                            targetWorkspaceId = null,
                            labelOverride = null,
                            updatedAt = now,
                            syncedAt = null,
                            version = entry.version + 1L,
                        ),
                    ),
                )
            }
        }

        suspend fun tombstone(
            entryId: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val entry = requireMutableCanonicalEntry(entryId)
                entryDao.upsert(
                    listOf(
                        entry.bump(now).copy(isDeleted = true),
                    ),
                )
            }
        }

        suspend fun tombstoneMany(
            entryIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            if (entryIds.isEmpty()) return

            database.withTransaction {
                val requestedIds = entryIds.distinct()
                val entries = entryDao.getByIds(requestedIds).associateBy { it.id }

                val changes =
                    requestedIds.mapNotNull { id ->
                        val entry = entries[id] ?: return@mapNotNull null
                        if (entry.isDeleted) {
                            null
                        } else {
                            require(
                                entry.hasCanonicalDirectionProvenance(),
                            ) {
                                "Direction entry has unsupported provenance"
                            }
                            entry.bump(now).copy(isDeleted = true)
                        }
                    }

                if (changes.isNotEmpty()) {
                    entryDao.upsert(changes)
                }
            }
        }

        /**
         * Tombstones live Workspace-navigation Directions that target any supplied Workspace.
         *
         * This is the canonical replacement for the pre-cutover
         * linked-Context cleanup path.
         */
        suspend fun tombstoneWorkspaceLinksTargeting(
            targetWorkspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int {
            val targets =
                targetWorkspaceIds
                    .asSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toSet()
            if (targets.isEmpty()) return 0

            return database.withTransaction {
                val matches =
                    entryDao.getAll().filter { entry ->
                        !entry.isDeleted &&
                            entry.orientationId == null &&
                            entry.targetWorkspaceId in targets
                    }

                require(
                    matches.all {
                        it.hasCanonicalDirectionProvenance()
                    },
                ) {
                    "Direction entries contain unsupported provenance"
                }

                if (matches.isNotEmpty()) {
                    entryDao.upsert(
                        matches.map { entry ->
                            entry.bump(now).copy(isDeleted = true)
                        },
                    )
                }
                matches.size
            }
        }


        private suspend fun requireMutableCanonicalEntry(
            entryId: String,
        ): WorkspaceDirectionEntryEntity {
            val entry =
                requireNotNull(entryDao.getById(entryId)) {
                    "Direction entry does not exist"
                }
            require(!entry.isDeleted) { "Direction entry is deleted" }
            require(
                entry.hasCanonicalDirectionProvenance(),
            ) {
                "Direction entry has unsupported provenance"
            }
            return entry
        }

        private fun WorkspaceDirectionEntryEntity.bump(now: Long) =
            copy(
                updatedAt = now,
                syncedAt = null,
                version = version + 1L,
            )

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


private fun WorkspaceDirectionEntryEntity.hasCanonicalDirectionProvenance(): Boolean =
    provenance == WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name ||
        provenance == WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name
