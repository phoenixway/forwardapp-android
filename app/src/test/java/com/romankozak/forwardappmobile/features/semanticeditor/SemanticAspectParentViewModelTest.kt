package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntitySelectionMode
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SemanticAspectParentViewModelTest {
    @Test fun `editor includes direct children even if current Aspect is itself a child`() = runTest(dispatcher) {
        current = aspect("current", "parent")
        val model = model(vm())
        assertThat(model.aspectParentId).isEqualTo("parent")
        assertThat(model.aspectChildren).containsExactly(AspectChildItem("child", "Title child"))
    }

    @Test fun `resume refresh observes child rename reparent and deletion without overwriting draft or pending intent`() = runTest(dispatcher) {
        val child = aspect("child", "current")
        val live = MutableStateFlow(listOf(current, child, aspect("parent")))
        every { aspects.observeLive() } returns live
        val vm = vm()
        vm.setName("Unsaved title")
        select(vm, "parent")
        live.value = listOf(current, child.copy(subject = child.subject.copy(title = "Renamed child")), aspect("parent"))
        vm.refreshRelations()
        advanceUntilIdle()
        assertThat(model(vm).aspectChildren.single().title).isEqualTo("Renamed child")
        assertThat(model(vm).name).isEqualTo("Unsaved title")
        assertThat(model(vm).pendingRelation).isEqualTo(PendingSemanticRelation.AspectParent("parent"))
        live.value = listOf(current, child.copy(node = child.node.copy(parentAspectId = "parent")), aspect("parent"))
        vm.refreshRelations()
        advanceUntilIdle()
        assertThat(model(vm).aspectChildren).isEmpty()
        live.value = listOf(current, child.copy(subject = child.subject.copy(isDeleted = true)), aspect("parent"))
        vm.refreshRelations()
        advanceUntilIdle()
        assertThat(model(vm).aspectChildren).isEmpty()
        coVerify(exactly = 0) { aspects.move(any(), any(), any(), any()); links.linkOrientation(any(), any(), any(), any(), any()) }
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val aspects = mockk<CanonicalAspectRepository>()
    private val orientations = mockk<CanonicalOrientationRepository>()
    private val links = mockk<CanonicalAspectLinksRepository>()
    private val bindings = mockk<CanonicalWorkspaceBindingRepository>()
    private val graph = mockk<CanonicalOrientationGraphRepository>()
    private val catalog = mockk<CanonicalEntityCatalogRepository>()
    private var current = aspect("current")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { aspects.get("current") } coAnswers { current }
        every { aspects.observeLive() } answers { flowOf(listOf(current, aspect("parent"), aspect("other"),
            aspect("child", "current"), aspect("grandchild", "child"))) }
        every { links.observeMembershipsForAspect("current") } returns flowOf(emptyList())
        every { bindings.observeBindings("current") } returns flowOf(emptyList())
        every { catalog.observeCatalog() } returns flowOf(listOf("current", "parent", "other", "child", "grandchild").map {
            CanonicalEntitySummary(CanonicalEntityRef.Aspect(it), "Title $it")
        })
        coEvery { aspects.move(any(), any(), any(), any()) } coAnswers {
            current = current.copy(node = current.node.copy(parentAspectId = secondArg()),
                subject = current.subject.copy(version = current.subject.version + 1))
        }
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `parent picker excludes self current parent and all descendants and preserves typed identity`() = runTest(dispatcher) {
        current = aspect("current", "parent")
        val vm = vm()
        val request = vm.aspectParentPickerRequest()!!
        assertThat(request.allowedClasses).containsExactly(CanonicalEntityClass.ASPECT)
        assertThat(request.selectionMode).isEqualTo(CanonicalEntitySelectionMode.SINGLE)
        assertThat(request.excludedRefs).containsExactly(CanonicalEntityRef.Aspect("current"),
            CanonicalEntityRef.Aspect("parent"), CanonicalEntityRef.Aspect("child"), CanonicalEntityRef.Aspect("grandchild"))
        assertThat(model(vm).aspectTitles[model(vm).aspectParentId]).isEqualTo("Title parent")
        coVerify(exactly = 0) { aspects.move(any(), any(), any(), any()) }
    }

    @Test fun `selection requires confirmation and moves only via Aspect authority preserving draft and new version`() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("Draft")
        select(vm, "parent")
        assertThat(model(vm).pendingRelation).isEqualTo(PendingSemanticRelation.AspectParent("parent"))
        assertThat(model(vm).aspectParentId).isNull()
        coVerify(exactly = 0) { aspects.move(any(), any(), any(), any()) }
        vm.confirmAspectParent()
        advanceUntilIdle()
        coVerify(exactly = 1) { aspects.move("current", "parent", null, any()) }
        assertThat(model(vm).aspectParentId).isEqualTo("parent")
        assertThat(model(vm).version).isEqualTo(2L)
        assertThat(model(vm).name).isEqualTo("Draft")
        assertThat(model(vm).pendingRelation).isNull()
        coVerify(exactly = 0) { links.linkOrientation(any(), any(), any(), any(), any()); bindings.bind(any(), any(), any(), any()) }
    }

    @Test fun `cancel parent and root confirmations creates no mutation`() = runTest(dispatcher) {
        current = aspect("current", "parent")
        val vm = vm()
        select(vm, "other")
        vm.cancelPendingRelation()
        vm.confirmAspectParent()
        vm.requestAspectRoot()
        assertThat(model(vm).pendingRelation).isEqualTo(PendingSemanticRelation.AspectParent(null))
        vm.cancelPendingRelation()
        vm.confirmAspectParent()
        assertThat(model(vm).aspectParentId).isEqualTo("parent")
        coVerify(exactly = 0) { aspects.move(any(), any(), any(), any()) }
    }

    @Test fun `make root uses null parent and leaves identity intact`() = runTest(dispatcher) {
        current = aspect("current", "parent")
        val vm = vm()
        vm.requestAspectRoot()
        vm.confirmAspectParent()
        advanceUntilIdle()
        coVerify(exactly = 1) { aspects.move("current", null, null, any()) }
        assertThat(model(vm).aspectParentId).isNull()
        assertThat(model(vm).id).isEqualTo("current")
    }

    @Test fun `self descendants same parent wrong types and missing target fail closed`() = runTest(dispatcher) {
        current = aspect("current", "parent")
        val vm = vm()
        val refs = listOf(CanonicalEntityRef.Aspect("current"), CanonicalEntityRef.Aspect("child"),
            CanonicalEntityRef.Aspect("grandchild"), CanonicalEntityRef.Aspect("parent"),
            CanonicalEntityRef.Workspace("other"), CanonicalEntityRef.Orientation("other"), CanonicalEntityRef.Aspect("missing"))
        refs.forEach { ref ->
            vm.acceptPickerResult(SemanticRelationPickerKind.ASPECT_PARENT, setOf(ref))
            assertThat(model(vm).pendingRelation).isNull()
            vm.confirmAspectParent()
        }
        coVerify(exactly = 0) { aspects.move(any(), any(), any(), any()) }
        val orientation = SemanticEntityEditorModel("o", SemanticEntityClass.ORIENTATION, "O", "O", null, 1)
        assertThat(orientation.aspectParentRequest()).isNull()
    }

    @Test fun `double confirm dispatches once and transactional rejection retains current parent and draft`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { aspects.move(any(), any(), any(), any()) } coAnswers { gate.await(); error("Cycle detected") }
        val vm = vm()
        vm.setName("Draft")
        select(vm, "parent")
        vm.confirmAspectParent()
        vm.confirmAspectParent()
        assertThat(vm.aspectParentPickerRequest()).isNull()
        gate.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { aspects.move("current", "parent", null, any()) }
        assertThat(model(vm).aspectParentId).isNull()
        assertThat(model(vm).name).isEqualTo("Draft")
        assertThat(model(vm).errorMessage).contains("Cycle detected")
        assertThat(model(vm).isRelationMutationInFlight).isFalse()
    }

    private fun select(vm: SemanticEntityEditorViewModel, id: String) =
        vm.acceptPickerResult(SemanticRelationPickerKind.ASPECT_PARENT, setOf(CanonicalEntityRef.Aspect(id)))
    private fun model(vm: SemanticEntityEditorViewModel) = (vm.uiState.value as SemanticEntityEditorUiState.Loaded).model
    private fun vm() = SemanticEntityEditorViewModel(SavedStateHandle(mapOf("subjectId" to "current")),
        orientations, aspects, links, bindings, graph, catalog)
    private fun aspect(id: String, parent: String? = null) = CanonicalAspect(
        ManagedSubjectEntity(id, "ASPECT", "Title $id", null, 1, 1, null, false, 1),
        AspectEntity(id, parent, 0, false))
}
