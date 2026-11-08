package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.WorkspaceBacklogTacticalProjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceBacklogTacticalProjectionTest {
    @Test fun `two badge facets share one upstream read and react to removal and stream changes`() = runTest {
        var subscriptions = 0
        val updates = MutableStateFlow(listOf(mission(1, "a", null), mission(2, "b", "stream")))
        val projection = WorkspaceBacklogTacticalProjection(flow { subscriptions++; emitAll(updates) }, backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { projection.itemIds.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { projection.streamIds.collect() }
        runCurrent()
        assertEquals(1, subscriptions)
        assertEquals(setOf("a", "b"), projection.itemIds.value)
        assertEquals(mapOf("a" to "general", "b" to "stream"), projection.streamIds.value)
        updates.value = listOf(mission(1, "a", "new-stream"))
        runCurrent()
        assertEquals(setOf("a"), projection.itemIds.value)
        assertEquals(mapOf("a" to "new-stream"), projection.streamIds.value)
        assertEquals(1, subscriptions)
        updates.value = emptyList(); runCurrent()
        assertTrue(projection.itemIds.value.isEmpty())
        assertTrue(projection.streamIds.value.isEmpty())
    }

    @Test fun `unlinked rows are ignored and duplicate placements retain existing stream policy`() = runTest {
        val projection = WorkspaceBacklogTacticalProjection(flowOf(listOf(
            mission(1, null, "unused"), mission(2, "a", "first"), mission(3, "a", "last"),
        )), backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { projection.itemIds.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { projection.streamIds.collect() }
        runCurrent()
        assertEquals(setOf("a"), projection.itemIds.value)
        assertEquals(mapOf("a" to "last"), projection.streamIds.value)
    }

    private fun mission(id: Long, item: String?, stream: String?) = TacticalMission(
        id = id, title = "Mission", description = null, deadline = 0, projectId = null,
        sourceBacklogItemId = item, missionStreamId = stream,
    )
}
