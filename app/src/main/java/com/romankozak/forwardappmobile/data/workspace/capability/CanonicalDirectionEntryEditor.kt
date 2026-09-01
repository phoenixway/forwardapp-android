package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDirectionEntryDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import java.util.UUID

internal class CanonicalDirectionEntryEditor(
    private val database: AppDatabase,
    private val entryDao: WorkspaceDirectionEntryDao,
    private val workspaceDao: WorkspaceDao,
    private val orientationDao: OrientationDao,
    private val orientationRepository: CanonicalOrientationRepository,
) {
    suspend fun retargetWorkspaceLink(
        entryId: String,
        targetWorkspaceId: String,
        now: Long,
    ) {
        database.withTransaction {
            require(targetWorkspaceId.isNotBlank()) { "Target Workspace id must not be blank" }

            val entry = requireMutableEntry(entryId)
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
                    listOf(entry.bumpDirectionVersion(now).copy(targetWorkspaceId = targetWorkspaceId)),
                )
            }
        }
    }

    suspend fun rename(
        entryId: String,
        text: String,
        now: Long,
    ) {
        database.withTransaction {
            val normalized = text.trim()
            require(normalized.isNotEmpty()) { "Direction text must not be blank" }

            val entry = requireMutableEntry(entryId)
            when {
                entry.orientationId != null && entry.targetWorkspaceId == null ->
                    renameSemanticDirection(requireNotNull(entry.orientationId), normalized, now)

                entry.orientationId == null && entry.targetWorkspaceId != null -> {
                    if (entry.labelOverride != normalized) {
                        entryDao.upsert(
                            listOf(entry.bumpDirectionVersion(now).copy(labelOverride = normalized)),
                        )
                    }
                }

                else -> error("Direction entry has invalid target shape")
            }
        }
    }

    suspend fun convertSemanticToWorkspaceLink(
        entryId: String,
        targetWorkspaceId: String,
        now: Long,
    ) {
        database.withTransaction {
            require(targetWorkspaceId.isNotBlank()) { "Target Workspace id must not be blank" }

            val entry = requireMutableEntry(entryId)
            require(entry.orientationId != null && entry.targetWorkspaceId == null) {
                "Only a semantic Direction can be converted to a Workspace link"
            }
            require(entry.workspaceId != targetWorkspaceId) {
                "Direction cannot target its owning Workspace"
            }

            val subject = requireDirectionSubject(requireNotNull(entry.orientationId))
            val target =
                requireNotNull(workspaceDao.getById(targetWorkspaceId)) {
                    "Target Workspace does not exist"
                }
            require(!target.isDeleted) { "Target Workspace is deleted" }

            val label = subject.title.trim()
            require(label.isNotEmpty()) { "Direction title must not be blank" }

            entryDao.upsert(
                listOf(
                    entry.bumpDirectionVersion(now).copy(
                        orientationId = null,
                        targetWorkspaceId = targetWorkspaceId,
                        labelOverride = label,
                    ),
                ),
            )
        }
    }

    suspend fun convertWorkspaceLinkToSemantic(
        entryId: String,
        now: Long,
    ) {
        database.withTransaction {
            val entry = requireMutableEntry(entryId)
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
                    entry.bumpDirectionVersion(now).copy(
                        orientationId = orientationId,
                        targetWorkspaceId = null,
                        labelOverride = null,
                    ),
                ),
            )
        }
    }

    private suspend fun renameSemanticDirection(
        orientationId: String,
        title: String,
        now: Long,
    ) {
        val subject =
            requireDirectionSubject(
                orientationId = orientationId,
                invalidKindMessage = "Direction entry does not reference a DIRECTION Orientation",
            )
        if (subject.title != title) {
            orientationDao.upsertManagedSubjects(
                listOf(
                    subject.copy(
                        title = title,
                        updatedAt = now,
                        syncedAt = null,
                        version = subject.version + 1L,
                    ),
                ),
            )
        }
    }

    private suspend fun requireDirectionSubject(
        orientationId: String,
        invalidKindMessage: String = "Direction Orientation has invalid kind",
    ) =
        requireNotNull(orientationDao.getManagedSubject(orientationId)) {
            "Direction Orientation subject does not exist"
        }.also { subject ->
            require(
                subject.subjectType == ManagedSubjectType.ORIENTATION.name && !subject.isDeleted,
            ) {
                "Direction Orientation subject is not active"
            }
            require(
                orientationDao.getAllOrientations().any {
                    it.subjectId == orientationId && it.kind == OrientationKind.DIRECTION.name
                },
            ) {
                invalidKindMessage
            }
        }

    private suspend fun requireMutableEntry(entryId: String): WorkspaceDirectionEntryEntity {
        val entry =
            requireNotNull(entryDao.getById(entryId)) {
                "Direction entry does not exist"
            }
        require(!entry.isDeleted) { "Direction entry is deleted" }
        require(entry.hasCanonicalDirectionProvenance()) {
            "Direction entry has unsupported provenance"
        }
        return entry
    }
}
