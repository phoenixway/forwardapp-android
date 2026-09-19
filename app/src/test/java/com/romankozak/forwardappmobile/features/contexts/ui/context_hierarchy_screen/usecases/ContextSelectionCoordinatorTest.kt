package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextSelectionCoordinatorTest {
    @Test
    fun canonicalProjectLikeIdsAreRetainedAndToggledWithoutContextBacking() {
        val coordinator = ContextSelectionCoordinator()

        coordinator.start("workspace-child")

        assertTrue(coordinator.handleProjectClick("workspace-child"))
        assertEquals(emptySet<String>(), coordinator.selectedIds.value)

        coordinator.start("workspace-child")
        coordinator.retainExistingProjectIds(setOf("workspace-child"))
        assertEquals(setOf("workspace-child"), coordinator.selectedIds.value)

        coordinator.retainExistingProjectIds(emptySet())
        assertFalse(coordinator.handleProjectClick("workspace-child"))
    }
}
