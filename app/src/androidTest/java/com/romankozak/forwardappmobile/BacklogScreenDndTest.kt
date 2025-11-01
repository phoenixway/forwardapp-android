package com.romankozak.forwardappmobile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.UiState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.BacklogView
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class BacklogScreenDndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDragAndDrop() {
        val mockViewModel: BacklogViewModel = mock()
        val items = (1..10).map {
            val goalId = "$it"
            ListItemContent.GoalItem(
                goal = Goal(id = goalId, text = "Goal $it", completed = false, createdAt = 0L, updatedAt = 0L),
                listItem = ListItem(id = "$it", projectId = "0", itemType = "goal", order = it.toLong(), entityId = goalId),
                reminders = emptyList()
            )
        }

        whenever(mockViewModel.listContent).thenReturn(MutableStateFlow(items))
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(UiState()))
        whenever(mockViewModel.dragState).thenReturn(MutableStateFlow(com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd.DragState()))

        composeTestRule.setContent {
            val listState = rememberLazyListState()

            BacklogView(
                viewModel = mockViewModel,
                uiState = UiState(),
                listState = listState,
                listContent = items,
                isAttachmentsExpanded = false,
                swipeEnabled = false
            )
        }

        composeTestRule.onNodeWithText("Goal 2").performGesture {
            longClick()
        }

        // This is a simplified simulation of drag and drop.
        // In a real scenario, you would need to dispatch touch events.
        // For this test, we will directly call the viewmodel functions.

        val fromIndex = 1
        val toIndex = 4

        // Simulate drag start
        mockViewModel.onDragStart(androidx.compose.ui.geometry.Offset.Zero, fromIndex)

        // Simulate drag
        mockViewModel.onDrag(androidx.compose.ui.geometry.Offset(0f, 100f))

        // Simulate drag end
        mockViewModel.onDragEnd()

        verify(mockViewModel).moveItem(fromIndex, toIndex)
    }
}