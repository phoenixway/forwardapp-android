package com.romankozak.forwardappmobile

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.ui.geometry.Offset
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd.DragDropManager
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd.LazyListInfoProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test

@ExperimentalCoroutinesApi
class DragDropManagerTest {

    private val onMove: (Int, Int) -> Unit = mockk(relaxed = true)
    private val lazyListInfoProvider: LazyListInfoProvider = mockk(relaxed = true)

    @Test
    fun `onDragStart should update dragState`() = runTest {
        // Given
        val dragDropManager = DragDropManager(this, lazyListInfoProvider, onMove)
        val index = 0
        val offset = Offset(10f, 20f)

        // When
        dragDropManager.onDragStart(offset, index)
        advanceUntilIdle()

        // Then
        val state = dragDropManager.dragState.value
        assert(state.dragInProgress)
        assert(state.draggedItemIndex == index)
        assert(state.dragAmount == offset)
    }

    @Test
    fun `onDrag should update dragAmount`() = runTest {
        // Given
        val dragDropManager = DragDropManager(this, lazyListInfoProvider, onMove)
        dragDropManager.onDragStart(Offset.Zero, 0)
        advanceUntilIdle()
        val offset = Offset(10f, 20f)

        // When
        dragDropManager.onDrag(offset)
        advanceUntilIdle()

        // Then
        val state = dragDropManager.dragState.value
        assert(state.dragAmount == offset)
    }

    @Test
    fun `onDragEnd should call onMove and reset state`() = runTest {
        // Given
        val dragDropManager = DragDropManager(this, lazyListInfoProvider, onMove)
        dragDropManager.onDragStart(Offset.Zero, 0)
        advanceUntilIdle()
        dragDropManager.onDrag(Offset(0f, 100f)) // Move item
        advanceUntilIdle()

        // When
        dragDropManager.onDragEnd()
        advanceUntilIdle()

        // Then
        verify { onMove(any(), any()) }
        val state = dragDropManager.dragState.value
        assert(!state.dragInProgress)
        assert(state.draggedItemIndex == null)
        assert(state.dragAmount == Offset.Zero)
    }

    @Test
    fun `onDragEnd should not call onMove if item is not moved`() = runTest {
        // Given
        val dragDropManager = DragDropManager(this, lazyListInfoProvider, onMove)
        dragDropManager.onDragStart(Offset.Zero, 0)
        advanceUntilIdle()

        // When
        dragDropManager.onDragEnd()
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { onMove(any(), any()) }
    }

    @Test
    fun `dragging first item down should call onMove with correct parameters`() = runTest {
        // Given
        val items = createLazyListItemInfo(5)
        every { lazyListInfoProvider.lazyListItemInfo } returns items
        val dragDropManager = DragDropManager(this, lazyListInfoProvider, onMove)

        // When
        dragDropManager.onDragStart(Offset.Zero, 0)
        advanceUntilIdle()
        dragDropManager.onDrag(Offset(0f, 150f)) // Drag down past the second item
        advanceUntilIdle()
        dragDropManager.onDragEnd()
        advanceUntilIdle()

        // Then
        verify { onMove(0, 1) }
    }

    @Test
    fun `dragging last item up should call onMove with correct parameters`() = runTest {
        // Given
        val items = createLazyListItemInfo(5)
        every { lazyListInfoProvider.lazyListItemInfo } returns items
        val dragDropManager = DragDropManager(this, lazyListInfoProvider, onMove)

        // When
        dragDropManager.onDragStart(Offset.Zero, 4)
        advanceUntilIdle()
        dragDropManager.onDrag(Offset(0f, -150f)) // Drag up past the fourth item
        advanceUntilIdle()
        dragDropManager.onDragEnd()
        advanceUntilIdle()

        // Then
        verify { onMove(4, 3) }
    }

    private fun createLazyListItemInfo(count: Int): List<LazyListItemInfo> {
        return (0 until count).map {
            mockk<LazyListItemInfo>().apply {
                every { index } returns it
                every { offset } returns it * 100
                every { size } returns 100
            }
        }
    }
}