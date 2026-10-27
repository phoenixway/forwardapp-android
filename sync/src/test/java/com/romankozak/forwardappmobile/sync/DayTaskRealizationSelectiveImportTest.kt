package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class DayTaskRealizationSelectiveImportTest {
    @Test fun `existing day closure retains task and exact same-day priority endpoint`() {
        val day = DayPlan(id = "day", date = 1)
        val priority = DayFocusItem("focus", day.id, "Priority")
        val task = DayTask(id = "task", dayPlanId = day.id, title = "Task", realizedDayFocusItemIds = listOf(priority.id))
        val source = SnapshotBundle(version = 2, dayPlans = listOf(day.toSnapshot()),
            dayFocusItems = listOf(priority.toSnapshot()), dayTasks = listOf(task.toSnapshot()))
        val result = SnapshotBundleSelectiveImportFilter().filter(source, WorkspaceSelectiveImportSelection())
        assertEquals(source.dayPlans, result.dayPlans)
        assertEquals(source.dayTasks, result.dayTasks)
        assertEquals(source.dayFocusItems, result.dayFocusItems)
        assertEquals(listOf(priority.id), result.dayTasks.single().realizedDayFocusItemIds)
    }
}
