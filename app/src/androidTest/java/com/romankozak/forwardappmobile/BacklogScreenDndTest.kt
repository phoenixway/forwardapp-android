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
import com.romankozak.forwardappmobile.ui.screens.projectscreen.ProjectScreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BacklogScreenDndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDragAndDrop() {
        val viewModel: ProjectScreenViewModel = mock()
        val dragState = MutableStateFlow<DragState?>(null)
        whenever(viewModel.dragState).thenReturn(dragState)
        whenever(viewModel.obsidianVaultName).thenReturn(MutableStateFlow(""))
        whenever(viewModel.contextMarkerToEmojiMap).thenReturn(MutableStateFlow(emptyMap()))
        whenever(viewModel.currentProjectContextEmojiToHide).thenReturn(MutableStateFlow(null))
        whenever(viewModel.subprojectChildren).thenReturn(MutableStateFlow(emptyMap()))

        val items = (1..10).map {
            ListItemContent.GoalItem(
                goal = Goal(id = it, title = "Goal $it"),
                listItem = ListItem(id = it, parentId = 0, type = "goal"),
                reminders = emptyList()
            )
        }

        composeTestRule.setContent {
            val listState = rememberLazyListState()

            BacklogView(
                viewModel = viewModel,
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

        // Simulate drag
        dragState.value = DragState(1, 1, 4, 0f, null)

        composeTestRule.onNodeWithText("Goal 5").performGesture {
            up()
        }

        verify(viewModel).moveItem(1, 4)
    }
}
