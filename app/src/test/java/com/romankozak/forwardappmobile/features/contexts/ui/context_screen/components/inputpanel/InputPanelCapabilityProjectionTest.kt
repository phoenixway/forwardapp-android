package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputPanelCapabilityProjectionTest {
    @Test
    fun `canonical false removes legacy Direction and Inbox while preserving unrelated capabilities`() {
        val result =
            resolveInputPanelCapabilities(
                enableInbox = true,
                enableLog = false,
                enableBacklog = false,
                enableDashboard = false,
                enableAttachments = false,
                experimentalCapabilityIds =
                    listOf(
                        CapabilityId("direction"),
                        CapabilityId("unrelated"),
                    ),
                enabledCapabilitiesOverride =
                    setOf(
                        CapabilityId("inbox"),
                        CapabilityId("direction"),
                    ),
                canonicalCapabilityOverrides =
                    mapOf(
                        CapabilityId("inbox") to false,
                        CapabilityId("direction") to false,
                    ),
            )

        assertFalse(CapabilityId("inbox") in result)
        assertFalse(CapabilityId("direction") in result)
        assertTrue(CapabilityId("unrelated") in result)
    }

    @Test
    fun `canonical true adds capability absent from every legacy source`() {
        val result =
            resolveInputPanelCapabilities(
                enableInbox = false,
                enableLog = false,
                enableBacklog = false,
                enableDashboard = false,
                enableAttachments = false,
                experimentalCapabilityIds = emptyList(),
                enabledCapabilitiesOverride = emptySet(),
                canonicalCapabilityOverrides = mapOf(CapabilityId("direction") to true),
            )

        assertEquals(setOf(CapabilityId("direction")), result)
    }
}
