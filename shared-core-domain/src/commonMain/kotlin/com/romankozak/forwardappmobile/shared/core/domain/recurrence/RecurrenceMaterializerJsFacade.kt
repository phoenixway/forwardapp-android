@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.CanonicalDayDatabase
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusItem
import com.romankozak.forwardappmobile.shared.core.models.day.DayPlan
import com.romankozak.forwardappmobile.shared.core.models.day.DayTask
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JavaScript boundary to the real canonical recurrence materializer.
 *
 * This facade only assembles the canonical database container and converts
 * Kotlin collections at the JS boundary. Materialization semantics remain in
 * planRecurringSeriesForDay.
 */
@JsExport
@JsName("planRecurringSeriesForDay")
fun planRecurringSeriesForDayForJs(
    dayPlans: Array<DayPlan>,
    dayTasks: Array<DayTask>,
    dayFocusItems: Array<DayFocusItem>,
    recurringSeries: Array<RecurringSeries>,
    dayKey: String,
    now: Double,
): RecurrenceMaterializationPlan =
    planRecurringSeriesForDay(
        database =
            CanonicalDayDatabase(
                dayPlans = dayPlans.toList(),
                dayTasks = dayTasks.toList(),
                dayFocusItems = dayFocusItems.toList(),
                recurringSeries = recurringSeries.toList(),
                dayManagementRuntimeState = null,
            ),
        dayKey = requireLocalDayKey(dayKey),
        now = requireJsSafeIntegerLong(now, "materialization.now"),
    )

@JsExport
@JsName("materializationPlanStatus")
fun materializationPlanStatusForJs(
    plan: RecurrenceMaterializationPlan,
): String = plan.status.name

@JsExport
@JsName("materializationPlanTasks")
fun materializationPlanTasksForJs(
    plan: RecurrenceMaterializationPlan,
): Array<DayTask> =
    plan.tasksToCreate.toTypedArray()

@JsExport
@JsName("materializationPlanFocusItems")
fun materializationPlanFocusItemsForJs(
    plan: RecurrenceMaterializationPlan,
): Array<DayFocusItem> =
    plan.focusItemsToCreate.toTypedArray()

@JsExport
@JsName("materializationPlanSkippedExistingOccurrenceKeys")
fun materializationPlanSkippedExistingOccurrenceKeysForJs(
    plan: RecurrenceMaterializationPlan,
): Array<String> =
    plan.skippedExistingOccurrenceKeys.toTypedArray()

@JsExport
@JsName("dayTaskLinkedProjectIds")
fun dayTaskLinkedProjectIdsForJs(
    task: DayTask,
): Array<String> =
    task.linkedProjectIds.toTypedArray()

@JsExport
@JsName("dayTaskLinkedAttachmentIds")
fun dayTaskLinkedAttachmentIdsForJs(
    task: DayTask,
): Array<String> =
    task.linkedAttachmentIds.toTypedArray()

@JsExport
@JsName("dayTaskTags")
fun dayTaskTagsForJs(
    task: DayTask,
): Array<String> =
    task.tags.toTypedArray()

@JsExport
@JsName("dayFocusItemRelatedLinks")
fun dayFocusItemRelatedLinksForJs(
    item: DayFocusItem,
): Array<com.romankozak.forwardappmobile.shared.core.models.link.CanonicalRelatedLink> =
    item.relatedLinks.toTypedArray()
