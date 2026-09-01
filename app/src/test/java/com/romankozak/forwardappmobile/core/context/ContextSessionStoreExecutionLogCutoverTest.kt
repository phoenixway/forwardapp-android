package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextSessionStoreExecutionLogCutoverTest {
    @Test
    fun `canonical disabled override wins over legacy enabled log`() {
        val controller = mockk<ContextController>(relaxed = true)
        val store = ContextSessionStore(controller, ContextCapabilitiesResolver())

        val state =
            store.syncFromConfig(
                contextId = "context-1",
                config =
                    ContextConfiguration.default("context-1").copy(
                        applyMode = "OVERRIDE",
                        enableLog = true,
                    ),
                preferredViewName = null,
                currentView = ContextViewMode.DASHBOARD,
                executionLogEnabledOverride = false,
            )

        assertFalse(state.enabledCapabilities.contains(CapabilityId("log")))
    }

    @Test
    fun `canonical enabled override wins over legacy disabled log`() {
        val controller = mockk<ContextController>(relaxed = true)
        val store = ContextSessionStore(controller, ContextCapabilitiesResolver())

        val state =
            store.syncFromConfig(
                contextId = "context-1",
                config =
                    ContextConfiguration.default("context-1").copy(
                        applyMode = "OVERRIDE",
                        enableLog = false,
                    ),
                preferredViewName = null,
                currentView = ContextViewMode.DASHBOARD,
                executionLogEnabledOverride = true,
            )

        assertTrue(state.enabledCapabilities.contains(CapabilityId("log")))
    }
}
