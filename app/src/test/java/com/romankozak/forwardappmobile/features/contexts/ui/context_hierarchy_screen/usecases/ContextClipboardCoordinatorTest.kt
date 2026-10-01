package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ContextParentPlanWriter
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyOccurrenceWriteResult
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyOccurrenceWriter
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyActionPlan
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommandService
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceScopeOperand
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyStructuralOperand
import com.romankozak.forwardappmobile.data.hierarchy.LegacyBeaconHierarchyTargetResolver
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextClipboardCoordinatorTest {
    private val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)
    private val hierarchyOccurrenceCommandService = mockk<HierarchyOccurrenceCommandService>()
    private val contextParentPlanWriter = mockk<CanonicalV2ContextParentPlanWriter>(relaxed = true)
    private val beaconOccurrenceWriter = mockk<CanonicalV2HierarchyOccurrenceWriter>()
    private val beaconTargetResolver = mockk<LegacyBeaconHierarchyTargetResolver>()
    private val orientationDao = mockk<OrientationDao>()

    private val coordinator =
        ContextClipboardCoordinator(
            mainBeaconRepository = mainBeaconRepository,
            hierarchyOccurrenceCommandService = hierarchyOccurrenceCommandService,
            contextParentPlanWriter = contextParentPlanWriter,
            beaconOccurrenceWriter = beaconOccurrenceWriter,
            beaconTargetResolver = beaconTargetResolver,
            orientationDao = orientationDao,
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `Beacon payload rejects missing exact occurrence`() {
        val result = coordinator.cutBeacon("legacy-beacon", occurrence = null)

        assertEquals("Неможливо вибрати орієнтир: відсутня точна occurrence", result)
        assertFalse(coordinator.hasBeaconPayload.value)
    }

    @Test
    fun `Beacon payload rejects non managed-subject occurrence`() {
        val result =
            coordinator.cutBeacon(
                "legacy-beacon",
                occurrence(
                    placementId = "workspace-occ",
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"),
                    kind = PlacementKind.LINK,
                ),
            )

        assertEquals("Неможливо вибрати орієнтир: відсутня точна occurrence", result)
        assertFalse(coordinator.hasBeaconPayload.value)
    }

    @Test
    fun `Beacon canonical target mismatch fails closed and retains CUT payload`() =
        runTest {
            val selected =
                occurrence(
                    placementId = "selected-link",
                    target = managedTarget("selected-subject"),
                    kind = PlacementKind.LINK,
                )
            coordinator.cutBeacon("legacy-source", selected)
            coEvery { beaconTargetResolver.resolve("legacy-source") } returns managedTarget("different-subject")

            val result =
                coordinator.pasteBeaconIntoBeacon(
                    targetBeaconId = "legacy-destination",
                    destinationOccurrence =
                        occurrence(
                            placementId = "destination",
                            target = managedTarget("destination-subject"),
                            kind = PlacementKind.PRIMARY,
                        ),
                )

            assertTrue(result.toast.contains("does not match its canonical target"))
            assertTrue(coordinator.hasBeaconPayload.value)
            coVerify(exactly = 0) { beaconOccurrenceWriter.executeBeaconPlan(any(), any()) }
        }

    @Test
    fun `paste Beacon into Beacon requires exact destination occurrence and retains CUT payload`() =
        runTest {
            val selected =
                occurrence(
                    placementId = "selected-link",
                    target = managedTarget("source-subject"),
                    kind = PlacementKind.LINK,
                )
            coordinator.cutBeacon("legacy-source", selected)

            val result =
                coordinator.pasteBeaconIntoBeacon(
                    targetBeaconId = "legacy-destination",
                    destinationOccurrence = null,
                )

            assertEquals("Відсутня точна occurrence цільового орієнтира", result.toast)
            assertTrue(coordinator.hasBeaconPayload.value)
            coVerify(exactly = 0) { beaconOccurrenceWriter.executeBeaconPlan(any(), any()) }
        }

    @Test
    fun `Beacon selected LINK CUT reaches V2 writer unchanged and clears payload on success`() =
        runTest {
            val sourceTarget = managedTarget("source-subject")
            val destinationTarget = managedTarget("destination-subject")
            val selected =
                occurrence(
                    placementId = "selected-link",
                    target = sourceTarget,
                    kind = PlacementKind.LINK,
                )
            val destination =
                occurrence(
                    placementId = "destination-occ",
                    target = destinationTarget,
                    kind = PlacementKind.PRIMARY,
                )

            coordinator.cutBeacon("legacy-source", selected)
            coEvery { beaconTargetResolver.resolve("legacy-source") } returns sourceTarget
            coEvery { beaconTargetResolver.resolve("legacy-destination") } returns destinationTarget
            coEvery { hierarchyOccurrenceCommandService.occurrence(selected.placementId) } returns selected
            coEvery { hierarchyOccurrenceCommandService.occurrence(destination.placementId) } returns destination

            val capturedPlan = slot<HierarchyActionPlan>()
            coEvery {
                beaconOccurrenceWriter.executeBeaconPlan(capture(capturedPlan), any())
            } returns CanonicalV2HierarchyOccurrenceWriteResult(emptyList())

            val result =
                coordinator.pasteBeaconIntoBeacon(
                    targetBeaconId = "legacy-destination",
                    destinationOccurrence = destination,
                )

            assertEquals("Операцію з appearance орієнтира виконано", result.toast)
            assertFalse(coordinator.hasBeaconPayload.value)
            assertEquals(
                listOf(
                    HierarchyStructuralOperand.MoveOccurrence(
                        placementId = PlacementId("selected-link"),
                        newParentPlacementId = PlacementId("destination-occ"),
                    ),
                ),
                capturedPlan.captured.structural,
            )
            assertTrue(
                capturedPlan.captured.structural.none {
                    it is HierarchyStructuralOperand.MoveOccurrence &&
                        it.placementId == PlacementId("source-primary")
                },
            )
        }

    @Test
    fun `Beacon LINK CUT into Group preserves semantic Group destination in V2 plan`() =
        runTest {
            val sourceTarget = managedTarget("source-subject")
            val selected =
                occurrence(
                    placementId = "selected-link",
                    target = sourceTarget,
                    kind = PlacementKind.LINK,
                )

            coordinator.cutBeacon("legacy-source", selected)
            coEvery { beaconTargetResolver.resolve("legacy-source") } returns sourceTarget
            coEvery { hierarchyOccurrenceCommandService.occurrence(selected.placementId) } returns selected
            coEvery {
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                    "legacy-group",
                )
            } returns groupMapping("legacy-group", "group-subject")
            coEvery { orientationDao.getManagedSubject("group-subject") } returns subject("group-subject")

            val capturedPlan = slot<HierarchyActionPlan>()
            coEvery {
                beaconOccurrenceWriter.executeBeaconPlan(capture(capturedPlan), any())
            } returns CanonicalV2HierarchyOccurrenceWriteResult(emptyList())

            val result = coordinator.pasteBeaconIntoGroup("legacy-group")

            assertEquals("Операцію з appearance орієнтира виконано", result.toast)
            assertFalse(coordinator.hasBeaconPayload.value)
            assertEquals(
                listOf(
                    HierarchyStructuralOperand.MoveOccurrence(
                        placementId = PlacementId("selected-link"),
                        newParentPlacementId = null,
                    ),
                ),
                capturedPlan.captured.structural,
            )
            assertEquals(
                listOf(
                    HierarchyOccurrenceScopeOperand.SetRootGroupScope(
                        placementId = PlacementId("selected-link"),
                        groupSubjectId = "group-subject",
                    ),
                ),
                capturedPlan.captured.occurrenceScope,
            )
        }

    @Test
    fun `failed Beacon CUT paste retains payload`() =
        runTest {
            val sourceTarget = managedTarget("source-subject")
            val destinationTarget = managedTarget("destination-subject")
            val selected =
                occurrence(
                    placementId = "selected-link",
                    target = sourceTarget,
                    kind = PlacementKind.LINK,
                )
            val destination =
                occurrence(
                    placementId = "destination-occ",
                    target = destinationTarget,
                    kind = PlacementKind.PRIMARY,
                )

            coordinator.cutBeacon("legacy-source", selected)
            coEvery { beaconTargetResolver.resolve("legacy-source") } returns sourceTarget
            coEvery { beaconTargetResolver.resolve("legacy-destination") } returns destinationTarget
            coEvery { hierarchyOccurrenceCommandService.occurrence(selected.placementId) } returns selected
            coEvery { hierarchyOccurrenceCommandService.occurrence(destination.placementId) } returns destination
            coEvery {
                beaconOccurrenceWriter.executeBeaconPlan(any(), any())
            } throws IllegalArgumentException("writer rejected")

            val result =
                coordinator.pasteBeaconIntoBeacon(
                    targetBeaconId = "legacy-destination",
                    destinationOccurrence = destination,
                )

            assertEquals("writer rejected", result.toast)
            assertTrue(coordinator.hasBeaconPayload.value)
        }

    @Test(expected = CancellationException::class)
    fun `Beacon paste propagates cancellation and retains payload`() =
        runTest {
            val sourceTarget = managedTarget("source-subject")
            val destinationTarget = managedTarget("destination-subject")
            val selected =
                occurrence(
                    placementId = "selected-link",
                    target = sourceTarget,
                    kind = PlacementKind.LINK,
                )
            val destination =
                occurrence(
                    placementId = "destination-occ",
                    target = destinationTarget,
                    kind = PlacementKind.PRIMARY,
                )

            coordinator.cutBeacon("legacy-source", selected)
            coEvery { beaconTargetResolver.resolve("legacy-source") } returns sourceTarget
            coEvery { beaconTargetResolver.resolve("legacy-destination") } returns destinationTarget
            coEvery { hierarchyOccurrenceCommandService.occurrence(selected.placementId) } returns selected
            coEvery { hierarchyOccurrenceCommandService.occurrence(destination.placementId) } returns destination
            coEvery {
                beaconOccurrenceWriter.executeBeaconPlan(any(), any())
            } throws CancellationException("cancel")

            try {
                coordinator.pasteBeaconIntoBeacon(
                    targetBeaconId = "legacy-destination",
                    destinationOccurrence = destination,
                )
            } finally {
                assertTrue(coordinator.hasBeaconPayload.value)
            }
        }

    private fun occurrence(
        placementId: String,
        target: HierarchyTargetRef,
        kind: PlacementKind,
    ): HierarchyOccurrenceRef =
        HierarchyOccurrenceRef(
            placementId = PlacementId(placementId),
            target = target,
            parentPlacementId = null,
            placementKind = kind,
            siblingOrder = 0L,
        )

    private fun managedTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun groupMapping(
        legacyGroupId: String,
        subjectId: String,
    ) =
        LegacySubjectMappingEntity(
            id = "mapping-$legacyGroupId",
            sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
            sourceId = legacyGroupId,
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
            subjectType = "BEACON_GROUP",
            title = id,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
