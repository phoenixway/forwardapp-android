package com.romankozak.forwardappmobile.data.planning.strategic

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolutionSnapshot
import com.romankozak.forwardappmobile.data.planning.tactical.legacyManualTacticalMissionSubjectId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtensionDependency
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalStrategicPlanningMappingTest {
    @Test
    fun `arcKey group maps to stable independent Strategic scope and typed extension`() {
        val quest = manualQuest("quest-1", "2026-10")
        val snapshot = manualSnapshot(quest)

        val first = mapCanonicalStrategicPlanning(listOf(quest), snapshot)
        val second = mapCanonicalStrategicPlanning(listOf(quest), snapshot)

        assertEquals(first, second)
        val scope = first.scopes.single()
        val extension = first.extensions.single()
        assertEquals(PlanningScopeKind.STRATEGIC_ARC, scope.kind)
        assertEquals(PlanningScopeLifecycle.ACTIVE, scope.lifecycle)
        assertNotEquals(quest.arcKey, scope.id.value)
        assertEquals(canonicalStrategicArcScopeId(quest.arcKey), scope.id)
        assertEquals(quest.arcKey, extension.legacyArcKey)
        assertTrue(validateStrategicPlanningScopeExtensionDependency(extension, scope).isEmpty())
    }

    @Test
    fun `multiple exact arcKey groups produce distinct stable scope identities`() {
        val first = manualQuest("quest-1", "Arc")
        val second = manualQuest("quest-2", " arc ")
        val firstId = manualSubjectId(first.id)
        val secondId = manualSubjectId(second.id)
        val result =
            mapCanonicalStrategicPlanning(
                listOf(first, second),
                snapshot(
                    mappings =
                        listOf(
                            mapping(LegacyOrientationSourceType.ARC_QUEST, first.id, firstId),
                            mapping(LegacyOrientationSourceType.ARC_QUEST, second.id, secondId),
                        ),
                    subjects = listOf(subject(firstId), subject(secondId)),
                    orientations =
                        listOf(
                            orientation(firstId, OrientationKind.QUEST),
                            orientation(secondId, OrientationKind.QUEST),
                        ),
                ),
            )

        assertEquals(2, result.scopes.size)
        assertNotEquals(result.scopes[0].id, result.scopes[1].id)
        assertEquals(setOf("Arc", " arc "), result.extensions.map { it.legacyArcKey }.toSet())
    }

    @Test
    fun `blank arcKey and duplicate participation identity fail plan construction`() {
        assertThrows(IllegalArgumentException::class.java) {
            mapCanonicalStrategicPlanning(listOf(manualQuest("blank", " ")), snapshot())
        }
        val duplicate = manualQuest("same", "arc")
        assertThrows(IllegalArgumentException::class.java) {
            mapCanonicalStrategicPlanning(listOf(duplicate, duplicate), snapshot())
        }
    }

    @Test
    fun `manual ArcQuest reuses canonical QUEST as FOCUS commitment`() {
        val quest = manualQuest("quest-1", "2026-10", status = ArcQuestStatus.PAUSED, order = 7L)
        val result = mapCanonicalStrategicPlanning(listOf(quest), manualSnapshot(quest))
        val commitment = result.commitments.single()

        assertEquals(manualSubjectId(quest.id), commitment.orientationId)
        assertEquals(PlanningRoleCode.FOCUS, commitment.role)
        assertEquals(PlanningCommitmentStatus.PAUSED, commitment.status)
        assertEquals(7L, commitment.order)
        assertEquals(quest.id, commitment.provenance.sourceId)
    }

    @Test
    fun `source-backed families reuse existing source Orientations`() {
        val cases =
            listOf(
                ArcQuestSourceType.CONTEXT to LegacyOrientationSourceType.CONTEXT,
                ArcQuestSourceType.BEACON to LegacyOrientationSourceType.MAIN_BEACON,
                ArcQuestSourceType.BEACON_GROUP to LegacyOrientationSourceType.MAIN_BEACON_GROUP,
            )
        cases.forEachIndexed { index, (arcType, legacyType) ->
            val sourceId = "source-$index"
            val orientationId = "orientation-$index"
            val quest = sourceQuest("quest-$index", arcType, sourceId)
            val result =
                mapCanonicalStrategicPlanning(
                    listOf(quest),
                    snapshot(
                        mappings = listOf(mapping(legacyType, sourceId, orientationId)),
                        subjects = listOf(subject(orientationId)),
                        orientations = listOf(orientation(orientationId, OrientationKind.INTENT)),
                    ),
                )
            assertEquals(orientationId, result.commitments.single().orientationId)
        }
    }

    @Test
    fun `same participant identity is stable and different participants are distinct`() {
        val scope = canonicalStrategicArcScopeId("2026-10")
        assertEquals(
            canonicalStrategicFocusCommitmentId(scope, "quest-1"),
            canonicalStrategicFocusCommitmentId(scope, "quest-1"),
        )
        assertNotEquals(
            canonicalStrategicFocusCommitmentId(scope, "quest-1"),
            canonicalStrategicFocusCommitmentId(scope, "quest-2"),
        )
    }

    @Test
    fun `mission-backed ArcQuest reuses the mission semantic Orientation`() {
        val mission =
            TacticalMission(
                id = 71L,
                title = "Mission",
                description = null,
                deadline = NO_DEADLINE,
                projectId = null,
                sourceType = MissionSourceType.MANUAL,
            )
        val orientationId = legacyManualTacticalMissionSubjectId(mission.id)
        val quest = sourceQuest("mission-quest", ArcQuestSourceType.MISSION, mission.id.toString())
        val mappings = listOf(mapping(LegacyOrientationSourceType.TACTICAL_MISSION, "71", orientationId))
        val subjects = listOf(subject(orientationId))
        val orientations = listOf(orientation(orientationId, OrientationKind.INTENT))
        val base = snapshot(mappings, subjects, orientations)
        val result =
            mapCanonicalStrategicPlanning(
                listOf(quest),
                base.copy(
                    tactical =
                        base.tactical.copy(
                            missions = listOf(mission),
                        ),
                ),
            )

        assertEquals(orientationId, result.commitments.single().orientationId)
    }

    @Test
    fun `ArcQuest status maps only to commitment status`() {
        ArcQuestStatus.entries.forEach { status ->
            val quest = manualQuest("quest-${status.name}", "2026-10", status = status)
            val commitment = mapCanonicalStrategicPlanning(listOf(quest), manualSnapshot(quest)).commitments.single()
            val expected =
                when (status) {
                    ArcQuestStatus.ACTIVE -> PlanningCommitmentStatus.ACTIVE
                    ArcQuestStatus.PAUSED -> PlanningCommitmentStatus.PAUSED
                    ArcQuestStatus.DONE -> PlanningCommitmentStatus.COMPLETED
                }
            assertEquals(expected, commitment.status)
        }
    }

    @Test
    fun `missing source authority is deferred while malformed source fails explicitly`() {
        val deferred = sourceQuest("deferred", ArcQuestSourceType.CONTEXT, "context")
        val malformed = deferred.copy(id = "malformed", sourceType = "UNKNOWN")
        val result = mapCanonicalStrategicPlanning(listOf(deferred, malformed), snapshot())

        assertTrue(result.participants[0] is StrategicParticipantMapping.Deferred)
        assertTrue(result.participants[1] is StrategicParticipantMapping.IntegrityFailure)
        assertEquals(0, result.commitments.size)
    }

    @Test
    fun `duplicate live semantic participation fails closed without arbitrary collapse`() {
        val first = sourceQuest("first", ArcQuestSourceType.CONTEXT, "context")
        val second = sourceQuest("second", ArcQuestSourceType.CONTEXT, "context")
        val canonical = "orientation"
        val result =
            mapCanonicalStrategicPlanning(
                listOf(first, second),
                snapshot(
                    mappings = listOf(mapping(LegacyOrientationSourceType.CONTEXT, "context", canonical)),
                    subjects = listOf(subject(canonical)),
                    orientations = listOf(orientation(canonical, OrientationKind.INTENT)),
                ),
            )

        assertEquals(0, result.commitments.size)
        assertEquals(
            2,
            result.participants.filterIsInstance<StrategicParticipantMapping.IntegrityFailure>().size,
        )
    }

    @Test
    fun `manual ArcQuest requires deterministic CUT_OVER QUEST authority`() {
        val quest = manualQuest("quest-1", "2026-10")
        val wrong =
            snapshot(
                mappings = listOf(mapping(LegacyOrientationSourceType.ARC_QUEST, quest.id, "wrong")),
                subjects = listOf(subject("wrong")),
                orientations = listOf(orientation("wrong", OrientationKind.QUEST)),
            )
        val result = mapCanonicalStrategicPlanning(listOf(quest), wrong)

        assertTrue(result.participants.single() is StrategicParticipantMapping.IntegrityFailure)
    }

    @Test
    fun `deleted manual ArcQuest preserves tombstoned CUT_OVER mapping as dropped FOCUS`() {
        val quest =
            manualQuest("deleted-manual", "2026-10").copy(
                isDeleted = true,
            )
        val subjectId = manualSubjectId(quest.id)
        val tombstonedMapping =
            mapping(
                LegacyOrientationSourceType.ARC_QUEST,
                quest.id,
                subjectId,
            ).copy(isDeleted = true)

        val result =
            mapCanonicalStrategicPlanning(
                listOf(quest),
                snapshot(
                    mappings = listOf(tombstonedMapping),
                    subjects = listOf(
                        subject(subjectId).copy(isDeleted = true),
                    ),
                    orientations = listOf(
                        orientation(subjectId, OrientationKind.QUEST),
                    ),
                ),
            )

        assertTrue(result.integrityFailures.isEmpty())
        assertTrue(result.deferred.isEmpty())
        assertEquals(1, result.commitments.size)

        val commitment = result.commitments.single()
        assertEquals(subjectId, commitment.orientationId)
        assertEquals(PlanningRoleCode.FOCUS, commitment.role)
        assertEquals(PlanningCommitmentStatus.DROPPED, commitment.status)
        assertTrue(commitment.isDeleted)

        // Pure mapping must not alter source tombstones.
        assertTrue(quest.isDeleted)
        assertTrue(tombstonedMapping.isDeleted)
        assertEquals(LegacySubjectMappingState.CUT_OVER.name, tombstonedMapping.state)
    }

    @Test
    fun `live manual ArcQuest cannot reuse tombstoned mapping`() {
        val quest = manualQuest("live-manual", "2026-10")
        val subjectId = manualSubjectId(quest.id)

        val result =
            mapCanonicalStrategicPlanning(
                listOf(quest),
                snapshot(
                    mappings = listOf(
                        mapping(
                            LegacyOrientationSourceType.ARC_QUEST,
                            quest.id,
                            subjectId,
                        ).copy(isDeleted = true),
                    ),
                    subjects = listOf(subject(subjectId)),
                    orientations = listOf(
                        orientation(subjectId, OrientationKind.QUEST),
                    ),
                ),
            )

        assertTrue(result.commitments.isEmpty())

        val failure =
            result.participants.single()
                as StrategicParticipantMapping.IntegrityFailure

        assertEquals(
            StrategicParticipantIntegrityCode.MAPPING_NOT_CUT_OVER,
            failure.code,
        )
    }

    @Test
    fun `deleted manual ArcQuest cannot bypass unsupported mapping state`() {
        val quest =
            manualQuest("deleted-invalid", "2026-10").copy(
                isDeleted = true,
            )
        val subjectId = manualSubjectId(quest.id)

        for (mappingDeleted in listOf(false, true)) {
            val result =
                mapCanonicalStrategicPlanning(
                    listOf(quest),
                    snapshot(
                        mappings = listOf(
                            mapping(
                                LegacyOrientationSourceType.ARC_QUEST,
                                quest.id,
                                subjectId,
                            ).copy(
                                state = "INVALID_STATE",
                                isDeleted = mappingDeleted,
                            ),
                        ),
                        subjects = listOf(
                            subject(subjectId).copy(isDeleted = true),
                        ),
                        orientations = listOf(
                            orientation(subjectId, OrientationKind.QUEST),
                        ),
                    ),
                )

            assertTrue(result.commitments.isEmpty())

            val failure =
                result.participants.single()
                    as StrategicParticipantMapping.IntegrityFailure

            assertEquals(
                StrategicParticipantIntegrityCode.MAPPING_NOT_CUT_OVER,
                failure.code,
            )
        }
    }

    private fun manualQuest(
        id: String,
        arcKey: String,
        status: ArcQuestStatus = ArcQuestStatus.ACTIVE,
        order: Long = 0L,
    ) = ArcQuestEntity(
        id = id,
        arcKey = arcKey,
        title = id,
        status = status.name,
        order = order,
        createdAt = 10L,
        updatedAt = 20L,
        version = 2L,
    )

    private fun sourceQuest(
        id: String,
        sourceType: ArcQuestSourceType,
        sourceId: String,
    ) = manualQuest(id, "2026-10").copy(sourceType = sourceType.name, sourceId = sourceId)

    private fun manualSnapshot(quest: ArcQuestEntity): StrategicPlanningResolutionSnapshot {
        val id = manualSubjectId(quest.id)
        return snapshot(
            mappings = listOf(mapping(LegacyOrientationSourceType.ARC_QUEST, quest.id, id)),
            subjects = listOf(subject(id)),
            orientations = listOf(orientation(id, OrientationKind.QUEST)),
        )
    }

    private fun manualSubjectId(questId: String): String =
        LegacySubjectUuid.resolve(LegacySubjectRef(LegacyOrientationSourceType.ARC_QUEST, questId))

    private fun snapshot(
        mappings: List<LegacySubjectMappingEntity> = emptyList(),
        subjects: List<ManagedSubjectEntity> = emptyList(),
        orientations: List<OrientationEntity> = emptyList(),
    ) = StrategicPlanningResolutionSnapshot(
        mappings = mappings,
        subjects = subjects,
        orientations = orientations,
        tactical =
            TacticalMissionOrientationResolutionSnapshot(
                missions = emptyList(),
                backlogEntries = emptyList(),
                arcQuests = emptyList(),
                mappings = mappings,
                subjects = subjects,
                orientations = orientations,
            ),
    )

    private fun mapping(
        sourceType: LegacyOrientationSourceType,
        sourceId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = "$sourceType:$sourceId",
        sourceType = sourceType.name,
        sourceId = sourceId,
        subjectId = subjectId,
        migrationVersion = 1,
        state = LegacySubjectMappingState.CUT_OVER.name,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun subject(id: String) =
        ManagedSubjectEntity(
            id = id,
            subjectType = ManagedSubjectType.ORIENTATION.name,
            title = id,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun orientation(
        id: String,
        kind: OrientationKind,
    ) = OrientationEntity(id, kind.name, null, ValueOrigin.UNSET.name)
}
