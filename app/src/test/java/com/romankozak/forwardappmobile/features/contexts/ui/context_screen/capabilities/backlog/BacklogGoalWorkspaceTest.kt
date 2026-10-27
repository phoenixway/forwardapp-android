package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.product.*
import com.romankozak.forwardappmobile.data.workspace.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BacklogGoalWorkspaceTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val goal = CanonicalGoalSemanticProjection("goal-locator", "subject", "Meaning", null, OrientationLifecycle.ACTIVE)
    private val subject = ManagedSubjectEntity("subject", "ORIENTATION", "Canonical goal", "**Plan**", 1, 2, null, false, 7)
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("subject"), "Canonical goal")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Operational")
    private val goals = MutableStateFlow(listOf(goal))
    private val subjects = MutableStateFlow(listOf(subject))
    private val bindings = MutableStateFlow(emptyList<WorkspaceBindingEntity>())
    private val catalog = MutableStateFlow(listOf(orientation, workspace))
    private val coordinator = mockk<SemanticEmbodyingWorkspaceCoordinator>()
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun binding(type: String = "EMBODIES") = WorkspaceBindingEntity("b", "w", "subject", type, true, 0, 1, 1, null, false, 1)
    private fun model() = BacklogGoalWorkspaceViewModel(
        mockk<CanonicalGoalSemanticReadRepository> { every { observeAll() } returns goals },
        mockk<OrientationDao> {
            every { observeManagedSubjects() } returns subjects
            every { observeWorkspaceBindings() } returns bindings
        },
        mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns catalog }, coordinator, navigation, dispatcher,
    ).also { store.put("goals", it) }

    @Test fun `locator resolves exact canonical source and never infers from raw ID or title`() {
        val result = projectBacklogGoalLinks(listOf(goal), listOf(subject), emptyList(), listOf(orientation, workspace,
            workspace.copy(ref = CanonicalEntityRef.Workspace("goal-locator"), title = "Canonical goal")))
        assertEquals("subject", result.getValue("goal-locator").subjectId)
        assertNull(result.getValue("goal-locator").workspace)
        assertTrue(result.getValue("goal-locator").canCreate)
        assertTrue(projectBacklogGoalLinks(emptyList(), listOf(subject), emptyList(), catalog.value).isEmpty())
    }

    @Test fun `deleted missing and wrong typed subjects fail closed`() {
        listOf(emptyList(), listOf(subject.copy(isDeleted = true)), listOf(subject.copy(subjectType = "ASPECT")))
            .forEach { assertTrue(projectBacklogGoalLinks(listOf(goal), it, emptyList(), catalog.value).isEmpty()) }
    }

    @Test fun `only unique live EMBODIES produces Workspace badge`() {
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach { type ->
            val link = projectBacklogGoalLinks(listOf(goal), subjects.value, listOf(binding(type)), catalog.value).getValue(goal.goalId)
            assertNull(link.workspace)
            assertTrue(link.canCreate)
        }
        val live = projectBacklogGoalLinks(listOf(goal), subjects.value, listOf(binding()), catalog.value).getValue(goal.goalId)
        assertEquals(workspace, live.workspace)
        assertFalse(live.canCreate)
        val duplicate = projectBacklogGoalLinks(listOf(goal), subjects.value, listOf(binding(), binding().copy(id = "other")), catalog.value).getValue(goal.goalId)
        assertNull(duplicate.workspace)
        assertFalse(duplicate.canCreate)
    }

    @Test fun `reactive binding rename deletion and unbind update without refresh`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        assertNull(model.links.value.getValue(goal.goalId).workspace)
        bindings.value = listOf(binding())
        assertEquals("w", model.links.value.getValue(goal.goalId).workspace?.ref?.id)
        catalog.value = listOf(orientation, workspace.copy(title = "Renamed"))
        assertEquals("Renamed", model.links.value.getValue(goal.goalId).workspace?.title)
        catalog.value = listOf(orientation)
        assertNull(model.links.value.getValue(goal.goalId).workspace)
        bindings.value = listOf(binding().copy(isDeleted = true))
        assertTrue(model.links.value.getValue(goal.goalId).canCreate)
    }

    @Test fun `creation uses canonical prefill version and existing coordinator with double submit guard`() = runTest {
        val pending = CompletableDeferred<String>()
        val request = slot<EmbodyingWorkspaceRequest>()
        coEvery { coordinator.create(capture(request), any()) } coAnswers { pending.await() }
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        model.open(goal.goalId)
        assertEquals("Canonical goal", model.creation.value.request?.name)
        assertEquals("**Plan**", model.creation.value.request?.description)
        model.confirm(); model.confirm()
        assertTrue(model.creation.value.busy)
        assertEquals("subject", request.captured.subjectId)
        assertEquals(7L, request.captured.expectedVersion)
        pending.complete("w")
        assertNull(model.creation.value.request)
        coVerify(exactly = 1) { coordinator.create(any(), any()) }
    }

    @Test fun `cancel never creates and bound entity cannot create again`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        model.open(goal.goalId); model.cancel(); model.confirm()
        bindings.value = listOf(binding())
        model.open(goal.goalId); model.confirm()
        assertNull(model.creation.value.request)
        coVerify(exactly = 0) { coordinator.create(any(), any()) }
    }

    @Test fun `semantic action stays semantic while badge navigates to exact Workspace`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        bindings.value = listOf(binding())
        model.openSemantic(goal.goalId); model.openWorkspace(goal.goalId)
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor("subject")) }
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("w")) }
    }

    @Test fun `failed creation remains retryable without reporting fake Workspace`() = runTest {
        coEvery { coordinator.create(any(), any()) } throws IllegalStateException("failure")
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        model.open(goal.goalId); model.confirm()
        assertEquals("failure", model.creation.value.error)
        assertNotNull(model.creation.value.request)
        assertNull(model.creation.value.createdId)
    }
}
