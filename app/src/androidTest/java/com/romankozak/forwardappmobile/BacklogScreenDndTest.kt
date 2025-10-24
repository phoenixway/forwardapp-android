package com.romankozak.forwardappmobile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.UiState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.BacklogView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class BacklogScreenDndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDragAndDrop() {
        val mockOnMove: (Int, Int) -> Unit = mock()

        val items = (1..10).map {
            ListItemContent.GoalItem(
                goal = Goal(id = it, title = "Goal $it"),
                listItem = ListItem(id = it, parentId = 0, type = "goal"),
                reminders = emptyList()
            )
        }

        composeTestRule.setContent {
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val dragDropState = remember {
                SimpleDragDropState(
                    state = listState,
                    scope = scope,
                    onMove = mockOnMove
                )
            }

            BacklogView(
                viewModel = mock(),
                uiState = UiState(),
                listState = listState,
                dragDropState = dragDropState,
                listContent = items,
                isAttachmentsExpanded = false,
                swipeEnabled = false
            )
        }

        composeTestRule.onNodeWithText("Goal 2").performGesture {
            longClick()
        }

        composeTestRule.onNodeWithText("Goal 5").performGesture {
            moveTo(center)
        }

        composeTestRule.onNodeWithText("Goal 5").performGesture {
            up()
        }

        verify(mockOnMove).invoke(1, 4)
    }
}