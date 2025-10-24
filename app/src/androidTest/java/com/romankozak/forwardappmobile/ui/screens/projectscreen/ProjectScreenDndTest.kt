package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.DraggableItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectScreenDndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDragAndDrop() {
        val items = mutableStateListOf(
            *List(5) { i -> // Reduced item count
                val goalId = (i + 1).toString()
                ListItemContent.GoalItem(
                    goal = Goal(
                        id = goalId,
                        text = "Item ${i + 1}",
                        completed = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = null
                    ),
                    reminders = emptyList(),
                    listItem = ListItem(
                        id = goalId,
                        projectId = "testProject",
                        itemType = ListItemTypeValues.GOAL,
                        entityId = goalId,
                        order = i.toLong()
                    )
                )
            }.toTypedArray()
        )

        composeTestRule.setContent {
            val lazyListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            val dragDropState = remember {
                SimpleDragDropState(
                    state = lazyListState,
                    scope = coroutineScope,
                    onMove = { from, to ->
                        Log.d("DND_TEST", "Moving from $from to $to")
                        items.add(to, items.removeAt(from))
                    }
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items, key = { _, item -> item.listItem.id }) { index, item ->
                    DraggableItem(
                        item = item,
                        index = index,
                        dragDropState = dragDropState
                    ) {
                        Text(text = (item as ListItemContent.GoalItem).goal.text)
                    }
                }
            }
        }

        val initialList = items.map { (it as ListItemContent.GoalItem).goal.text }
        Log.d("DND_TEST", "Initial list: $initialList")

        // Drag Item 2 down to position 3
        composeTestRule.onNodeWithText("Item 2").performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Item 2").performTouchInput {
            // Dragging by 2 item heights (approx)
            moveBy(Offset(0f, 300f), 1000)
        }
        composeTestRule.onNodeWithText("Item 2").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        val finalList = items.map { (it as ListItemContent.GoalItem).goal.text }
        Log.d("DND_TEST", "Final list: $finalList")

        val expectedList = listOf("Item 1", "Item 3", "Item 4", "Item 2", "Item 5")
        assertEquals(expectedList, finalList)
    }
}