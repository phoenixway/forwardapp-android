package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TacticalMissionOrientationResolverTest {
    @Test
    fun `manual mission identity is deterministic and one-to-one`() {
        assertEquals(legacyManualTacticalMissionSubjectId(7L), legacyManualTacticalMissionSubjectId(7L))
        assertNotEquals(legacyManualTacticalMissionSubjectId(7L), legacyManualTacticalMissionSubjectId(8L))

        val result = resolve(mission(id = 7L))
        assertTrue(result is TacticalMissionOrientationResolution.NeedsAdoption)
        result as TacticalMissionOrientationResolution.NeedsAdoption
        assertEquals(TacticalMissionOrientationResolutionCode.MANUAL_ADOPTION_REQUIRED, result.code)
        assertEquals(legacyManualTacticalMissionSubjectId(7L), result.deterministicSubjectId)
    }

    @Test
    fun `already adopted manual mission resolves idempotently`() {
        val subjectId = legacyManualTacticalMissionSubjectId(7L)
        val snapshot =
            snapshot(
                mappings = listOf(mapping(LegacyOrientationSourceType.TACTICAL_MISSION, "7", subjectId)),
                subjects = listOf(subject(subjectId)),
                orientations = listOf(orientation(subjectId, OrientationKind.INTENT)),
            )

        val first = resolve(mission(id = 7L), snapshot)
        val second = resolve(mission(id = 7L), snapshot)

        assertEquals(first, second)
        assertEquals(
            TacticalMissionOrientationResolution.Resolved(
                subjectId,
                TacticalMissionOrientationResolutionPath.LEGACY_MANUAL_MAPPING,
            ),
            first,
        )
    }

    @Test
    fun `manual mapping conflict and deleted target fail closed`() {
        val conflict =
            resolve(
                mission(id = 7L),
                snapshot(mappings = listOf(mapping(LegacyOrientationSourceType.TACTICAL_MISSION, "7", "wrong"))),
            ) as TacticalMissionOrientationResolution.IntegrityFailure
        assertEquals(TacticalMissionOrientationResolutionCode.MAPPING_CONFLICT, conflict.code)

        val subjectId = legacyManualTacticalMissionSubjectId(7L)
        val deleted =
            resolve(
                mission(id = 7L),
                snapshot(
                    mappings = listOf(mapping(LegacyOrientationSourceType.TACTICAL_MISSION, "7", subjectId)),
                    subjects = listOf(subject(subjectId, isDeleted = true)),
                    orientations = listOf(orientation(subjectId, OrientationKind.INTENT)),
                ),
            ) as TacticalMissionOrientationResolution.IntegrityFailure
        assertEquals(TacticalMissionOrientationResolutionCode.TOMBSTONED_ORIENTATION_TARGET, deleted.code)
    }

    @Test
    fun `context backlog mission resolves only an Orientation target`() {
        val mission =
            mission(
                id = 11L,
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry",
            )
        val resolved =
            resolve(
                mission,
                snapshot(
                    backlogEntries = listOf(backlogEntry()),
                    subjects = listOf(subject("orientation")),
                    orientations = listOf(orientation("orientation")),
                ),
            )
        assertEquals(
            TacticalMissionOrientationResolution.Resolved(
                "orientation",
                TacticalMissionOrientationResolutionPath.BACKLOG_ORIENTATION,
            ),
            resolved,
        )

        val needsAdoption =
            resolve(
                mission,
                snapshot(
                    backlogEntries =
                        listOf(
                            backlogEntry(
                                targetKind = WorkspaceBacklogTargetKind.WORKSPACE,
                                targetId = "workspace",
                            ),
                        ),
                ),
            ) as TacticalMissionOrientationResolution.NeedsAdoption
        assertEquals(TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET, needsAdoption.code)
    }

    @Test
    fun `slot backlog mission validates slot owner separately`() {
        val valid =
            mission(
                id = 12L,
                sourceType = MissionSourceType.SLOT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry",
                activitySlotContextId = "owner",
            )
        val canonical =
            snapshot(
                backlogEntries = listOf(backlogEntry()),
                subjects = listOf(subject("orientation")),
                orientations = listOf(orientation("orientation")),
            )
        assertTrue(resolve(valid, canonical) is TacticalMissionOrientationResolution.Resolved)

        val mismatch =
            resolve(valid.copy(activitySlotContextId = "another"), canonical)
                as TacticalMissionOrientationResolution.IntegrityFailure
        assertEquals(TacticalMissionOrientationResolutionCode.SOURCE_OWNER_MISMATCH, mismatch.code)
    }

    @Test
    fun `previous week reuses resolved Orientation and never clones identity`() {
        val source =
            mission(
                id = 21L,
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry",
            )
        val carried = mission(id = 22L, sourceType = MissionSourceType.PREVIOUS_WEEK, carriedFromMissionId = 21L)
        val result =
            resolve(
                carried,
                snapshot(
                    missions = listOf(source, carried),
                    backlogEntries = listOf(backlogEntry()),
                    subjects = listOf(subject("orientation")),
                    orientations = listOf(orientation("orientation")),
                ),
            )

        assertEquals(
            TacticalMissionOrientationResolution.Resolved(
                "orientation",
                TacticalMissionOrientationResolutionPath.PREVIOUS_MISSION,
            ),
            result,
        )
    }

    @Test
    fun `previous week unresolved ancestor remains explicitly unadopted`() {
        val manual = mission(id = 31L)
        val carried = mission(id = 32L, sourceType = MissionSourceType.PREVIOUS_WEEK, carriedFromMissionId = 31L)
        val result = resolve(carried, snapshot(missions = listOf(manual, carried)))

        assertTrue(result is TacticalMissionOrientationResolution.NeedsAdoption)
        result as TacticalMissionOrientationResolution.NeedsAdoption
        assertEquals(TacticalMissionOrientationResolutionCode.PREVIOUS_MISSION_UNRESOLVED, result.code)
        assertEquals(legacyManualTacticalMissionSubjectId(31L), result.deterministicSubjectId)
    }

    @Test
    fun `source-backed and manual ArcQuest resolve canonical targets`() {
        val beaconQuest = arcQuest(id = "arc-beacon", sourceType = ArcQuestSourceType.BEACON, sourceId = "beacon")
        val beaconMission = mission(id = 41L, sourceType = MissionSourceType.ARC_QUEST, sourceArcQuestId = beaconQuest.id)
        val resolved =
            resolve(
                beaconMission,
                snapshot(
                    arcQuests = listOf(beaconQuest),
                    mappings = listOf(mapping(LegacyOrientationSourceType.MAIN_BEACON, "beacon", "orientation")),
                    subjects = listOf(subject("orientation")),
                    orientations = listOf(orientation("orientation", OrientationKind.MAIN_BEACON)),
                ),
            )
        assertEquals(
            TacticalMissionOrientationResolution.Resolved(
                "orientation",
                TacticalMissionOrientationResolutionPath.ARC_SOURCE,
            ),
            resolved,
        )

        val manualQuest = arcQuest(id = "arc-manual", sourceType = ArcQuestSourceType.MANUAL, sourceId = null)
        val manualResolved =
            resolve(
                mission(id = 42L, sourceType = MissionSourceType.ARC_QUEST, sourceArcQuestId = manualQuest.id),
                snapshot(
                    arcQuests = listOf(manualQuest),
                    mappings =
                        listOf(
                            mapping(
                                LegacyOrientationSourceType.ARC_QUEST,
                                manualQuest.id,
                                "quest-orientation",
                            ),
                        ),
                    subjects = listOf(subject("quest-orientation")),
                    orientations = listOf(orientation("quest-orientation", OrientationKind.QUEST)),
                ),
            )
        assertEquals(
            TacticalMissionOrientationResolution.Resolved(
                "quest-orientation",
                TacticalMissionOrientationResolutionPath.ARC_SOURCE,
            ),
            manualResolved,
        )

        val wrongKind =
            resolve(
                mission(id = 43L, sourceType = MissionSourceType.ARC_QUEST, sourceArcQuestId = manualQuest.id),
                snapshot(
                    arcQuests = listOf(manualQuest),
                    mappings =
                        listOf(
                            mapping(
                                LegacyOrientationSourceType.ARC_QUEST,
                                manualQuest.id,
                                "wrong-kind",
                            ),
                        ),
                    subjects = listOf(subject("wrong-kind")),
                    orientations = listOf(orientation("wrong-kind", OrientationKind.GOAL)),
                ),
            ) as TacticalMissionOrientationResolution.IntegrityFailure
        assertEquals(
            TacticalMissionOrientationResolutionCode.INVALID_ORIENTATION_TARGET,
            wrongKind.code,
        )
    }

    private fun resolve(
        mission: TacticalMission,
        snapshot: TacticalMissionOrientationResolutionSnapshot = snapshot(),
    ): TacticalMissionOrientationResolution = resolveTacticalMissionOrientation(mission, snapshot)

    private fun snapshot(
        missions: List<TacticalMission> = emptyList(),
        backlogEntries: List<WorkspaceBacklogEntryEntity> = emptyList(),
        arcQuests: List<ArcQuestEntity> = emptyList(),
        mappings: List<LegacySubjectMappingEntity> = emptyList(),
        subjects: List<ManagedSubjectEntity> = emptyList(),
        orientations: List<OrientationEntity> = emptyList(),
    ) = TacticalMissionOrientationResolutionSnapshot(
        missions = missions,
        backlogEntries = backlogEntries,
        arcQuests = arcQuests,
        mappings = mappings,
        subjects = subjects,
        orientations = orientations,
    )

    private fun mission(
        id: Long,
        sourceType: MissionSourceType = MissionSourceType.MANUAL,
        sourceContextId: String? = null,
        sourceBacklogItemId: String? = null,
        sourceArcQuestId: String? = null,
        carriedFromMissionId: Long? = null,
        activitySlotContextId: String? = null,
    ) = TacticalMission(
        id = id,
        title = "Mission $id",
        description = "Description",
        deadline = NO_DEADLINE,
        projectId = null,
        sourceType = sourceType,
        sourceContextId = sourceContextId,
        sourceBacklogItemId = sourceBacklogItemId,
        sourceArcQuestId = sourceArcQuestId,
        carriedFromMissionId = carriedFromMissionId,
        activitySlotContextId = activitySlotContextId,
    )

    private fun mapping(
        sourceType: LegacyOrientationSourceType,
        sourceId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = subjectId,
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

    private fun subject(
        id: String,
        isDeleted: Boolean = false,
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = ManagedSubjectType.ORIENTATION.name,
        title = id,
        description = null,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
    )

    private fun orientation(
        id: String,
        kind: OrientationKind = OrientationKind.GOAL,
    ) = OrientationEntity(
        subjectId = id,
        kind = kind.name,
        lifecycle = null,
        lifecycleOrigin = ValueOrigin.UNSET.name,
    )

    private fun backlogEntry(
        targetKind: WorkspaceBacklogTargetKind = WorkspaceBacklogTargetKind.ORIENTATION,
        targetId: String = "orientation",
    ) = WorkspaceBacklogEntryEntity(
        id = "entry",
        workspaceId = "owner",
        capabilityInstanceId = "backlog",
        targetKind = targetKind.name,
        targetId = targetId,
        entryOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun arcQuest(
        id: String,
        sourceType: ArcQuestSourceType,
        sourceId: String?,
    ) = ArcQuestEntity(
        id = id,
        arcKey = "2026-Q4",
        title = id,
        sourceType = sourceType.name,
        sourceId = sourceId,
        createdAt = 1L,
        updatedAt = 1L,
        version = 1L,
    )
}
