package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceClipboardCoordinatorTest {
    private val repository = mockk<CanonicalWorkspaceRepository>()
    private val mainBeaconRepository = mockk<MainBeaconRepository>()
    private val coordinator =
        WorkspaceClipboardCoordinator(
            canonicalWorkspaceRepository = repository,
            mainBeaconRepository = mainBeaconRepository,
        )

    @Test
    fun `copy stores canonical Workspace ids and COPY intent`() {
        val result = coordinator.copyWorkspaces(linkedSetOf("alpha", "beta"))

        assertTrue(result.success)
        assertEquals(linkedSetOf("alpha", "beta"), coordinator.uiState.value.first)
        assertEquals(ContextClipboardOperationUi.COPY, coordinator.uiState.value.second)
    }

    @Test
    fun `target-only bulk CUT fails closed without mutating clipboard`() {
        val result = coordinator.cutWorkspaces(linkedSetOf("alpha", "beta"))

        assertFalse(result.success)
        assertTrue(coordinator.uiState.value.first.isEmpty())
        assertNull(coordinator.uiState.value.second)
    }

    @Test
    fun `successful CUT paste clears payload`() =
        runTest {
            coordinator.cutWorkspace("source", occurrence("source", "source-placement"))
            coEvery {
                repository.moveV2Occurrences(
                    sourcePlacementsByWorkspaceId = any(),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            } returns listOf("source")

            val result = coordinator.pasteInto("target", occurrence("target", "target-placement"))

            assertTrue(result.success)
            assertTrue(coordinator.uiState.value.first.isEmpty())
            assertNull(coordinator.uiState.value.second)
        }

    @Test
    fun `failed CUT paste retains payload`() =
        runTest {
            coordinator.cutWorkspace("source", occurrence("source", "source-placement"))
            coEvery {
                repository.moveV2Occurrences(
                    sourcePlacementsByWorkspaceId = any(),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            } throws IllegalArgumentException("cycle")

            val result = coordinator.pasteInto("target", occurrence("target", "target-placement"))

            assertFalse(result.success)
            assertEquals(setOf("source"), coordinator.uiState.value.first)
            assertEquals(ContextClipboardOperationUi.CUT, coordinator.uiState.value.second)
        }

    @Test
    fun `successful COPY paste retains payload for repeated paste`() =
        runTest {
            coordinator.copyWorkspace("source")
            coEvery {
                repository.copyV2WorkspacesShallow(
                    ids = any(),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            } returns listOf("copy-id")

            val result = coordinator.pasteInto("target", occurrence("target", "target-placement"))

            assertTrue(result.success)
            assertEquals(setOf("source"), coordinator.uiState.value.first)
            assertEquals(ContextClipboardOperationUi.COPY, coordinator.uiState.value.second)
        }

    @Test
    fun `COPY paste into Main Beacon adds appearance and retains payload`() =
        runTest {
            coordinator.copyWorkspace("source")
            coEvery {
                mainBeaconRepository.addRelatedContexts(
                    beaconId = "beacon",
                    contextIds = setOf("source"),
                )
            } returns 1

            val result = coordinator.pasteIntoBeacon("beacon")

            assertTrue(result.success)
            assertEquals(setOf("source"), coordinator.uiState.value.first)
            assertEquals(ContextClipboardOperationUi.COPY, coordinator.uiState.value.second)
        }

    @Test
    fun `CUT paste into Main Beacon moves appearance and consumes payload`() =
        runTest {
            coordinator.cutWorkspace("source", occurrence("source", "source-placement"))
            coEvery {
                mainBeaconRepository.moveRelatedContextsToBeacon(
                    beaconId = "beacon",
                    contextIds = setOf("source"),
                )
            } returns 1

            val result = coordinator.pasteIntoBeacon("beacon")

            assertTrue(result.success)
            assertTrue(coordinator.uiState.value.first.isEmpty())
            assertNull(coordinator.uiState.value.second)
        }

    @Test
    fun `paste propagates coroutine cancellation`() =
        runTest {
            coordinator.copyWorkspace("source")
            coEvery {
                repository.copyV2WorkspacesShallow(
                    ids = any(),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            } throws CancellationException("cancelled")

            var cancellation: CancellationException? = null
            try {
                coordinator.pasteInto("target", occurrence("target", "target-placement"))
            } catch (error: CancellationException) {
                cancellation = error
            }

            assertTrue(cancellation != null)
            assertEquals(setOf("source"), coordinator.uiState.value.first)
            assertEquals(ContextClipboardOperationUi.COPY, coordinator.uiState.value.second)
        }

    @Test
    fun `completed stale CUT paste does not clear newer clipboard payload`() =
        runTest {
            val repositoryEntered = CompletableDeferred<Unit>()
            val releaseRepository = CompletableDeferred<Unit>()

            coordinator.cutWorkspace("old-source", occurrence("old-source", "old-placement"))
            coEvery {
                repository.moveV2Occurrences(
                    sourcePlacementsByWorkspaceId = any(),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            } coAnswers {
                repositoryEntered.complete(Unit)
                releaseRepository.await()
                listOf("old-source")
            }

            val paste = async { coordinator.pasteInto("target", occurrence("target", "target-placement")) }

            repositoryEntered.await()
            coordinator.copyWorkspace("new-source")
            releaseRepository.complete(Unit)

            val result = paste.await()

            assertTrue(result.success)
            assertEquals(setOf("new-source"), coordinator.uiState.value.first)
            assertEquals(ContextClipboardOperationUi.COPY, coordinator.uiState.value.second)
        }

    @Test
    fun `copyWorkspaceInto uses exact destination occurrence`() =
        runTest {
            coEvery {
                repository.copyV2WorkspacesShallow(
                    ids = setOf("source"),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            } returns listOf("copy-id")

            val result =
                coordinator.copyWorkspaceInto(
                    sourceId = "source",
                    targetId = "target",
                    destinationOccurrence = occurrence("target", "target-placement"),
                )

            assertTrue(result.success)
            coVerify(exactly = 1) {
                repository.copyV2WorkspacesShallow(
                    ids = setOf("source"),
                    targetWorkspaceId = "target",
                    targetPlacementId = PlacementId("target-placement"),
                    now = any(),
                )
            }
        }

    @Test
    fun `copyWorkspaceInto rejects missing or mismatched destination before repository write`() =
        runTest {
            val missing =
                coordinator.copyWorkspaceInto(
                    sourceId = "source",
                    targetId = "target",
                    destinationOccurrence = null,
                )
            val mismatched =
                coordinator.copyWorkspaceInto(
                    sourceId = "source",
                    targetId = "target",
                    destinationOccurrence = occurrence("other", "other-placement"),
                )

            assertFalse(missing.success)
            assertFalse(mismatched.success)
            coVerify(exactly = 0) {
                repository.copyV2WorkspacesShallow(
                    ids = any(),
                    targetWorkspaceId = any(),
                    targetPlacementId = any(),
                    now = any(),
                )
            }
        }

    @Test
    fun `exact reserved System Workspace can enter canonical clipboard`() {
        val result = coordinator.copyWorkspace(SystemContexts.INBOX.raw)

        assertTrue(result.success)
        assertEquals(
            setOf(SystemContexts.INBOX.raw),
            coordinator.uiState.value.first,
        )
        assertEquals(
            ContextClipboardOperationUi.COPY,
            coordinator.uiState.value.second,
        )
    }

    @Test
    fun `exact reserved System Workspace can be a paste target`() {
        coordinator.copyWorkspace("ordinary")

        assertTrue(coordinator.canPasteInto(SystemContexts.INBOX.raw))
    }

    private fun occurrence(
        workspaceId: String,
        placementId: String,
    ): HierarchyOccurrenceRef =
        mockk<HierarchyOccurrenceRef> {
            every { target } returns
                HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspaceId)
            every { this@mockk.placementId } returns PlacementId(placementId)
        }

}
