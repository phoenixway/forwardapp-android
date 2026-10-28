package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolutionSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V3.6d runtime authority adapter for Strategic FOCUS participation.
 *
 * PlanningScope + StrategicPlanningScopeExtension own Strategic scopes,
 * PlanningCommitment owns eligible participation, and Orientation owns common
 * semantics. ArcQuest remains the carrier for source/specialized fields and
 * for explicitly deferred participants only.
 */
@Singleton
class CanonicalStrategicPlanningAdapter
    @Inject
    internal constructor(
        private val database: AppDatabase,
        private val authorityActivator: CanonicalStrategicPlanningAuthorityActivator,
        private val planningRepository: CanonicalPlanningRepository,
        private val orientationRepository: CanonicalOrientationRepository,
        private val questOrientationBridge: QuestOrientationBridge,
    ) {
        fun observeArcKeys(): Flow<List<String>> =
            flow {
                authorityActivator.requireActivated()
                emitAll(
                    database.invalidationTracker
                        .createFlow(
                            "planning_scopes",
                            "strategic_planning_scope_extensions",
                            emitInitialState = true,
                        ).map {
                            database.withTransaction {
                                val scopes =
                                    database.planningDao().getAllScopes()
                                        .map { it.toPlanningScopeStrict() }
                                        .filter { it.kind == PlanningScopeKind.STRATEGIC_ARC }
                                val extensions =
                                    database.strategicPlanningScopeExtensionDao().getAll()
                                        .map { it.toDomainStrict() }
                                require(extensions.map { it.scopeId }.distinct().size == extensions.size)
                                val extensionsByScope = extensions.associateBy { it.scopeId }

                                scopes.filterNot(PlanningScope::isDeleted).map { scope ->
                                    val extension =
                                        requireNotNull(extensionsByScope[scope.id]) {
                                            "Canonical Strategic scope ${scope.id.value} is missing its extension"
                                        }
                                    require(!extension.isDeleted) {
                                        "Live Strategic scope ${scope.id.value} has a tombstoned extension"
                                    }
                                    extension.legacyArcKey
                                }.sorted()
                            }
                        }.distinctUntilChanged().flowOn(Dispatchers.Default),
                )
            }

        fun observeArcQuests(arcKey: String): Flow<List<ArcQuestEntity>> =
            flow {
                authorityActivator.requireActivated()
                emitAll(
                    database.invalidationTracker
                        .createFlow(
                            "arc_quests",
                            "planning_commitments",
                            "managed_subjects",
                            "planning_scopes",
                            "strategic_planning_scope_extensions",
                            "legacy_subject_mappings",
                            "orientations",
                            "tactical_missions",
                            "workspace_backlog_entries",
                            emitInitialState = true,
                        ).map {
                            val frame = database.withTransaction { StrategicQuestReadFrame.load(database) }
                            projectArc(
                                arcKey, frame.rows, frame.commitments,
                                frame.resolution.subjects.associateBy { it.id }, frame.scopes,
                                frame.extensions, frame.resolution,
                            )
                        }.distinctUntilChanged().flowOn(Dispatchers.Default),
                )
            }

        suspend fun getById(id: String): ArcQuestEntity? {
            authorityActivator.requireActivated()
            val row = database.arcQuestDao().getById(id) ?: return null
            return projectArc(
                arcKey = row.arcKey,
                rows = database.arcQuestDao().getAllSync(),
                commitments = allCommitments(),
                subjects = database.orientationDao().getAllManagedSubjects().associateBy(ManagedSubjectEntity::id),
                scopes = database.planningDao().getAllScopes().map { it.toPlanningScopeStrict() },
                extensions = database.strategicPlanningScopeExtensionDao().getAll().map { it.toDomainStrict() },
                resolutionSnapshot = loadResolutionSnapshot(),
            ).singleOrNull { it.id == id }
        }

        suspend fun addQuest(
            quest: ArcQuestEntity,
            source: com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget? = null,
        ) {
            authorityActivator.requireActivated()
            require(quest.arcKey.isNotBlank()) { "Strategic arcKey must not be blank" }
            val now = System.currentTimeMillis()
            database.withTransaction {
                require(database.arcQuestDao().getById(quest.id) == null) {
                    "ArcQuest ${quest.id} already exists"
                }
                com.romankozak.forwardappmobile.StartupTrace.mark("Planning.Quest.transaction.entered")
                val sourceQuest = if (source?.kind == com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind.ORIENTATION) {
                    quest.copy(sourceType = ArcQuestSourceType.ORIENTATION.name, sourceId = source.id)
                } else quest
                val stored =
                    sourceQuest.copy(
                        order = (database.arcQuestDao().minimumCanonicalOrder(canonicalStrategicArcScopeId(quest.arcKey).value, quest.arcKey) ?: 0L) - 1L,
                        updatedAt = now,
                        syncedAt = null,
                        version = 1L,
                    )
                database.arcQuestDao().insert(stored)
                questOrientationBridge.writeCommon(previous = null, desired = stored, now = now)

                val snapshot = loadResolutionSnapshot()
                ensureScope(stored, snapshot)
                when (val result = mapStrategicParticipant(stored, snapshot)) {
                    is StrategicParticipantMapping.Eligible ->
                        planningRepository.saveCommitment(result.commitment)
                    is StrategicParticipantMapping.Deferred -> Unit
                    is StrategicParticipantMapping.IntegrityFailure ->
                        error("ArcQuest ${stored.id} cannot be admitted: ${result.code}: ${result.detail}")
                }
                source?.let {
                    com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository(database).attach(
                        com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                            com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.ARC_QUEST,
                            stored.id,
                        ), it, com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceRole.SOURCE, now,
                    )
                }
            }
        }

        suspend fun carryQuest(questId: String, targetArcKey: String, preserveOutgoing: Boolean = true): StrategicQuestCarryForwardResult {
            authorityActivator.requireActivated()
            return StrategicQuestCarryForward(database, this, orientationRepository).carry(questId, targetArcKey, preserveOutgoing)
        }

        suspend fun updateQuest(quest: ArcQuestEntity) {
            authorityActivator.requireActivated()
            val now = System.currentTimeMillis()
            database.withTransaction {
                val previous =
                    requireNotNull(database.arcQuestDao().getById(quest.id)) {
                        "ArcQuest ${quest.id} does not exist"
                    }
                require(previous.arcKey == quest.arcKey) { "ArcQuest arcKey is immutable after Strategic cutover" }
                require(previous.sourceType == quest.sourceType && previous.sourceId == quest.sourceId) {
                    "ArcQuest semantic source is immutable after Strategic cutover"
                }
                val current = strategicCommitment(previous.id)
                if (current == null) {
                    requireDeferred(previous)
                    requireDeferred(quest)
                    database.arcQuestDao().update(
                        quest.copy(updatedAt = now, syncedAt = null, version = previous.version + 1L),
                    )
                    return@withTransaction
                }
                require(!current.isDeleted) { "Deleted Strategic participation ${quest.id} cannot be updated" }

                orientationRepository.updateOrientationSemantics(
                    subjectId = current.orientationId,
                    title = quest.title,
                    description = quest.description,
                    now = now,
                )

                val candidate = requireEligible(quest)
                val desired =
                    current.copy(
                        order = candidate.order,
                        status = candidate.status,
                        updatedAt = now,
                        syncedAt = null,
                        version = current.version + 1L,
                    )
                if (desired.commonRuntimeFacts() != current.commonRuntimeFacts()) {
                    planningRepository.saveCommitment(desired)
                }

                val subject = requireCanonicalSubject(current.orientationId)
                database.arcQuestDao().update(
                    projectCanonical(
                        row = quest.copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = previous.version + 1L,
                        ),
                        commitment = if (desired.commonRuntimeFacts() == current.commonRuntimeFacts()) current else desired,
                        subject = subject,
                    ),
                )
            }
        }

        suspend fun deleteQuest(quest: ArcQuestEntity) {
            authorityActivator.requireActivated()
            val now = System.currentTimeMillis()
            database.withTransaction {
                val row =
                    requireNotNull(database.arcQuestDao().getById(quest.id)) {
                        "ArcQuest ${quest.id} does not exist"
                    }
                com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository(database).retireOwner(
                    com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                        com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.ARC_QUEST, row.id), now)
                val commitment = strategicCommitment(row.id)
                if (commitment == null) {
                    requireDeferred(row)
                    database.arcQuestDao().update(
                        row.copy(isDeleted = true, updatedAt = now, syncedAt = null, version = row.version + 1L),
                    )
                } else {
                    val tombstone = planningRepository.tombstoneCommitment(commitment.id, now)
                    val subject = requireCanonicalSubject(tombstone.orientationId)
                    database.arcQuestDao().update(
                        projectCanonical(
                            row = row.copy(updatedAt = now, syncedAt = null, version = row.version + 1L),
                            commitment = tombstone,
                            subject = subject,
                        ),
                    )
                }
            }
        }

        suspend fun reorder(quests: List<ArcQuestEntity>) {
            quests.forEachIndexed { index, quest ->
                val current = requireNotNull(getById(quest.id)) { "ArcQuest ${quest.id} does not exist" }
                if (current.order != index.toLong()) updateQuest(current.copy(order = index.toLong()))
            }
        }

        /** Refreshes subordinate legacy carriers after canonical Restore/merge ingress. */
        suspend fun projectCompatibilityRows() {
            authorityActivator.requireActivated()
            database.withTransaction {
                val rows = database.arcQuestDao().getAllSync().associateBy(ArcQuestEntity::id)
                allCommitments()
                    .filter { it.role == PlanningRoleCode.FOCUS && it.provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE }
                    .forEach { commitment ->
                        val sourceId = requireNotNull(commitment.provenance.sourceId)
                        val row = requireNotNull(rows[sourceId]) {
                            "Canonical Strategic FOCUS ${commitment.id.value} is missing ArcQuest carrier"
                        }
                        val subject =
                            requireCanonicalSubject(
                                commitment.orientationId,
                                allowDeleted = commitment.isDeleted,
                            )
                        val projected = projectCanonical(row, commitment, subject)
                        database.arcQuestDao().projectCanonicalFields(
                            questId = row.id,
                            title = projected.title,
                            description = projected.description,
                            status = projected.status,
                            order = projected.order,
                            isDeleted = projected.isDeleted,
                        )
                    }
            }
        }

        private suspend fun projectArc(
            arcKey: String,
            rows: List<ArcQuestEntity>,
            commitments: List<PlanningCommitment>,
            subjects: Map<String, ManagedSubjectEntity>,
            scopes: List<PlanningScope>,
            extensions: List<com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension>,
            resolutionSnapshot: StrategicPlanningResolutionSnapshot,
        ): List<ArcQuestEntity> {
            val scopeId = canonicalStrategicArcScopeId(arcKey)
            val arcRows = rows.filter { it.arcKey == arcKey }
            val scope = scopes.singleOrNull { it.id == scopeId }
            if (scope == null) {
                require(arcRows.isEmpty()) { "Strategic arc $arcKey is missing canonical scope ${scopeId.value}" }
                return emptyList()
            }
            require(scope.kind == PlanningScopeKind.STRATEGIC_ARC && !scope.isDeleted)
            val extension = extensions.singleOrNull { it.scopeId == scopeId }
            require(extension != null && !extension.isDeleted && extension.legacyArcKey == arcKey) {
                "Strategic arc $arcKey has missing or divergent canonical extension"
            }

            val owned = commitments.filter {
                it.scopeId == scopeId && it.role == PlanningRoleCode.FOCUS &&
                    it.provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE
            }
            val bySource = owned.groupBy { it.provenance.sourceId }
            val projected = buildList {
                arcRows.forEach { row ->
                    val records = bySource[row.id].orEmpty()
                    require(records.size <= 1) { "ArcQuest ${row.id} has competing canonical FOCUS records" }
                    val commitment = records.singleOrNull()
                    if (commitment != null) {
                        require(
                            commitment.id == canonicalStrategicFocusCommitmentId(scopeId, row.id),
                        ) { "ArcQuest ${row.id} has non-deterministic canonical commitment identity" }
                        if (!commitment.isDeleted) {
                            add(projectCanonical(row, commitment, requireCanonicalSubject(commitment.orientationId, subjects)))
                        }
                    } else if (!row.isDeleted) {
                        when (val result = mapStrategicParticipant(row, resolutionSnapshot)) {
                            is StrategicParticipantMapping.Deferred -> add(row)
                            is StrategicParticipantMapping.Eligible ->
                                error("Eligible ArcQuest ${row.id} is missing canonical FOCUS participation")
                            is StrategicParticipantMapping.IntegrityFailure ->
                                error("ArcQuest ${row.id} integrity failure: ${result.code}: ${result.detail}")
                        }
                    }
                }
            }
            val rowIds = arcRows.map(ArcQuestEntity::id).toSet()
            val orphan = owned.firstOrNull { it.provenance.sourceId !in rowIds }
            require(orphan == null) {
                "Canonical Strategic FOCUS ${orphan?.id?.value} is missing ArcQuest compatibility carrier"
            }
            return projected.sortedWith(compareBy<ArcQuestEntity> { it.order }.thenBy { it.id })
        }

        private suspend fun ensureScope(
            quest: ArcQuestEntity,
            snapshot: StrategicPlanningResolutionSnapshot,
        ) {
            val scopeId = canonicalStrategicArcScopeId(quest.arcKey)
            val existing = database.planningDao().getScope(scopeId.value)?.toPlanningScopeStrict()
            if (existing != null) {
                require(existing.kind == PlanningScopeKind.STRATEGIC_ARC && !existing.isDeleted)
                val extension =
                    requireNotNull(database.strategicPlanningScopeExtensionDao().get(scopeId.value)?.toDomainStrict())
                require(!extension.isDeleted && extension.legacyArcKey == quest.arcKey)
                return
            }
            val plan = mapCanonicalStrategicPlanning(listOf(quest), snapshot)
            require(plan.integrityFailures.isEmpty())
            database.planningDao().upsertScope(plan.scopes.single().toPlanningScopeEntity())
            database.strategicPlanningScopeExtensionDao().upsert(plan.extensions.single().toEntity())
        }

        private suspend fun strategicCommitment(arcQuestId: String): PlanningCommitment? {
            val id = canonicalStrategicFocusCommitmentId(
                canonicalStrategicArcScopeId(
                    requireNotNull(database.arcQuestDao().getById(arcQuestId)).arcKey,
                ),
                arcQuestId,
            )
            return planningRepository.getCommitment(id)
        }

        private suspend fun requireEligible(quest: ArcQuestEntity): PlanningCommitment =
            when (val result = mapStrategicParticipant(quest, loadResolutionSnapshot())) {
                is StrategicParticipantMapping.Eligible -> result.commitment
                is StrategicParticipantMapping.Deferred -> error("ArcQuest ${quest.id} is deferred: ${result.code}")
                is StrategicParticipantMapping.IntegrityFailure ->
                    error("ArcQuest ${quest.id} integrity failure: ${result.code}: ${result.detail}")
            }

        private suspend fun requireDeferred(quest: ArcQuestEntity) {
            when (val result = mapStrategicParticipant(quest, loadResolutionSnapshot())) {
                is StrategicParticipantMapping.Deferred -> Unit
                is StrategicParticipantMapping.Eligible ->
                    error("Eligible ArcQuest ${quest.id} is missing canonical FOCUS participation")
                is StrategicParticipantMapping.IntegrityFailure ->
                    error("ArcQuest ${quest.id} integrity failure: ${result.code}: ${result.detail}")
            }
        }

        private suspend fun loadResolutionSnapshot(
            arcQuests: List<ArcQuestEntity>? = null,
        ): StrategicPlanningResolutionSnapshot {
            val resolvedArcQuests = arcQuests ?: database.arcQuestDao().getAllSync()
            val mappings = database.orientationDao().getAllLegacyMappings()
            val subjects = database.orientationDao().getAllManagedSubjects()
            val orientations = database.orientationDao().getAllOrientations()
            return StrategicPlanningResolutionSnapshot(
                mappings = mappings,
                subjects = subjects,
                orientations = orientations,
                tactical =
                    TacticalMissionOrientationResolutionSnapshot(
                        missions = database.tacticalMissionDao().getAllMissionsSync(),
                        backlogEntries = database.workspaceBacklogEntryDao().getAll(),
                        arcQuests = resolvedArcQuests,
                        mappings = mappings,
                        subjects = subjects,
                        orientations = orientations,
                    ),
            )
        }

        private suspend fun allCommitments(): List<PlanningCommitment> =
            database.planningDao().getAllCommitments().map {
                it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
            }

        private suspend fun requireCanonicalSubject(
            orientationId: String,
            cached: Map<String, ManagedSubjectEntity>? = null,
            allowDeleted: Boolean = false,
        ): ManagedSubjectEntity {
            val subject = if (cached != null) cached[orientationId] else database.orientationDao().getManagedSubject(orientationId)
            require(
                subject != null &&
                    subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                    (allowDeleted || !subject.isDeleted)
            ) {
                "Strategic FOCUS target $orientationId is not a live canonical Orientation"
            }
            return subject
        }
    }

private data class CommitmentRuntimeFacts(
    val order: Long,
    val status: PlanningCommitmentStatus,
)

private fun PlanningCommitment.commonRuntimeFacts() = CommitmentRuntimeFacts(order, status)

private fun projectCanonical(
    row: ArcQuestEntity,
    commitment: PlanningCommitment,
    subject: ManagedSubjectEntity,
): ArcQuestEntity {
    require(commitment.role == PlanningRoleCode.FOCUS)
    require(commitment.provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE)
    require(commitment.provenance.sourceId == row.id)
    require(commitment.isDeleted || commitment.status != PlanningCommitmentStatus.DROPPED) {
        "Live Strategic FOCUS ${commitment.id.value} cannot have DROPPED status"
    }
    return row.copy(
        title = subject.title,
        description = subject.description,
        status = commitment.status.toArcQuestStatus(row.status),
        order = commitment.order,
        isDeleted = commitment.isDeleted,
    )
}

private fun PlanningCommitmentStatus.toArcQuestStatus(current: String): String =
    when (this) {
        PlanningCommitmentStatus.PLANNED ->
            error("Strategic FOCUS does not support PLANNED status")
        PlanningCommitmentStatus.ACTIVE -> ArcQuestStatus.ACTIVE.name
        PlanningCommitmentStatus.PAUSED -> ArcQuestStatus.PAUSED.name
        PlanningCommitmentStatus.COMPLETED -> ArcQuestStatus.DONE.name
        PlanningCommitmentStatus.DROPPED -> current
    }
