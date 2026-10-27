package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsRegistry
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.WorkspacePresentationPreferences
import com.romankozak.forwardappmobile.data.workspace.WorkspacePresentationPreferencesRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceOperationalSettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private inner class Fixture(id: String = "workspace", source: String? = null) {
        val row = MutableStateFlow<WorkspaceEntity?>(WorkspaceEntity(id, "Canonical", null, null, 1, 1, null, false, 1,
            provenance = if (source == null) "STANDALONE" else "CONTEXT_BACKED", sourceContextId = source))
        val workspace = mockk<WorkspaceDao> {
            every { observeById(id) } returns row
            coEvery { getById(id) } answers { row.value }
        }
        val rows = MutableStateFlow<List<WorkspaceCapabilityInstanceEntity>>(emptyList())
        val orientation = mockk<OrientationDao> { every { observeWorkspaceCapabilities(id) } returns rows }
        val prefsState = MutableStateFlow<WorkspacePresentationPreferences?>(WorkspacePresentationPreferences())
        val prefs = mockk<WorkspacePresentationPreferencesRepository>(relaxed = true) {
            every { observeCanonicalOrNull(id) } returns prefsState
        }
        val commands = mockk<CanonicalOrdinaryCapabilitySettings>(relaxed = true)
        val reminders = mockk<ReminderRepository>(relaxed = true) {
            every { getRemindersForEntityFlow(id) } returns flowOf(emptyList())
        }
        val saved = SavedStateHandle(mapOf("workspaceId" to id))
        val registry = mockk<CapabilitySettingsRegistry> { every { forCapabilities(any()) } returns emptyList() }
        val tags = mockk<CanonicalWorkspaceTagRepository>(relaxed = true) {
            every { observeLiveTagsByWorkspace() } returns flowOf(emptyMap())
        }
        val systemTags = mockk<SystemWorkspaceTagAuthority> { every { observeEffectiveOwners() } returns flowOf(emptyList()) }
        val vm = WorkspaceOperationalSettingsViewModel(saved, workspace, orientation, prefs, commands, reminders, registry, tags, systemTags)
            .also { store.put("vm:$id", it) }
    }

    @Test fun `ordinary Context-backed and System identities load without a Context dependency or writes`() = runTest(dispatcher) {
        listOf("native" to null, "backed" to "legacy", "inbox" to null).forEach { (id, source) ->
            val f = Fixture(id, source)
            assertThat(f.vm.state.value.ready).isTrue()
            assertThat(f.vm.state.value.available).isTrue()
            coVerify(exactly = 0) { f.commands.setEnabled(any(), any(), any()) }
            coVerify(exactly = 0) { f.prefs.setShowCheckboxes(any(), any(), any()) }
        }
    }

    @Test fun `display action saves only preferences and waits for canonical flow confirmation`() = runTest(dispatcher) {
        val f = Fixture()
        f.vm.setCheckboxes(true)
        coVerify(exactly = 1) { f.prefs.setShowCheckboxes("workspace", true, any()) }
        assertThat(f.vm.state.value.showCheckboxes).isFalse()
        f.prefsState.value = WorkspacePresentationPreferences(showCheckboxes = true)
        assertThat(f.vm.state.value.showCheckboxes).isTrue()
        verify { f.commands wasNot Called }
    }

    @Test fun `one feature action delegates only the selected family and failure preserves persisted projection`() = runTest(dispatcher) {
        val f = Fixture()
        coEvery { f.commands.setEnabled(any(), any(), any()) } throws IllegalStateException("Rejected")
        f.vm.setCapability("inbox", true)
        coVerify(exactly = 1) { f.commands.setEnabled("workspace", CapabilityId("inbox"), true) }
        assertThat(f.vm.state.value.error).isEqualTo("Rejected")
        assertThat(f.vm.state.value.enabled).containsExactly(CapabilityId("dashboard"), CapabilityId("relations"))
        assertThat(f.vm.state.value.busy).isFalse()
        coVerify(exactly = 0) { f.prefs.setShowCheckboxes(any(), any(), any()) }
    }

    @Test fun `in-flight action suppresses repeated taps and missing Workspace prevents writes`() = runTest(dispatcher) {
        val f = Fixture()
        val gate = CompletableDeferred<Unit>()
        coEvery { f.commands.setEnabled(any(), any(), any()) } coAnswers { gate.await() }
        f.vm.setCapability("backlog", true)
        f.vm.setCapability("backlog", true)
        assertThat(f.vm.state.value.busy).isTrue()
        gate.complete(Unit)
        coVerify(exactly = 1) { f.commands.setEnabled(any(), any(), any()) }
        f.row.value = null
        f.vm.setCheckboxes(true)
        coVerify(exactly = 0) { f.prefs.setShowCheckboxes(any(), any(), any()) }
    }

    @Test fun `reminders retain exact Workspace identity and remain separate from scalar save`() = runTest(dispatcher) {
        val f = Fixture()
        f.vm.onSetReminder(2030, 0, 1, 12, 0)
        f.vm.onClearReminder()
        coVerify(exactly = 1) { f.reminders.createReminder("workspace", "PROJECT", any()) }
        coVerify(exactly = 1) { f.reminders.clearRemindersForEntityType("workspace", "PROJECT") }
        verify { f.commands wasNot Called }
    }

    @Test fun `live active optional capabilities and intrinsic core views enable their registered settings tabs`() = runTest(dispatcher) {
        val f = Fixture()
        fun cap(id: String, type: String, state: String = "ACTIVE", deleted: Boolean = false) =
            WorkspaceCapabilityInstanceEntity(id, "workspace", type, "default", 0, state, 1, "{}", 1, 1, null, deleted, 1)
        f.rows.value = listOf(cap("a", "INBOX"), cap("b", "BACKLOG", "DISABLED"), cap("c", "DIRECTION", deleted = true))
        assertThat(f.vm.state.value.enabled).containsExactly(CapabilityId("inbox"), CapabilityId("dashboard"), CapabilityId("relations"))
        f.vm.capabilityTabs()
        verify { f.registry.forCapabilities(setOf(CapabilityId("inbox"), CapabilityId("dashboard"), CapabilityId("relations"))) }
    }

    @Test fun `tab state persists and no score or structural preset tab is exposed`() = runTest(dispatcher) {
        val f = Fixture()
        f.vm.selectTab("semantics")
        assertThat(f.saved.get<String>("workspacePropertiesTab")).isEqualTo("semantics")
        assertThat(workspacePropertiesBaseTabs).doesNotContain("evaluation")
        assertThat(workspacePropertiesBaseTabs).doesNotContain("presets")
        assertThat(workspaceSettingsIdentity(SavedStateHandle(mapOf("projectId" to "legacy-route")))).isEqualTo("legacy-route")
        assertThat(workspaceSettingsIdentity(SavedStateHandle(mapOf("workspaceId" to "", "projectId" to "legacy")))).isEmpty()
    }

    @Test fun `tags use canonical tag authority independently from identity and capability fields`() = runTest(dispatcher) {
        val f = Fixture()
        f.vm.addTag("tag")
        coVerify(exactly = 1) { f.tags.replaceTags("workspace", listOf("tag"), any()) }
        verify { f.commands wasNot Called }
    }
}
