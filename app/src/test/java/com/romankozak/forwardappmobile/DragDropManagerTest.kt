package com.romankozak.forwardappmobile

import androidx.compose.ui.geometry.Offset
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd.DragDropManager
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd.LazyListInfoProvider
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DragDropManagerTest {

    private lateinit var dragDropManager: DragDropManager
    private val onMove: (Int, Int) -> Unit = mockk(relaxed = true)
    private val lazyListInfoProvider: LazyListInfoProvider = mockk(relaxed = true)
    private val testScope = TestCoroutineScope()

    @Before
    fun setUp() {
        dragDropManager = DragDropManager(testScope, lazyListInfoProvider, onMove)
    }

    @Test
    fun `onDragStart should update dragState`() {
        // Given
        val index = 0
        val offset = Offset(10f, 20f)

        // When
        dragDropManager.onDragStart(offset, index)

        // Then
        val state = dragDropManager.dragState.value
        assert(state.dragInProgress)
        assert(state.draggedItemIndex == index)
        assert(state.dragAmount == offset)
    }

    @Test
    fun `onDrag should update dragAmount`() {
        // Given
        dragDropManager.onDragStart(Offset.Zero, 0)
        val offset = Offset(10f, 20f)

        // When
        dragDropManager.onDrag(offset)

        // Then
        val state = dragDropManager.dragState.value
        assert(state.dragAmount == offset)
    }

    @Test
    fun `onDragEnd should call onMove and reset state`() {
        // Given
        dragDropManager.onDragStart(Offset.Zero, 0)
        dragDropManager.onDrag(Offset(0f, 100f)) // Move item

        // When
        dragDropManager.onDragEnd()

        // Then
        verify { onMove(any(), any()) }
        val state = dragDropManager.dragState.value
        assert(!state.dragInProgress)
        assert(state.draggedItemIndex == null)
        assert(state.dragAmount == Offset.Zero)
    }
}
