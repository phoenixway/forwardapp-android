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

    @Test fun `picker allows all three classes and consumes identity only once before explicit destination`() = runTest(dispatcher) {
        val model = vm()
        assertThat(model.pickerRequest().allowedClasses).containsExactlyElementsIn(CanonicalEntityClass.entries)
        val returned = result(CanonicalEntityRef.Aspect("subject:1"))
        model.acceptPickerResult(returned)
        model.acceptPickerResult(returned)
        assertThat(model.pendingEntity.value).isEqualTo("ASPECT:subject:1")
        verify { writer wasNot Called }
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("new-occurrence")
        val destination = H1StructuralParent.Node(H1TypedNode.Container("container:1"))
        model.addEntity(destination)
        model.addEntity(destination) // Guard rapid duplicate tap.
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.addAppearance(
            HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "subject:1"), destination, any(), any(),
        ) }
        assertThat(model.pendingEntity.value).isNull()
    }

    @Test fun `stale unsolicited and malformed picker results never write`() = runTest(dispatcher) {
        val model = vm()
        val unsolicited = result(CanonicalEntityRef.Workspace("w"))
        unsolicited["bigPicturePickerPending"] = false
        model.acceptPickerResult(unsolicited)
        val malformed = SavedStateHandle(mapOf("bigPicturePickerPending" to true,
            CANONICAL_ENTITY_PICKER_RESULT_KEY to listOf("WRONG:x")))
        model.acceptPickerResult(malformed)
        assertThat(model.pendingEntity.value).isNull()
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
            model.acceptPickerResult(result(ref))
            model.addEntity(destinations[i])
            advanceUntilIdle()
            coVerify(exactly = 1) { writer.addAppearance(ref.toBigPictureTarget(), destinations[i], any(), any()) }
        }
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
        model.addGroup("WIP", parent)
        advanceUntilIdle()
        val order = listOf(H1TypedNode.Container("x"), H1TypedNode.Occurrence("a"), H1TypedNode.Container("y"))
        model.reorder(parent, order)
        advanceUntilIdle()
        coVerify(exactly = 1) { writer.createContainer("WIP", parent, null, any(), any()) }
        coVerify(exactly = 1) { writer.reorderChildren(parent, order, any()) }
        confirmVerified(writer)
    }

    @Test fun `failed add preserves selected entity and exposes error for retry`() = runTest(dispatcher) {
        coEvery { writer.addAppearance(any(), any(), any(), any()) } throws IllegalStateException("Deleted target")
        val model = vm()
        model.acceptPickerResult(result(CanonicalEntityRef.Workspace("w")))
        model.addEntity(H1StructuralParent.Root)
        assertThat(model.state.value.busy).isTrue()
        advanceUntilIdle()
        assertThat(model.state.value.busy).isFalse()
        assertThat(model.state.value.message).contains("Deleted target")
        assertThat(model.pendingEntity.value).isEqualTo("WORKSPACE:w")
    }
}
