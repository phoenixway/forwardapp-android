package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.context.*
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.WorkspacePresentationPreferences
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ContextDataApplyActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.*
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceInitialViewTest {
    private fun store() = ContextSessionStore(DefaultContextController(DefaultContextState(
        id = ContextId("initial"), features = CapabilitySet(emptySet()), views = ViewSet(emptySet(), ViewId("dashboard")),
        config = ContextConfiguration(id = "initial", contextId = "initial"))), ContextCapabilitiesResolver())

    private fun data(id: String, preferred: String? = null, inboxEnabled: Boolean = false) = ContextData.Loaded(
        context = null, items = emptyList(), attachmentItems = emptyList(), config = ContextConfiguration.default(id),
        logs = emptyList(), checklists = emptyList(), noteDocuments = emptyList(), directionItems = emptyList(),
        linkedContextNames = emptyMap(), reminders = emptyList(), recentItems = emptyList(), notes = emptyList(),
        enabledCapabilityOverrides = WorkspaceCoreViews.capabilities, executionLogEnabledOverride = false,
        canonicalCapabilityOverrides = WorkspaceCoreViews.overrides() + mapOf(CapabilityId("backlog") to false, CapabilityId("inbox") to inboxEnabled),
        suppressPresetCapabilityDerivation = true,
        presentation = ContextPresentation(id = id, name = "Workspace", description = "", parentId = null,
            roleCode = null, order = 0L, tags = null),
        presentationPreferences = WorkspacePresentationPreferences(defaultViewModeName = preferred),
    )

    @Test fun `new Workspace starts on Dashboard with both intrinsic views and without Backlog`() = runTest {
        val state = ContextStateManager(this)
        val session = store()
        val actions = ContextDataApplyActions(state, session, mockk<RecentItemsRepository>(relaxed = true), this)
        assertThat(isWorkspaceViewReady(state.uiState.value, session.state.value.currentView)).isFalse()
        actions.applyLoaded(data("new"), {}, {})
        assertThat(state.uiState.value.currentViewMode).isEqualTo(ContextViewMode.DASHBOARD)
        assertThat(session.state.value.availableViews).containsAtLeast(ContextViewMode.DASHBOARD, ContextViewMode.RELATIONS)
        assertThat(session.state.value.availableViews).doesNotContain(ContextViewMode.BACKLOG)
        assertThat(isWorkspaceViewReady(state.uiState.value, session.state.value.currentView)).isTrue()
    }

    @Test fun `saved view is the first rendered view without an intermediate Dashboard`() = runTest {
        val state = ContextStateManager(this)
        val session = store()
        val rendered = mutableListOf<ContextViewMode>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.uiState.collect { if (isWorkspaceViewReady(it, session.state.value.currentView)) rendered += it.currentViewMode }
        }
        ContextDataApplyActions(state, session, mockk(relaxed = true), this)
            .applyLoaded(data("saved", "INBOX", inboxEnabled = true), {}, {})
        assertThat(rendered).containsExactly(ContextViewMode.INBOX)
    }

    @Test fun `switching Workspace does not inherit the previous Workspace view`() = runTest {
        val state = ContextStateManager(this)
        val session = store()
        val actions = ContextDataApplyActions(state, session, mockk(relaxed = true), this)
        actions.applyLoaded(data("first", "INBOX", inboxEnabled = true), {}, {})
        actions.applyLoaded(data("second", inboxEnabled = true), {}, {})
        assertThat(state.uiState.value.currentViewMode).isEqualTo(ContextViewMode.DASHBOARD)
    }

    @Test fun `disabled saved view falls back to Dashboard not a disabled Backlog`() = runTest {
        val state = ContextStateManager(this)
        val session = store()
        ContextDataApplyActions(state, session, mockk(relaxed = true), this).applyLoaded(data("w", "BACKLOG"), {}, {})
        assertThat(state.uiState.value.currentViewMode).isEqualTo(ContextViewMode.DASHBOARD)
    }

    @Test fun `resolved empty data cannot remain stuck behind the render gate`() = runTest {
        val state = ContextStateManager(this)
        val session = store()
        val actions = ContextDataApplyActions(state, session, mockk(relaxed = true), this)
        actions.applyLoaded(data("w", "INBOX", inboxEnabled = true), {}, {})
        actions.applyEmpty({}, {})
        assertThat(state.uiState.value.presentation).isNull()
        assertThat(isWorkspaceViewReady(state.uiState.value, session.state.value.currentView)).isTrue()
    }

    @Test fun `render gate also waits for session consistency and context switching`() {
        val ready = ContextUiState(isInitialViewResolved = true, currentViewMode = ContextViewMode.INBOX)
        assertThat(isWorkspaceViewReady(ready, ContextViewMode.DASHBOARD)).isFalse()
        assertThat(isWorkspaceViewReady(ready.copy(isContextSwitching = true), ContextViewMode.INBOX)).isFalse()
        assertThat(isWorkspaceViewReady(ready, ContextViewMode.INBOX)).isTrue()
    }
}
