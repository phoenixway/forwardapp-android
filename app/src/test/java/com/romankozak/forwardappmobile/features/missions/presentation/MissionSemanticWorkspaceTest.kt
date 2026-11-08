package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.data.workspace.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.planning.CompositionUiFixture
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MissionSemanticWorkspaceTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val base: PlanningParticipantSummary = CompositionUiFixture.item("intent", PlanningScopeKind.TACTICAL_CYCLE)
    private val item: PlanningParticipantSummary = base.copy(commitment = base.commitment.copy(role = PlanningRoleCode.MISSION,
        provenance = base.commitment.provenance.copy(sourceType = "TACTICAL_MISSION", sourceId = "42")),
        entity = base.entity.copy(title = "Canonical intent", orientationKind = OrientationKind.INTENT))
    private val subject = ManagedSubjectEntity(item.commitment.orientationId, "ORIENTATION", "Canonical intent", "**Plan**", 1, 2, null, false, 7)
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("workspace"), "Operational")
    private val frames = MutableStateFlow(PlanningCommitmentCompositionProjection(listOf(item), emptyList()))
    private val subjects = MutableStateFlow(listOf(subject))
    private val bindings = MutableStateFlow(emptyList<WorkspaceBindingEntity>())
    private val catalog = MutableStateFlow(listOf(item.entity, workspace))
    private val coordinator = mockk<SemanticEmbodyingWorkspaceCoordinator>()
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun binding(type: String = "EMBODIES", workspaceId: String = "workspace") =
        WorkspaceBindingEntity("b-$workspaceId-$type", workspaceId, subject.id, type, true, 0, 1, 1, null, false, 1)
    private fun project() = projectMissionSemanticWorkspaces(frames.value, subjects.value, bindings.value, catalog.value)
    private fun primary() = projectMissionPrimaryTargets(frames.value, catalog.value, project())
    private fun model() = MissionSemanticWorkspaceViewModel(
        mockk<PlanningCompositionReadRepository> { every { projection } returns frames },
        mockk<OrientationDao> { every { observeManagedSubjects() } returns subjects; every { observeWorkspaceBindings() } returns bindings },
        mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns catalog }, coordinator, navigation, dispatcher,
    ).also { store.put("workspace", it) }

    @Test fun `manual Mission action uses exact own INTENT with canonical prefill and version`() {
        val create = project().getValue(42) as MissionSemanticWorkspaceAction.Create
        assertEquals(subject.id, create.request.subjectId)
        assertEquals("Canonical intent", create.request.name)
        assertEquals("**Plan**", create.request.description)
        assertEquals(7L, create.request.expectedVersion)
    }

    @Test fun `default open without Workspace edits exact Mission Orientation and never creates Workspace`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.targets.collect {} }
        model.openPrimary(42)
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor(subject.id)) }
        assertNull(model.creation.value.request)
        coVerify(exactly = 0) { coordinator.create(any(), any()) }
    }

    @Test fun `default open and Workspace badge react to binding rename removal and target deletion`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.targets.collect {} }
        bindings.value = listOf(binding())
        assertEquals("Operational", model.targets.value.getValue(42).workspace?.title)
        model.openPrimary(42)
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("workspace")) }
        catalog.value = listOf(item.entity, workspace.copy(title = "Renamed"))
        assertEquals("Renamed", model.targets.value.getValue(42).workspace?.title)
        bindings.value = emptyList()
        assertEquals(NavTarget.SemanticEntityEditor(subject.id), model.targets.value.getValue(42).destination)
        bindings.value = listOf(binding())
        catalog.value = listOf(item.entity)
        assertNull(model.targets.value.getValue(42).workspace)
        assertEquals(NavTarget.SemanticEntityEditor(subject.id), model.targets.value.getValue(42).destination)
        catalog.value = emptyList()
        assertTrue(model.targets.value.isEmpty())
    }

    @Test fun `ambiguous and non EMBODIES bindings fall back to exact semantic editor`() {
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach {
            bindings.value = listOf(binding(it))
            assertEquals(NavTarget.SemanticEntityEditor(subject.id), primary().getValue(42).destination)
        }
        catalog.value = listOf(item.entity, workspace, workspace.copy(ref = CanonicalEntityRef.Workspace("other")))
        bindings.value = listOf(binding(), binding(workspaceId = "other"))
        assertEquals(NavTarget.SemanticEntityEditor(subject.id), primary().getValue(42).destination)
    }

    @Test fun `default open never guesses missing or ambiguous Mission participation`() {
        frames.value = frames.value.copy(participants = emptyList())
        assertTrue(primary().isEmpty())
        frames.value = frames.value.copy(participants = listOf(item, item.copy(commitment = item.commitment.copy(id = PlanningCommitmentId("duplicate")))))
        assertTrue(primary().isEmpty())
    }

    @Test fun `reused existing Orientation retains kind and identity instead of manufacturing INTENT`() {
        frames.value = frames.value.copy(participants = listOf(item.copy(entity = item.entity.copy(orientationKind = OrientationKind.PROJECT))))
        assertEquals(subject.id, (project().getValue(42) as MissionSemanticWorkspaceAction.Create).request.subjectId)
    }

    @Test fun `unique live EMBODIES opens exact Workspace while other binding kinds still permit creation`() {
        bindings.value = listOf(binding())
        assertEquals("workspace", (project().getValue(42) as MissionSemanticWorkspaceAction.Open).workspaceId)
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach {
            bindings.value = listOf(binding(it))
            assertTrue(project()[42] is MissionSemanticWorkspaceAction.Create)
        }
    }

    @Test fun `missing deleted or ambiguous embodiment fails closed and cannot create a duplicate`() {
        bindings.value = listOf(binding())
        catalog.value = listOf(item.entity)
        assertTrue(project().isEmpty())
        catalog.value = listOf(item.entity, workspace, workspace.copy(ref = CanonicalEntityRef.Workspace("other")))
        bindings.value = listOf(binding(), binding(workspaceId = "other"))
        assertTrue(project().isEmpty())
    }

    @Test fun `missing participation tombstoned subject ambiguous owner and typed ID collision never infer source`() {
        frames.value = frames.value.copy(participants = emptyList())
        assertTrue(project().isEmpty())
        frames.value = frames.value.copy(participants = listOf(item, item.copy(commitment = item.commitment.copy(id = PlanningCommitmentId("duplicate")))))
        assertTrue(project().isEmpty())
        frames.value = frames.value.copy(participants = listOf(item))
        subjects.value = listOf(subject.copy(isDeleted = true))
        assertTrue(project().isEmpty())
        subjects.value = listOf(subject)
        catalog.value = listOf(workspace.copy(ref = CanonicalEntityRef.Workspace(subject.id), title = subject.title))
        assertTrue(project().isEmpty())
    }

    @Test fun `binding add remove rename and participation removal update reactively`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.actions.collect {} }
        assertTrue(model.actions.value[42] is MissionSemanticWorkspaceAction.Create)
        bindings.value = listOf(binding())
        assertTrue(model.actions.value[42] is MissionSemanticWorkspaceAction.Open)
        catalog.value = listOf(item.entity, workspace.copy(title = "Renamed"))
        assertEquals("Renamed", (model.actions.value.getValue(42) as MissionSemanticWorkspaceAction.Open).title)
        bindings.value = emptyList()
        assertTrue(model.actions.value[42] is MissionSemanticWorkspaceAction.Create)
        frames.value = frames.value.copy(participants = emptyList())
        assertTrue(model.actions.value.isEmpty())
    }

    @Test fun `cancel does not create and existing Workspace navigates without creation`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.actions.collect {} }
        model.perform(42); model.cancel(); model.confirm()
        coVerify(exactly = 0) { coordinator.create(any(), any()) }
        bindings.value = listOf(binding())
        model.perform(42)
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("workspace")) }
    }

    @Test fun `confirmation dispatches canonical creation once and result is consumed once`() = runTest {
        val pending = CompletableDeferred<String>()
        val captured = slot<EmbodyingWorkspaceRequest>()
        coEvery { coordinator.create(capture(captured), any()) } coAnswers { pending.await() }
        val model = model()
        backgroundScope.launch(dispatcher) { model.actions.collect {} }
        model.perform(42); model.confirm(); model.confirm()
        assertTrue(model.creation.value.busy)
        assertEquals(subject.id, captured.captured.subjectId)
        pending.complete("created-workspace")
        assertEquals("created-workspace", model.consumeCreated())
        assertNull(model.consumeCreated())
        coVerify(exactly = 1) { coordinator.create(any(), any()) }
    }

    @Test fun `creation failure preserves editable draft without success or navigation`() = runTest {
        coEvery { coordinator.create(any(), any()) } throws IllegalStateException("changed source")
        val model = model()
        backgroundScope.launch(dispatcher) { model.actions.collect {} }
        model.perform(42); model.setName("My workspace"); model.confirm()
        assertEquals("My workspace", model.creation.value.request?.name)
        assertEquals("changed source", model.creation.value.error)
        assertNull(model.consumeCreated())
        verify(exactly = 0) { navigation.navigate(any<NavTarget>()) }
    }
}
