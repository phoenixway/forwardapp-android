package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityArchetype
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityAvailability
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CapabilityKernelTest {
    @Test
    fun `enable creates resurrects and reuses active projection`() {
        val created = transitionCapabilityLifecycle(null, CapabilityLifecycleCommand.ENABLE)
        assertEquals(WorkspaceCapabilityState.ACTIVE, created.state)
        assertTrue(!created.isDeleted)

        val unchanged = transitionCapabilityLifecycle(created, CapabilityLifecycleCommand.ENABLE)
        assertEquals(created, unchanged)

        val resurrected =
            transitionCapabilityLifecycle(
                created.copy(isDeleted = true),
                CapabilityLifecycleCommand.ENABLE,
            )
        assertEquals(created, resurrected)
    }

    @Test
    fun `restore is non activating and archived enable fails`() {
        val archived =
            CapabilityLifecycleProjection(
                state = WorkspaceCapabilityState.ARCHIVED,
                isDeleted = false,
            )

        assertFailsWith<IllegalArgumentException> {
            transitionCapabilityLifecycle(archived, CapabilityLifecycleCommand.ENABLE)
        }

        val restored = transitionCapabilityLifecycle(archived, CapabilityLifecycleCommand.RESTORE)
        assertEquals(WorkspaceCapabilityState.DISABLED, restored.state)
        assertTrue(!restored.isDeleted)
    }

    @Test
    fun `registry declares target reserved and retired archetypes`() {
        val definitions = orientationCapabilityRegistry.associateBy { it.type }

        assertEquals(
            WorkspaceCapabilityArchetype.OWNED_COLLECTION,
            definitions.getValue(WorkspaceCapabilityType.KEY_PROBLEMS).archetype,
        )
        assertEquals(
            WorkspaceCapabilityArchetype.ORDERED_PLACEMENT,
            definitions.getValue(WorkspaceCapabilityType.DIRECTION).archetype,
        )
        assertEquals(
            WorkspaceCapabilityAvailability.RESERVED,
            definitions.getValue(WorkspaceCapabilityType.DOCUMENTS).availability,
        )
        assertEquals(
            WorkspaceCapabilityAvailability.RETIRED,
            definitions.getValue(WorkspaceCapabilityType.ARTIFACT).availability,
        )
        assertEquals(
            WorkspaceCapabilityAvailability.RETIRED,
            definitions.getValue(WorkspaceCapabilityType.JOURNAL).availability,
        )
    }
}
