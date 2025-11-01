package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListItemInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DnDVisualsManagerTest {

    private lateinit var lazyListInfoProvider: LazyListInfoProvider
    private lateinit var dndVisualsManager: DnDVisualsManager

    @Before
    fun setup() {
        lazyListInfoProvider = mockk(relaxed = true)
        dndVisualsManager = DnDVisualsManager(lazyListInfoProvider)
    }

    @Test
    fun `calculateDnDVisualState returns default when drag not in progress`() {
        val dragAndDropState = DragAndDropState(dragInProgress = false)
        val result = dndVisualsManager.calculateDnDVisualState(dragAndDropState)
        assertEquals(DnDVisualState(isDragging = false), result)
    }

    @Test
    fun `calculateDnDVisualState returns default when draggedItemIndex is null`() {
        val dragAndDropState = DragAndDropState(dragInProgress = true, draggedItemIndex = null)
        val result = dndVisualsManager.calculateDnDVisualState(dragAndDropState)
        assertEquals(DnDVisualState(isDragging = false), result)
    }

    @Test
    fun `calculateDnDVisualState calculates correct offsets for dragging item down`() {
        // Given
        val draggedItemIndex = 0
        val targetItemIndex = 1
        val draggedItemHeight = 100f
        val dragAmount = Offset(0f, 50f)

        val itemInfo0 = mockk<LazyListItemInfo> { every { index } returns 0; every { size } returns 100 }
        val itemInfo1 = mockk<LazyListItemInfo> { every { index } returns 1; every { size } returns 100 }
        val itemInfo2 = mockk<LazyListItemInfo> { every { index } returns 2; every { size } returns 100 }

        every { lazyListInfoProvider.lazyListItemInfo } returns listOf(itemInfo0, itemInfo1, itemInfo2)

        val dragAndDropState = DragAndDropState(
            dragInProgress = true,
            draggedItemIndex = draggedItemIndex,
            targetItemIndex = targetItemIndex,
            dragAmount = dragAmount
        )

        // When
        val result = dndVisualsManager.calculateDnDVisualState(dragAndDropState)

        // Then
        assertEquals(true, result.isDragging)
        assertEquals(draggedItemHeight, result.draggedItemHeight)
        assertEquals(dragAmount.y, result.itemOffsets[draggedItemIndex])
        assertEquals(-draggedItemHeight, result.itemOffsets[targetItemIndex])
        assertEquals(0f, result.itemOffsets[2])
    }

    @Test
    fun `calculateDnDVisualState calculates correct offsets for dragging item up`() {
        // Given
        val draggedItemIndex = 2
        val targetItemIndex = 1
        val draggedItemHeight = 100f
        val dragAmount = Offset(0f, -50f)

        val itemInfo0 = mockk<LazyListItemInfo> { every { index } returns 0; every { size } returns 100 }
        val itemInfo1 = mockk<LazyListItemInfo> { every { index } returns 1; every { size } returns 100 }
        val itemInfo2 = mockk<LazyListItemInfo> { every { index } returns 2; every { size } returns 100 }

        every { lazyListInfoProvider.lazyListItemInfo } returns listOf(itemInfo0, itemInfo1, itemInfo2)

        val dragAndDropState = DragAndDropState(
            dragInProgress = true,
            draggedItemIndex = draggedItemIndex,
            targetItemIndex = targetItemIndex,
            dragAmount = dragAmount
        )

        // When
        val result = dndVisualsManager.calculateDnDVisualState(dragAndDropState)

        // Then
        assertEquals(true, result.isDragging)
        assertEquals(draggedItemHeight, result.draggedItemHeight)
        assertEquals(dragAmount.y, result.itemOffsets[draggedItemIndex])
        assertEquals(draggedItemHeight, result.itemOffsets[targetItemIndex])
        assertEquals(0f, result.itemOffsets[0])
    }

    @Test
    fun `calculateDnDVisualState handles no target index`() {
        // Given
        val draggedItemIndex = 0
        val draggedItemHeight = 100f
        val dragAmount = Offset(0f, 50f)

        val itemInfo0 = mockk<LazyListItemInfo> { every { index } returns 0; every { size } returns 100 }
        val itemInfo1 = mockk<LazyListItemInfo> { every { index } returns 1; every { size } returns 100 }

        every { lazyListInfoProvider.lazyListItemInfo } returns listOf(itemInfo0, itemInfo1)

        val dragAndDropState = DragAndDropState(
            dragInProgress = true,
            draggedItemIndex = draggedItemIndex,
            targetItemIndex = null,
            dragAmount = dragAmount
        )

        // When
        val result = dndVisualsManager.calculateDnDVisualState(dragAndDropState)

        // Then
        assertEquals(true, result.isDragging)
        assertEquals(draggedItemHeight, result.draggedItemHeight)
        assertEquals(dragAmount.y, result.itemOffsets[draggedItemIndex])
        assertEquals(0f, result.itemOffsets[1]) // No offset for other items if no target
    }

    @Test
    fun `calculateDnDVisualState handles target index less than 0`() {
        // Given
        val draggedItemIndex = 0
        val targetItemIndex = -1
        val draggedItemHeight = 100f
        val dragAmount = Offset(0f, 50f)

        val itemInfo0 = mockk<LazyListItemInfo> { every { index } returns 0; every { size } returns 100 }
        val itemInfo1 = mockk<LazyListItemInfo> { every { index } returns 1; every { size } returns 100 }

        every { lazyListInfoProvider.lazyListItemInfo } returns listOf(itemInfo0, itemInfo1)

        val dragAndDropState = DragAndDropState(
            dragInProgress = true,
            draggedItemIndex = draggedItemIndex,
            targetItemIndex = targetItemIndex,
            dragAmount = dragAmount
        )

        // When
        val result = dndVisualsManager.calculateDnDVisualState(dragAndDropState)

        // Then
        assertEquals(true, result.isDragging)
        assertEquals(draggedItemHeight, result.draggedItemHeight)
        assertEquals(dragAmount.y, result.itemOffsets[draggedItemIndex])
        assertEquals(0f, result.itemOffsets[1]) // No offset for other items if target is invalid
    }
}