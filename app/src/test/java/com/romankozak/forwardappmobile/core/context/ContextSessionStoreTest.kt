package com.romankozak.forwardappmobile.core.context

import com.google.common.truth.Truth.assertThat
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
}
