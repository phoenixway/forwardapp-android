package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CanonicalAspect(
    val subject: ManagedSubjectEntity,
    val node: AspectEntity,
)

/** Canonical command boundary for Aspect identity and its ordered one-parent hierarchy. */
@Singleton
class CanonicalAspectRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: OrientationDao,
    ) {
        fun observeLive(): Flow<List<CanonicalAspect>> =
            combine(dao.observeManagedSubjects(), dao.observeAspects()) { subjects, nodes ->
                val activeSubjects =
                    subjects
                        .filter { it.subjectType == ManagedSubjectType.ASPECT.name && !it.isDeleted }
                        .associateBy { it.id }
                nodes.mapNotNull { node -> activeSubjects[node.subjectId]?.let { CanonicalAspect(it, node) } }
                    .sortedWith(compareBy({ it.node.parentAspectId }, { it.node.aspectOrder }))
            }

        suspend fun get(id: String): CanonicalAspect? {
            val subject = dao.getManagedSubject(id) ?: return null
            val node = dao.getAspect(id) ?: return null
            return CanonicalAspect(subject, node)
        }

        suspend fun create(
            title: String,
            description: String? = null,
            parentAspectId: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalizedTitle = title.trim()
                require(normalizedTitle.isNotEmpty()) { "Aspect title must not be blank" }
                val live = loadLiveAspects()
                require(parentAspectId == null || parentAspectId in live) { "Aspect parent must be active" }
                val id = UUID.randomUUID().toString()
                val node =
                    AspectEntity(
                        subjectId = id,
                        parentAspectId = parentAspectId,
                        aspectOrder = nextOrder(live.values, parentAspectId),
                        archived = false,
                    )
                validateHierarchy(live.values + node)
                dao.upsertManagedSubjects(
                    listOf(
                        ManagedSubjectEntity(
                            id = id,
                            subjectType = ManagedSubjectType.ASPECT.name,
                            title = normalizedTitle,
                            description = description?.trim()?.ifEmpty { null },
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )
                dao.upsertAspects(listOf(node))
                id
            }

        suspend fun updateDetails(
            id: String,
            title: String,
            description: String?,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Aspect title must not be blank" }
            val current = requireActiveSubject(id)
            require(dao.getAspect(id) != null) { "Aspect node does not exist" }
            dao.upsertManagedSubjects(
                listOf(
                    current.bump(now).copy(
                        title = normalizedTitle,
                        description = description?.trim()?.ifEmpty { null },
                    ),
                ),
            )
        }

        suspend fun move(
            id: String,
            newParentAspectId: String?,
            order: Long? = null,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val subject = requireActiveSubject(id)
            val live = loadLiveAspects()
            val current = requireNotNull(live[id]) { "Aspect node does not exist" }
            require(newParentAspectId == null || newParentAspectId in live) { "Aspect parent must be active" }
            val changed =
                current.copy(
                    parentAspectId = newParentAspectId,
                    aspectOrder = order ?: nextOrder(live.values.filterNot { it.subjectId == id }, newParentAspectId),
                )
            validateHierarchy(live.values.filterNot { it.subjectId == id } + changed)
            dao.upsertAspects(listOf(changed))
            dao.upsertManagedSubjects(listOf(subject.bump(now)))
        }

        suspend fun reorderSiblings(
            parentAspectId: String?,
            orderedIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val live = loadLiveAspects()
            val siblings = live.values.filter { it.parentAspectId == parentAspectId }
            require(orderedIds.size == orderedIds.distinct().size) { "Aspect reorder contains duplicate ids" }
            require(orderedIds.toSet() == siblings.map { it.subjectId }.toSet()) {
                "Aspect reorder must contain every active sibling exactly once"
            }
            dao.upsertAspects(
                orderedIds.mapIndexed { index, id -> live.getValue(id).copy(aspectOrder = index.toLong()) },
            )
            dao.upsertManagedSubjects(orderedIds.map { requireActiveSubject(it).bump(now) })
        }

        suspend fun setArchived(
            id: String,
            archived: Boolean,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val subject = requireActiveSubject(id)
            val node = requireNotNull(dao.getAspect(id)) { "Aspect node does not exist" }
            if (node.archived != archived) {
                dao.upsertAspects(listOf(node.copy(archived = archived)))
                dao.upsertManagedSubjects(listOf(subject.bump(now)))
            }
        }

        suspend fun tombstone(id: String, now: Long = System.currentTimeMillis()) =
            database.withTransaction {
                val subject = dao.getManagedSubject(id) ?: error("Aspect subject does not exist")
                require(subject.subjectType == ManagedSubjectType.ASPECT.name) { "Subject is not an Aspect" }
                if (subject.isDeleted) return@withTransaction
                val live = loadLiveAspects()
                require(id in live) { "Aspect node does not exist" }
                val children = live.values.filter { it.parentAspectId == id }.sortedBy { it.aspectOrder }
                val rootStart = nextOrder(live.values.filterNot { it.subjectId == id }, null)
                val moved =
                    children.mapIndexed { index, child ->
                        child.copy(parentAspectId = null, aspectOrder = rootStart + index)
                    }
                validateHierarchy(live.values.filterNot { it.subjectId == id || it.parentAspectId == id } + moved)
                dao.upsertAspects(moved)
                dao.upsertManagedSubjects(
                    listOf(subject.bump(now).copy(isDeleted = true)) +
                        children.map { requireActiveSubject(it.subjectId).bump(now) },
                )
                dao.upsertAspectOrientationRefs(
                    dao.getAllAspectOrientationRefs()
                        .filter { it.aspectId == id && !it.isDeleted }
                        .map { it.copy(updatedAt = now, syncedAt = null, isDeleted = true, version = it.version + 1L) },
                )
                dao.upsertWorkspaceBindings(
                    dao.getAllWorkspaceBindings()
                        .filter { it.subjectId == id && !it.isDeleted }
                        .map { it.copy(updatedAt = now, syncedAt = null, isDeleted = true, version = it.version + 1L) },
                )
            }

        private suspend fun loadLiveAspects(): Map<String, AspectEntity> {
            val activeIds =
                dao.getAllManagedSubjects()
                    .filter { it.subjectType == ManagedSubjectType.ASPECT.name && !it.isDeleted }
                    .mapTo(hashSetOf()) { it.id }
            return dao.getAllAspects().filter { it.subjectId in activeIds }.associateBy { it.subjectId }
        }

        private suspend fun requireActiveSubject(id: String): ManagedSubjectEntity {
            val subject = requireNotNull(dao.getManagedSubject(id)) { "Aspect subject does not exist" }
            require(subject.subjectType == ManagedSubjectType.ASPECT.name && !subject.isDeleted) {
                "Aspect is not active"
            }
            return subject
        }
    }

private fun validateHierarchy(nodes: Collection<AspectEntity>) {
    require(validateSingleParentHierarchy(nodes.associate { it.subjectId to it.parentAspectId }).isEmpty()) {
        "Aspect hierarchy violates DOMAIN-CONTRACT v1"
    }
}

private fun nextOrder(nodes: Collection<AspectEntity>, parentId: String?): Long =
    (nodes.filter { it.parentAspectId == parentId }.maxOfOrNull { it.aspectOrder } ?: -1L) + 1L

private fun ManagedSubjectEntity.bump(now: Long) = copy(updatedAt = now, syncedAt = null, version = version + 1L)
