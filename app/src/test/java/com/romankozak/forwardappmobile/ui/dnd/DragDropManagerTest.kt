package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.ui.geometry.Offset
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DragDropManagerTest {

    private lateinit var dragDropManager: DragDropManager
    private val onMove: (Int, Int) -> Unit = mockk(relaxed = true)
    private val scrollBy: suspend (Float) -> Unit = mockk(relaxed = true)
    private val lazyListInfoProvider: LazyListInfoProvider = mockk(relaxed = true)
    private val testScope = TestScope()

    @Before
    fun setUp() {
        dragDropManager = DragDropManager(
            scope = testScope,
            lazyListInfoProvider = lazyListInfoProvider,
            onMove = onMove,
            scrollBy = scrollBy
        )
    }

    @Test
    fun `onDragStart updates the drag state`() = testScope.runTest {
        // Given
        val index = 0
        val offset = Offset(10f, 20f)
        val itemInfo = mockk<LazyListItemInfo> {
            every { size } returns 100
            every { index } returns 0
        }
        every { lazyListInfoProvider.lazyListItemInfo } returns listOf(itemInfo)


        // When
        dragDropManager.onDragStart(offset, index)

        // Then
        val state = dragDropManager.dragState.value
        assert(state.dragInProgress)
        assert(state.draggedItemIndex == index)
        assert(state.dragAmount == offset)
    }

    @Test
    fun `onDrag updates the drag amount`() = testScope.runTest {
        // Given
        val startOffset = Offset(10f, 20f)
        val dragOffset = Offset(5f, 15f)
        dragDropManager.onDragStart(startOffset, 0)

        // When
        dragDropManager.onDrag(dragOffset)

        // Then
        val state = dragDropManager.dragState.value
        assert(state.dragAmount == startOffset + dragOffset)
    }

    @Test
    fun `onDragEnd calls onMove and resets the state`() = testScope.runTest {
        // Given
        val from = 0
        val to = 1
        dragDropManager.onDragStart(Offset.Zero, from)
        // Simulate drag to a new target
        val itemInfo1 = mockk<LazyListItemInfo> {
            every { size } returns 100
            every { index } returns 0
            every { offset } returns 0
        }
        val itemInfo2 = mockk<LazyListItemInfo> {
            every { size } returns 100
            every { index } returns 1
            every { offset } returns 100
        }
        every { lazyListInfoProvider.lazyListItemInfo } returns listOf(itemInfo1, itemInfo2)
        dragDropManager.onDrag(Offset(0f, 110f))


        // When
        dragDropManager.onDragEnd()

        // Then
        verify { onMove(from, to) }
        val state = dragDropManager.dragState.value
        assert(!state.dragInProgress)
        assert(state.draggedItemIndex == null)
        assert(state.targetItemIndex == null)
        assert(state.dragAmount == Offset.Zero)
    }
}