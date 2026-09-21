package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class CanonicalV1HierarchyMaterializationOutcome {
    CREATED,
    EXACT_RERUN,
}

data class CanonicalV1HierarchyMaterializationReport(
    val outcome: CanonicalV1HierarchyMaterializationOutcome,
    val occurrenceCount: Int,
    val rootCount: Int,
    val primaryCount: Int,
    val linkCount: Int,
    val ambiguousPrimaryTargetCount: Int,
)


internal fun CanonicalV1HierarchySnapshot.toDeterministicHierarchyPlacements(
    now: Long,
): List<HierarchyPlacement> =
    occurrences.map { occurrence ->
        HierarchyPlacement(
            id =
                CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                    hierarchyId = hierarchyId.value,
                    occurrenceKey = occurrence.occurrenceKey,
                ),
            hierarchyId = hierarchyId,
            target = occurrence.target,
            parentPlacementId =
                occurrence.parentOccurrenceKey?.let { parentKey ->
                    CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                        hierarchyId = hierarchyId.value,
                        occurrenceKey = parentKey,
                    )
                },
            placementKind = occurrence.placementKind,
            siblingOrder = occurrence.siblingOrder,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
    }

class CanonicalV1HierarchyMaterializationConflictException(
    message: String,
) : IllegalStateException(message)

/**
 * Atomic one-shot H2 persistence boundary.
 *
 * H2 is not an ongoing V1/V2 reconciler. The transaction accepts only a
 * pristine V2 placement table or an exact structural rerun of the same
 * deterministic occurrence materialization.
 */
@Singleton
class CanonicalV1HierarchyMaterializer
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun materialize(
            snapshot: CanonicalV1HierarchySnapshot,
            now: Long = System.currentTimeMillis(),
        ): CanonicalV1HierarchyMaterializationReport =
            database.withTransaction {
                materializeInCurrentTransaction(snapshot, now)
            }

        internal suspend fun materializeInCurrentTransaction(
            snapshot: CanonicalV1HierarchySnapshot,
            now: Long,
        ): CanonicalV1HierarchyMaterializationReport {
            validateSnapshotShape(snapshot)

            val desired = snapshot.toDeterministicHierarchyPlacements(now)
            val desiredGroupScopes =
                snapshot.toDeterministicHierarchyPlacementGroupScopes(now)
            val desiredLinkedAppearances =
                snapshot.toDeterministicHierarchyPlacementLinkedAppearances(now)

            database.requireValidProspectiveHierarchy(desired)
            validateCanonicalGroupScopeSubjects(desiredGroupScopes)

            val dao = database.hierarchyPlacementDao()
            val groupScopeDao = database.hierarchyPlacementGroupScopeDao()
            val linkedAppearanceDao = database.hierarchyPlacementLinkedAppearanceDao()
            val existing =
                dao.getAll()
                    .map { it.toHierarchyPlacementStrict() }
            val existingGroupScopes = groupScopeDao.getAll()
            val existingLinkedAppearances = linkedAppearanceDao.getAll()

            val outcome =
                when {
                    existing.isEmpty() -> {
                        if (desired.isNotEmpty()) {
                            dao.upsertAll(desired.map { it.toHierarchyPlacementEntity() })
                        }
                        CanonicalV1HierarchyMaterializationOutcome.CREATED
                    }

                    exactStructuralMatch(existing, desired) ->
                        CanonicalV1HierarchyMaterializationOutcome.EXACT_RERUN

                    else ->
                        throw CanonicalV1HierarchyMaterializationConflictException(
                            buildConflictMessage(existing, desired),
                        )
                }

            when {
                existingGroupScopes.isEmpty() -> {
                    /*
                     * v176 additive upgrade: an already exact deterministic H1
                     * graph may receive missing occurrence-scoped Group
                     * provenance on an explicit one-shot H2 rerun.
                     */
                    if (desiredGroupScopes.isNotEmpty()) {
                        groupScopeDao.upsertAll(desiredGroupScopes)
                    }
                }

                exactDeterministicHierarchyGroupScopeMatch(
                    existing = existingGroupScopes,
                    desired = desiredGroupScopes,
                ) -> Unit

                else ->
                    throw CanonicalV1HierarchyMaterializationConflictException(
                        buildGroupScopeConflictMessage(
                            existing = existingGroupScopes,
                            desired = desiredGroupScopes,
                        ),
                    )
            }

            when {
                existingLinkedAppearances.isEmpty() -> {
                    /*
                     * v177 additive upgrade: an already exact deterministic H1
                     * graph may receive exact linked-presentation provenance
                     * from preserved H2 source-authority evidence.
                     */
                    if (desiredLinkedAppearances.isNotEmpty()) {
                        linkedAppearanceDao.upsertAll(desiredLinkedAppearances)
                    }
                }

                exactDeterministicHierarchyLinkedAppearanceMatch(
                    existing = existingLinkedAppearances,
                    desired = desiredLinkedAppearances,
                ) -> Unit

                else ->
                    throw CanonicalV1HierarchyMaterializationConflictException(
                        buildLinkedAppearanceConflictMessage(
                            existing = existingLinkedAppearances,
                            desired = desiredLinkedAppearances,
                        ),
                    )
            }

            return CanonicalV1HierarchyMaterializationReport(
                outcome = outcome,
                occurrenceCount = snapshot.occurrences.size,
                rootCount = snapshot.occurrences.count { it.parentOccurrenceKey == null },
                primaryCount =
                    snapshot.occurrences.count {
                        it.placementKind == PlacementKind.PRIMARY
                    },
                linkCount =
                    snapshot.occurrences.count {
                        it.placementKind == PlacementKind.LINK
                    },
                ambiguousPrimaryTargetCount =
                    snapshot.diagnostics.count {
                        it.code ==
                            CanonicalV1HierarchyDiagnosticCode.AMBIGUOUS_PRIMARY_EVIDENCE
                    },
            )
        }

        private fun validateSnapshotShape(
            snapshot: CanonicalV1HierarchySnapshot,
        ) {
            val byKey = snapshot.occurrences.associateBy { it.occurrenceKey }
            require(byKey.size == snapshot.occurrences.size) {
                "CanonicalV1HierarchySnapshot contains duplicate occurrenceKey values"
            }

            snapshot.occurrences.forEach { occurrence ->
                require(occurrence.occurrenceKey.isNotBlank()) {
                    "CanonicalV1HierarchySnapshot occurrenceKey must not be blank"
                }
                occurrence.parentOccurrenceKey?.let { parentKey ->
                    require(parentKey in byKey) {
                        "Snapshot occurrence ${occurrence.occurrenceKey} references missing parent $parentKey"
                    }
                    require(parentKey != occurrence.occurrenceKey) {
                        "Snapshot occurrence ${occurrence.occurrenceKey} cannot parent itself"
                    }
                }
            }

            snapshot.occurrences
                .groupBy { it.parentOccurrenceKey }
                .forEach { (parentKey, siblings) ->
                    val orders = siblings.map { it.siblingOrder }
                    require(orders.distinct().size == orders.size) {
                        "Snapshot siblings under $parentKey contain duplicate siblingOrder values"
                    }
                    require(orders.sorted() == orders.indices.map(Int::toLong)) {
                        "Snapshot siblings under $parentKey must use dense deterministic siblingOrder values"
                    }
                }

        }

        private suspend fun validateCanonicalGroupScopeSubjects(
            scopes: List<com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity>,
        ) {
            val groupSubjectIds =
                scopes.asSequence()
                    .filterNot { it.isDeleted }
                    .mapNotNull { it.groupSubjectId }
                    .toSet()
            if (groupSubjectIds.isEmpty()) return

            val orientationDao = database.orientationDao()
            val subjectsById =
                orientationDao.getAllManagedSubjects()
                    .associateBy { it.id }
            val orientationsBySubjectId =
                orientationDao.getAllOrientations()
                    .associateBy { it.subjectId }
            val groupMappingsBySubjectId =
                orientationDao.getAllLegacyMappings()
                    .asSequence()
                    .filter {
                        !it.isDeleted &&
                            it.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name &&
                            it.state == LegacySubjectMappingState.CUT_OVER.name
                    }.associateBy { it.subjectId }

            groupSubjectIds.forEach { subjectId ->
                val subject =
                    requireNotNull(subjectsById[subjectId]) {
                        "Group scope references missing canonical subject $subjectId"
                    }
                require(
                    !subject.isDeleted &&
                        subject.subjectType == ManagedSubjectType.ORIENTATION.name,
                ) {
                    "Group scope subject $subjectId is not a live ORIENTATION ManagedSubject"
                }

                val orientation =
                    requireNotNull(orientationsBySubjectId[subjectId]) {
                        "Group scope subject $subjectId has no Orientation row"
                    }
                require(orientation.kind == OrientationKind.MAIN_BEACON_GROUP.name) {
                    "Group scope subject $subjectId is not MAIN_BEACON_GROUP"
                }

                requireNotNull(groupMappingsBySubjectId[subjectId]) {
                    "Group scope subject $subjectId has no live CUT_OVER MAIN_BEACON_GROUP mapping"
                }
            }
        }

        private fun exactStructuralMatch(
            existing: List<HierarchyPlacement>,
            desired: List<HierarchyPlacement>,
        ): Boolean {
            if (existing.size != desired.size) return false
            val existingById = existing.associateBy { it.id }

            return desired.all { wanted ->
                val current = existingById[wanted.id] ?: return@all false
                !current.isDeleted &&
                    current.hierarchyId == wanted.hierarchyId &&
                    current.target == wanted.target &&
                    current.parentPlacementId == wanted.parentPlacementId &&
                    current.placementKind == wanted.placementKind &&
                    current.siblingOrder == wanted.siblingOrder
            }
        }

        private fun buildConflictMessage(
            existing: List<HierarchyPlacement>,
            desired: List<HierarchyPlacement>,
        ): String {
            val existingIds = existing.mapTo(sortedSetOf()) { it.id.value }
            val desiredIds = desired.mapTo(sortedSetOf()) { it.id.value }
            val extra = (existingIds - desiredIds).take(5)
            val missing = (desiredIds - existingIds).take(5)

            return buildString {
                append("H2 requires a pristine placement table or an exact deterministic rerun; ")
                append("existing=${existing.size}, desired=${desired.size}")
                if (extra.isNotEmpty()) append(", extraIds=$extra")
                if (missing.isNotEmpty()) append(", missingIds=$missing")
                if (extra.isEmpty() && missing.isEmpty()) {
                    append(", deterministic ids exist with different structural state")
                }
            }
        }

        companion object {
            private val PLACEMENT_NAMESPACE: UUID =
                LegacySubjectUuid.uuidV5(
                    UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
                    "HIERARCHY_PLACEMENT_V2",
                )

            internal fun deterministicPlacementId(
                hierarchyId: String,
                occurrenceKey: String,
            ): PlacementId {
                require(hierarchyId.isNotBlank()) {
                    "hierarchyId must not be blank"
                }
                require(occurrenceKey.isNotBlank()) {
                    "occurrenceKey must not be blank"
                }

                return PlacementId(
                    LegacySubjectUuid.uuidV5(
                        PLACEMENT_NAMESPACE,
                        "$hierarchyId\u0000$occurrenceKey",
                    ).toString(),
                )
            }
        }
    }
