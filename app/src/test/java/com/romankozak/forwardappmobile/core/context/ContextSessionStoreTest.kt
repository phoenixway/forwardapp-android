package com.romankozak.forwardappmobile.core.context

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import org.junit.Test

class ContextSessionStoreTest {
    private fun createStore(): ContextSessionStore {
        val initial =
            DefaultContextState(
                id = ContextId("test"),
                features = CapabilitySet(emptySet()),
                views = ViewSet(emptySet(), ViewId("backlog")),
                config = ContextConfiguration(id = "initial", contextId = "test"),
            )
        val controller = DefaultContextController(initial)
        return ContextSessionStore(controller, ContextCapabilitiesResolver())
    }

    @Test
    fun `syncFromConfig prefers saved view mode when available`() {
        val store = createStore()
        val config =
            ContextConfiguration(
                id = "cfg",
                contextId = "c1",
                enableBacklog = true,
                enableInbox = true,
                enableDashboard = true,
            )

        val state =
            store.syncFromConfig(
                contextId = "c1",
                config = config,
                preferredViewName = ContextViewMode.INBOX.name,
                currentView = ContextViewMode.BACKLOG,
            )

        assertThat(state.currentView).isEqualTo(ContextViewMode.INBOX)
        assertThat(store.state.value.currentView).isEqualTo(ContextViewMode.INBOX)
    }

    @Test
    fun `syncFromConfig falls back to current view when saved is invalid`() {
        val store = createStore()
        val config =
            ContextConfiguration(
                id = "cfg",
                contextId = "c1",
                enableBacklog = true,
                enableInbox = true,
                enableDashboard = true,
            )

        val state =
            store.syncFromConfig(
                contextId = "c1",
                config = config,
                preferredViewName = ContextViewMode.DIRECTION.name,
                currentView = ContextViewMode.BACKLOG,
            )

        assertThat(state.currentView).isEqualTo(ContextViewMode.BACKLOG)
    }

    @Test
    fun `selectView falls back when requested view is not available`() {
        val store = createStore()
        val config =
            ContextConfiguration(
                id = "cfg",
                contextId = "c1",
                enableBacklog = true,
            )

        store.syncFromConfig(
            contextId = "c1",
            config = config,
            preferredViewName = null,
            currentView = ContextViewMode.BACKLOG,
        )

        val resolved = store.selectView(ContextViewMode.INBOX)
        assertThat(resolved).isEqualTo(ContextViewMode.BACKLOG)
        assertThat(store.state.value.currentView).isEqualTo(ContextViewMode.BACKLOG)
    }

    @Test
    fun `syncFromConfig dashboard override wins over legacy config`() {
        val store = createStore()
        val config =
            ContextConfiguration(
                id = "cfg",
                contextId = "c1",
                enableDashboard = true,
                enableBacklog = true,
            )

        val state =
            store.syncFromConfig(
                contextId = "c1",
                config = config,
                preferredViewName = ContextViewMode.DASHBOARD.name,
                currentView = ContextViewMode.DASHBOARD,
                dashboardEnabledOverride = false,
            )

        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("dashboard"))
        assertThat(state.currentView).isEqualTo(ContextViewMode.BACKLOG)
    }

    @Test
    fun `canonical System capability overrides win over contradictory legacy config`() {
        val store = createStore()
        val config =
            ContextConfiguration(
                id = "cfg",
                contextId = SystemContexts.INBOX.raw,
                enableInbox = true,
                enableAttachments = true,
                experimentalCapabilityIds =
                    listOf(
                        CapabilityId("direction"),
                        CapabilityId("inbox_sorting"),
                        CapabilityId("key_problems"),
                    ),
                enableBacklog = true,
            )

        val state =
            store.syncFromConfig(
                contextId = SystemContexts.INBOX.raw,
                config = config,
                preferredViewName = ContextViewMode.INBOX.name,
                currentView = ContextViewMode.BACKLOG,
                canonicalCapabilityOverrides =
                    mapOf(
                        CapabilityId("inbox") to false,
                        CapabilityId("direction") to false,
                        CapabilityId("connections") to false,
                        CapabilityId("inbox_sorting") to false,
                        CapabilityId("key_problems") to false,
                        CapabilityId("backlog") to false,
                    ),
            )

        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("inbox"))
        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("direction"))
        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("connections"))
        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("inbox_sorting"))
        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("key_problems"))
        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("backlog"))
        assertThat(state.canonicalCapabilityOverrides)
            .containsExactly(
                CapabilityId("inbox"),
                false,
                CapabilityId("direction"),
                false,
                CapabilityId("connections"),
                false,
                CapabilityId("inbox_sorting"),
                false,
                CapabilityId("key_problems"),
                false,
                CapabilityId("backlog"),
                false,
            )
        assertThat(state.availableViews).doesNotContain(ContextViewMode.BACKLOG)
        assertThat(state.currentView).isEqualTo(ContextViewMode.BACKLOG)
    }

    @Test
    fun `canonical System Backlog active survives legacy override mode and false flag`() {
        val store = createStore()
        val state =
            store.syncFromConfig(
                contextId = SystemContexts.INBOX.raw,
                config =
                    ContextConfiguration(
                        id = "cfg",
                        contextId = SystemContexts.INBOX.raw,
                        basePresetCode = "management",
                        applyMode = "OVERRIDE",
                        enableBacklog = false,
                    ),
                preferredViewName = ContextViewMode.BACKLOG.name,
                currentView = ContextViewMode.DASHBOARD,
                canonicalCapabilityOverrides = mapOf(CapabilityId("backlog") to true),
                suppressPresetCapabilityDerivation = true,
            )

        assertThat(state.enabledCapabilities).contains(CapabilityId("backlog"))
        assertThat(state.availableViews).contains(ContextViewMode.BACKLOG)
        assertThat(state.currentView).isEqualTo(ContextViewMode.BACKLOG)
    }

    @Test
    fun `canonical System Backlog disabled beats role preset`() {
        val store = createStore()
        val state =
            store.syncFromConfig(
                contextId = SystemContexts.INBOX.raw,
                config =
                    ContextConfiguration(
                        id = "cfg",
                        contextId = SystemContexts.INBOX.raw,
                        basePresetCode = "management",
                    ),
                preferredViewName = ContextViewMode.BACKLOG.name,
                currentView = ContextViewMode.BACKLOG,
                canonicalCapabilityOverrides = mapOf(CapabilityId("backlog") to false),
                suppressPresetCapabilityDerivation = true,
            )

        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("backlog"))
        assertThat(state.availableViews).doesNotContain(ContextViewMode.BACKLOG)
        assertThat(state.canonicalCapabilityOverrides).containsEntry(CapabilityId("backlog"), false)
        assertThat(state.suppressPresetCapabilityDerivation).isTrue()
    }

    @Test
    fun `promoted System preset metadata is inert while residual experimental ids remain live`() {
        val store = createStore()
        val residual = CapabilityId("unrelated")

        val state =
            store.syncFromConfig(
                contextId = SystemContexts.INBOX.raw,
                config =
                    ContextConfiguration(
                        id = "cfg",
                        contextId = SystemContexts.INBOX.raw,
                        basePresetCode = "management",
                        applyMode = "ADDITIVE",
                        experimentalCapabilityIds = listOf(residual),
                    ),
                preferredViewName = ContextViewMode.BACKLOG.name,
                currentView = ContextViewMode.DASHBOARD,
                suppressPresetCapabilityDerivation = true,
            )

        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("inbox"))
        assertThat(state.enabledCapabilities).doesNotContain(CapabilityId("backlog"))
        assertThat(state.enabledCapabilities).contains(residual)
    }
}
