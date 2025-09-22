

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.ListItemType
import com.romankozak.forwardappmobile.ui.screens.daymanagement.CompactDayPlanHeader
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.DayTaskAsGoalItem
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.DayTaskAsSublistItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.EnhancedCustomCheckbox
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTask>,
    dayPlan: DayPlan?,
    onToggleTask: (String) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onTasksReordered: (List<DayTask>) -> Unit,
    onSublistClick: (projectId: String) -> Unit,
    
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalTasks by remember { mutableStateOf(tasks) }
    val lazyListState = rememberLazyListState()

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(60_000L)
            }
        }.collect { currentTime = it }
    }

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalTasks = internalTasks.toMutableList().apply { add(to.index, removeAt(from.index)) }
            onTasksReordered(internalTasks)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    LaunchedEffect(tasks) { internalTasks = tasks }

    
    Column(modifier = modifier) {
        
        val completedTasks = tasks.count { it.completed }
        val totalTasks = tasks.size

        
        CompactDayPlanHeader(
            dayPlan = dayPlan,
            completedTasks = completedTasks,
            totalTasks = totalTasks,
            onNavigateToPreviousDay = onNavigateToPreviousDay,
            onNavigateToNextDay = onNavigateToNextDay,
            isNextDayNavigationEnabled = isNextDayNavigationEnabled,
        )

        
        LazyColumn(state = lazyListState) {
            items(items = internalTasks, key = { task -> task.id }) { task ->
                ReorderableItem(reorderableLazyListState, key = task.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "")

                    Surface(
                        shadowElevation = elevation,
                        modifier =
                            Modifier.combinedClickable(
                                onClick = {
                                    
                                    if (task.taskType == ListItemType.SUBLIST) {
                                        task.projectId?.let { onSublistClick(it) }
                                    }
                                },
                                onLongClick = { onTaskLongPress(task) },
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        ) {
                            EnhancedCustomCheckbox(
                                checked = task.completed,
                                onCheckedChange = { onToggleTask(task.id) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                
                                when (task.taskType) {
                                    ListItemType.SUBLIST -> DayTaskAsSublistItem(task, currentTime)
                                    else -> DayTaskAsGoalItem(task, currentTime)
                                }
                            }
                            IconButton(
                                modifier = Modifier.draggableHandle(),
                                onClick = {},
                            ) {
                                Icon(Icons.Rounded.DragHandle, contentDescription = "Перетягнути")
                            }
                        }
                    }
                }
            }
        }
    }
}
