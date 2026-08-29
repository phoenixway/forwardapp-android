package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
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
        private val entryDao: WorkspaceDirectionEntryDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val orientationRepository: CanonicalOrientationRepository,
    ) {
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

                val capability = requireActiveDirectionCapability(workspaceId)
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

                val capability = requireActiveDirectionCapability(workspaceId)
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
                requireActiveDirectionCapability(workspaceId)
                require(orderedEntryIds.size == orderedEntryIds.distinct().size) {
                    "Direction reorder contains duplicate ids"
                }

                val current = entryDao.getLiveForWorkspace(workspaceId)
                require(current.all {
                    it.provenance == WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name
                }) {
                    "Legacy Direction shadows are read-only before authority cutover"
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
                                entry.provenance ==
                                    WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                            ) {
                                "Legacy Direction shadow is read-only before authority cutover"
                            }
                            entry.bump(now).copy(isDeleted = true)
                        }
                    }

                if (changes.isNotEmpty()) {
                    entryDao.upsert(changes)
                }
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
                entry.provenance == WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
            ) {
                "Legacy Direction shadow is read-only before authority cutover"
            }
            return entry
        }

        private fun WorkspaceDirectionEntryEntity.bump(now: Long) =
            copy(
                updatedAt = now,
                syncedAt = null,
                version = version + 1L,
            )

        private suspend fun requireActiveDirectionCapability(workspaceId: String) =
            orientationDao.getAllWorkspaceCapabilities()
                .filter {
                    it.workspaceId == workspaceId &&
                        it.capabilityType == WorkspaceCapabilityType.DIRECTION.name &&
                        it.instanceKey == "default"
                }
                .also { matches ->
                    val workspace = requireNotNull(workspaceDao.getById(workspaceId)) {
                        "Direction Workspace does not exist"
                    }
                    require(!workspace.isDeleted) { "Direction Workspace is deleted" }
                    require(matches.size == 1) {
                        "Expected one stable default DIRECTION capability for Workspace $workspaceId"
                    }
                }
                .single()
                .also {
                    require(!it.isDeleted && it.state == "ACTIVE") {
                        "DIRECTION capability is not active"
                    }
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
