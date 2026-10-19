package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.toBigPictureTarget
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntityLibraryPlacementViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val writer = mockk<H1StructuralMutationCoordinator>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `all canonical classes delegate identity and typed destination without deciding role`() = runTest(dispatcher) {
        val refs = listOf(CanonicalEntityRef.Orientation("same"), CanonicalEntityRef.Aspect("same"), CanonicalEntityRef.Workspace("same"))
        val parents = listOf(H1StructuralParent.Root, H1StructuralParent.Node(H1TypedNode.Occurrence("p")),
            H1StructuralParent.Node(H1TypedNode.Container("historical-group:v1:g")))
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("new")
        refs.zip(parents).forEach { (ref, parent) ->
            val vm = EntityLibraryPlacementViewModel(writer, SavedStateHandle())
            vm.begin(ref)
            vm.place(parent)
            vm.place(parent) // Double tap must not create another appearance.
            assertThat(vm.state.value.busy).isTrue()
            advanceUntilIdle()
            coVerify(exactly = 1) { writer.addAppearance(ref.toBigPictureTarget(), parent, any(), any()) }
            assertThat(vm.state.value.pendingRef).isNull()
            assertThat(vm.state.value.message).isEqualTo("Додано до Big Picture")
        }
    }

    @Test fun `pending identity restores without automatically replaying a mutation`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val ref = CanonicalEntityRef.Workspace("workspace:/exact")
        EntityLibraryPlacementViewModel(writer, saved).begin(ref)
        val restored = EntityLibraryPlacementViewModel(writer, saved)
        advanceUntilIdle()
        assertThat(restored.state.value.pendingRef).isEqualTo(ref)
        verify { writer wasNot Called }
        restored.cancel()
        assertThat(EntityLibraryPlacementViewModel(writer, saved).state.value.pendingRef).isNull()
    }

    @Test fun `failed placement retains identity for retry and success clears recoverable intent`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val vm = EntityLibraryPlacementViewModel(writer, saved)
        val ref = CanonicalEntityRef.Aspect("a")
        coEvery { writer.addAppearance(any(), any(), any(), any()) } throws IllegalArgumentException("Destination deleted")
        vm.begin(ref)
        vm.place(H1StructuralParent.Node(H1TypedNode.Container("deleted")))
        advanceUntilIdle()
        assertThat(vm.state.value.busy).isFalse()
        assertThat(vm.state.value.error).contains("Destination deleted")
        assertThat(EntityLibraryPlacementViewModel(writer, saved).state.value.pendingRef).isEqualTo(ref)
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("p")
        vm.place(H1StructuralParent.Root)
        advanceUntilIdle()
        assertThat(EntityLibraryPlacementViewModel(writer, saved).state.value.pendingRef).isNull()
    }

    @Test fun `cancel and unsolicited destination never write`() = runTest(dispatcher) {
        val vm = EntityLibraryPlacementViewModel(writer, SavedStateHandle())
        vm.place(H1StructuralParent.Root)
        vm.begin(CanonicalEntityRef.Orientation("o"))
        vm.cancel()
        vm.place(H1StructuralParent.Root)
        advanceUntilIdle()
        verify { writer wasNot Called }
    }

    @Test fun `unknown saved identity fails closed without defaulting to another entity`() = runTest(dispatcher) {
        val vm = EntityLibraryPlacementViewModel(writer, SavedStateHandle(mapOf("libraryBigPicturePendingRef" to "INVALID:x")))
        assertThat(vm.state.value.pendingRef).isNull()
        assertThat(vm.state.value.message).isNotNull()
        vm.place(H1StructuralParent.Root)
        advanceUntilIdle()
        verify { writer wasNot Called }
    }
}
