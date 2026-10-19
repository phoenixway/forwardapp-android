package com.romankozak.forwardappmobile.data.hierarchy.container

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.hierarchy.GroupScopeCoverageStore
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementGroupScopeMutationCoordinator
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import javax.inject.Inject

/** Reserved, reversible provenance namespace. Container tombstones are the durable cutover receipt. */
internal const val HISTORICAL_GROUP_CONTAINER_PREFIX = "historical-group:v1:"

internal fun historicalGroupContainerId(subjectId: String): String =
    HISTORICAL_GROUP_CONTAINER_PREFIX + subjectId

/**
 * Finite V2 compatibility admission, NOT a semantic reconciler.
 * Only an absent deterministic identity is materialized. An existing identity
 * (live, moved, renamed OR deleted) is always respected, never rebuilt from PART_OF.
 * All structure is authored by H1 and committed with provenance retirement.
 */
class HistoricalGroupContainerCutover @Inject constructor(private val db: AppDatabase) {
    suspend fun materialize(groupSubjectIds: Set<String>? = null) = db.withTransaction {
        if (groupSubjectIds == null && !db.bigPicturePresentationDao().needsHistoricalGroupCutover()) {
            return@withTransaction
        }
        val orientation = db.orientationDao()
        val mappings = orientation.getAllLegacyMappings().filter {
            !it.isDeleted && it.state == "CUT_OVER" && it.sourceType == "MAIN_BEACON_GROUP" &&
                (groupSubjectIds == null || it.subjectId in groupSubjectIds)
        }
        if (mappings.isEmpty()) return@withTransaction
        val existing = db.bigPicturePresentationDao().containers().mapTo(hashSetOf()) { it.record.id }
        val scopes = db.hierarchyPlacementGroupScopeDao().getAll().filter {
            !it.isDeleted && it.groupSubjectId != null &&
                (groupSubjectIds == null || it.groupSubjectId in groupSubjectIds)
        }
        if (scopes.isEmpty() && mappings.all { historicalGroupContainerId(it.subjectId) in existing }) {
            return@withTransaction
        }
        val subjects = orientation.getAllManagedSubjects().associateBy { it.id }
        val groupKinds = orientation.getAllOrientations().filter { it.kind == "MAIN_BEACON_GROUP" }
            .mapTo(hashSetOf()) { it.subjectId }
        val order = db.mainBeaconDao().getAllGroupsSync().associate { it.id to it.order }
        val groups = mappings.filter {
            subjects[it.subjectId]?.let { s -> !s.isDeleted && s.subjectType == "ORIENTATION" } == true &&
                it.subjectId in groupKinds
        }.distinctBy { it.subjectId }.sortedWith(
            compareBy({ order[it.sourceId] ?: Long.MAX_VALUE }, { subjects.getValue(it.subjectId).title.lowercase(java.util.Locale.ROOT) }, { it.subjectId }),
        )
        val groupIds = groups.mapTo(hashSetOf()) { it.subjectId }
        require(scopes.all { it.groupSubjectId in groupIds }) { "Historical container cutover has invalid Group provenance" }
        // Validate the old contract BEFORE turning any active scope into inert history.
        HierarchyPlacementGroupScopeMutationCoordinator(db).validateAuthoritativeState()
        val writer = H1StructuralMutationCoordinator(db)
        val placements = db.hierarchyPlacementDao().getAll().associateBy { it.id }
        var insertionIndex = 0
        groups.forEach { mapping ->
            val group = subjects.getValue(mapping.subjectId)
            val id = historicalGroupContainerId(mapping.subjectId)
            val members = scopes.filter { it.groupSubjectId == mapping.subjectId }
                .sortedWith(compareBy({ placements.getValue(it.placementId).siblingOrder }, { it.placementId }))
            if (id !in existing) {
                // Canonical group creation time is stable across receivers/replay.
                writer.createContainer(group.title, H1StructuralParent.Root, position = insertionIndex++,
                    now = group.createdAt, id = id)
                members.forEach { scope ->
                    writer.moveNode(H1TypedNode.Occurrence(scope.placementId),
                        H1StructuralParent.Node(H1TypedNode.Container(id)),
                        now = Math.addExact(maxOf(scope.updatedAt, group.createdAt), 1))
                }
            }
            // A replayed old scope must not move a user-edited occurrence or resurrect a container.
            members.forEach { scope ->
                HierarchyPlacementGroupScopeMutationCoordinator(db).retireScope(
                    PlacementId(scope.placementId), Math.addExact(scope.updatedAt, 1))
            }
        }
        BigPicturePresentationRepository(db).requireCurrentUnifiedStructuralGraphValid()
        GroupScopeCoverageStore(db).validateCurrent()
    }
}
