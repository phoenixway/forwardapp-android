package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitypicker.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BigPictureMutationViewModelTest {
    @Test fun `rename dispatches exact container once while busy`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { writer.renameContainer("section", "New title", any()) } coAnswers { gate.await() }
        val model = vm()
        model.renameSection(H1TypedNode.Container("section"), "New title")
        model.renameSection(H1TypedNode.Container("section"), "New title")
        advanceUntilIdle()
        assertThat(model.state.value.busy).isTrue()
        gate.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.renameContainer("section", "New title", any()) }
        assertThat(model.state.value.busy).isFalse()
    }

    @Test fun `recursive removal dispatches exact occurrence subtree once and clears retired clipboard`() = runTest(dispatcher) {
        val gate = kotlinx.coroutines.CompletableDeferred<List<PlacementId>>()
        coEvery { writer.removeOccurrenceSubtree(PlacementId("link"), any()) } coAnswers { gate.await() }
        val saved = SavedStateHandle(mapOf("bigPictureCut" to "occurrence:child"))
        val model = vm(saved)
        model.removeSubtree(H1TypedNode.Occurrence("link"))
        model.removeSubtree(H1TypedNode.Occurrence("link"))
        advanceUntilIdle()
        assertThat(model.state.value.busy).isTrue()
        gate.complete(listOf(PlacementId("link"), PlacementId("child")))
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.removeOccurrenceSubtree(PlacementId("link"), any()) }
        coVerify(exactly = 0) { writer.removeOccurrence(any(), any()); writer.deleteContainer(any(), any()) }
        assertThat(model.clipboard.value).isNull()
    }

    @Test fun `failed recursive removal keeps clipboard and releases submit guard`() = runTest(dispatcher) {
        coEvery { writer.removeOccurrenceSubtree(any(), any()) } throws IllegalStateException("rejected")
        val saved = SavedStateHandle(mapOf("bigPictureCut" to "occurrence:link"))
        val model = vm(saved)
        model.removeSubtree(H1TypedNode.Occurrence("link"))
        advanceUntilIdle()
        assertThat(model.clipboard.value).isEqualTo("occurrence:link")
        assertThat(model.state.value.busy).isFalse()
        assertThat(model.state.value.message).contains("rejected")
    }

    @Test fun `leaf rejection does not automatically escalate to recursive removal`() = runTest(dispatcher) {
        coEvery { writer.removeOccurrence(any(), any()) } throws
            com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException(PlacementId("p"))
        val model = vm()
        model.remove(H1TypedNode.Occurrence("p"))
        advanceUntilIdle()
        coVerify(exactly = 0) { writer.removeOccurrenceSubtree(any(), any()) }
        assertThat(model.state.value.message).contains("Повторіть дію")
    }

    private val dispatcher = StandardTestDispatcher()
    private val writer = mockk<H1StructuralMutationCoordinator>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = BigPictureMutationViewModel(writer, handle)
    private fun result(ref: CanonicalEntityRef) = SavedStateHandle(mapOf(
        "bigPicturePickerPending" to true,
        CANONICAL_ENTITY_PICKER_RESULT_KEY to listOf(CanonicalEntityRefCodec.encode(ref)),
    ))

    @Test fun `production mutation activation is enabled after GroupScope ingress repair`() {
        assertThat(BIG_PICTURE_MUTATION_UX_ENABLED).isTrue()
    }

    @Test fun `picker adds immediately at captured location and consumes identity only once`() = runTest(dispatcher) {
        val model = vm()
        assertThat(model.pickerRequest().allowedClasses).containsExactlyElementsIn(CanonicalEntityClass.entries)
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("new-occurrence")
        val destination = H1StructuralParent.Node(H1TypedNode.Container("container:1"))
        model.beginAdd(destination)
        verify { writer wasNot Called }
        val returned = result(CanonicalEntityRef.Aspect("subject:1"))
        model.acceptPickerResult(returned)
        model.acceptPickerResult(returned)
        assertThat(model.state.value.busy).isTrue()
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.addAppearance(
            HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "subject:1"), destination, any(), any(),
        ) }
        assertThat(returned.get<List<String>>(CANONICAL_ENTITY_PICKER_RESULT_KEY)).isNull()
        assertThat(returned.get<Boolean>("bigPicturePickerPending")).isFalse()
    }

    @Test fun `stale unsolicited and malformed picker results never write`() = runTest(dispatcher) {
        val model = vm()
        val unsolicited = result(CanonicalEntityRef.Workspace("w"))
        unsolicited["bigPicturePickerPending"] = false
        model.acceptPickerResult(unsolicited)
        model.beginAdd(H1StructuralParent.Root)
        val malformed = SavedStateHandle(mapOf("bigPicturePickerPending" to true,
            CANONICAL_ENTITY_PICKER_RESULT_KEY to listOf("WRONG:x")))
        model.acceptPickerResult(malformed)
        assertThat(model.state.value.message).isNotNull()
        verify { writer wasNot Called }
    }

    @Test fun `all picker identities preserve type and root occurrence container destinations`() = runTest(dispatcher) {
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("p")
        val refs = listOf(CanonicalEntityRef.Orientation("o"), CanonicalEntityRef.Aspect("a"), CanonicalEntityRef.Workspace("w"))
        val destinations = listOf(H1StructuralParent.Root,
            H1StructuralParent.Node(H1TypedNode.Occurrence("parent")), H1StructuralParent.Node(H1TypedNode.Container("section")))
        refs.forEachIndexed { i, ref ->
            val model = vm()
            model.beginAdd(destinations[i])
            model.acceptPickerResult(result(ref))
            advanceUntilIdle()
            coVerify(exactly = 1) { writer.addAppearance(ref.toBigPictureTarget(), destinations[i], any(), any()) }
        }
    }

    @Test fun `captured destination survives recreation while picker is open`() = runTest(dispatcher) {
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("new")
        val saved = SavedStateHandle()
        val destination = H1StructuralParent.Node(H1TypedNode.Occurrence("parent:with:colons"))
        vm(saved).beginAdd(destination)
        val restored = vm(saved)
        val returned = result(CanonicalEntityRef.Workspace("w"))
        restored.acceptPickerResult(returned)
        advanceUntilIdle()
        vm(saved).acceptPickerResult(returned)
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.addAppearance(
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"), destination, any(), any(),
        ) }
        assertThat(saved.get<String>("bigPictureAddDestination")).isNull()
    }

    @Test fun `missing or corrupt saved destination never falls back to root`() = runTest(dispatcher) {
        listOf(null, "invalid").forEach { encoded ->
            val saved = SavedStateHandle(mapOf("bigPictureAddDestination" to encoded))
            val model = vm(saved)
            model.acceptPickerResult(result(CanonicalEntityRef.Orientation("o")))
            advanceUntilIdle()
            assertThat(model.state.value.message).isNotNull()
        }
        verify { writer wasNot Called }
    }

    @Test fun `cut restores exact occurrence not target and failed cyclic paste retains source`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val model = vm(saved)
        model.cut(H1TypedNode.Occurrence("occurrence:LINK"))
        val restored = vm(saved)
        val cyclic = H1StructuralParent.Node(H1TypedNode.Occurrence("descendant"))
        coEvery { writer.moveNode(any(), any(), any(), any()) } throws IllegalArgumentException("Cycle")
        restored.paste(cyclic)
        advanceUntilIdle()
        assertThat(restored.clipboard.value).isEqualTo("occurrence:occurrence:LINK")
        assertThat(restored.state.value.message).contains("Cycle")
        coEvery { writer.moveNode(any(), any(), any(), any()) } just Runs
        val destinations = listOf(H1StructuralParent.Root,
            H1StructuralParent.Node(H1TypedNode.Occurrence("other")), H1StructuralParent.Node(H1TypedNode.Container("section")))
        destinations.forEach { destination ->
            restored.cut(H1TypedNode.Occurrence("occurrence:LINK"))
            restored.paste(destination)
            advanceUntilIdle()
            coVerify(exactly = 1) { writer.moveNode(H1TypedNode.Occurrence("occurrence:LINK"), destination, null, any()) }
            assertThat(restored.clipboard.value).isNull()
        }
    }

    @Test fun `remove routes exact occurrence and container to separate H1 commands`() = runTest(dispatcher) {
        coEvery { writer.removeOccurrence(any(), any()) } just Runs
        coEvery { writer.deleteContainer(any(), any()) } just Runs
        val model = vm()
        model.remove(H1TypedNode.Occurrence("appearance-not-subject"))
        advanceUntilIdle()
        model.remove(H1TypedNode.Container("section-not-subject"))
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.removeOccurrence(PlacementId("appearance-not-subject"), any()) }
        coVerify(exactly = 1) { writer.deleteContainer("section-not-subject", any()) }
    }

    @Test fun `add group and mixed reorder forward complete typed command without semantic companion`() = runTest(dispatcher) {
        coEvery { writer.createContainer(any(), any(), any(), any(), any()) } returns "section"
        coEvery { writer.reorderChildren(any(), any(), any()) } just Runs
        val model = vm()
        val parent = H1StructuralParent.Node(H1TypedNode.Occurrence("parent"))
        listOf(H1StructuralParent.Root, parent, H1StructuralParent.Node(H1TypedNode.Container("section"))).forEach {
            model.addGroup("WIP", it)
            advanceUntilIdle()
            coVerify(exactly = 1) { writer.createContainer("WIP", it, null, any(), any()) }
        }
        val order = listOf(H1TypedNode.Container("x"), H1TypedNode.Occurrence("a"), H1TypedNode.Container("y"))
        model.reorder(parent, order)
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.reorderChildren(parent, order, any()) }
        confirmVerified(writer)
    }

    @Test fun `failed direct add exposes error without replaying consumed picker result`() = runTest(dispatcher) {
        coEvery { writer.addAppearance(any(), any(), any(), any()) } throws IllegalStateException("Deleted target")
        val model = vm()
        model.beginAdd(H1StructuralParent.Root)
        val returned = result(CanonicalEntityRef.Workspace("w"))
        model.acceptPickerResult(returned)
        assertThat(model.state.value.busy).isTrue()
        advanceUntilIdle()
        assertThat(model.state.value.busy).isFalse()
        assertThat(model.state.value.message).contains("Deleted target")
        model.acceptPickerResult(returned)
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.addAppearance(any(), any(), any(), any()) }
    }
}
