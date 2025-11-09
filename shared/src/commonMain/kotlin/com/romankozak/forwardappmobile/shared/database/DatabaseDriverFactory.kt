package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Platform-specific configuration needed to create a SQLDelight driver.
 */
expect abstract class PlatformContext

/**
 * Factory that creates a platform-specific SQLDelight driver.
 *
 * A `PlatformContext` can provide additional information (for example, the Android `Context`).
 */
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

private val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long) = databaseValue != 0L
    override fun encode(value: Boolean) = if (value) 1L else 0L
}

private val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long) = databaseValue.toInt()
    override fun encode(value: Int) = value.toLong()
}

private val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

private val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double) = databaseValue
    override fun encode(value: Double) = value
}

private val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String) =
        if (databaseValue.isEmpty()) listOf() else databaseValue.split(",")

    override fun encode(value: List<String>) = value.joinToString(separator = ",")
}

private val relatedLinksListAdapter = object : ColumnAdapter<RelatedLinkList, String> {
    override fun decode(databaseValue: String): RelatedLinkList {
        return if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            Json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
        }
    }

    override fun encode(value: RelatedLinkList): String {
        return Json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}

private val customMetricsAdapter = object : ColumnAdapter<Map<String, Double>, String> {
    override fun decode(databaseValue: String): Map<String, Double> {
        return Json.decodeFromString(databaseValue)
    }

    override fun encode(value: Map<String, Double>): String {
        return Json.encodeToString(value)
    }
}

private fun <T : Enum<T>> EnumColumnAdapter(valueOf: (String) -> T) = object : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String): T = valueOf(databaseValue)
    override fun encode(value: T): String = value.name
}

private val taskPriorityAdapter: ColumnAdapter<TaskPriority, String> = EnumColumnAdapter(TaskPriority::valueOf)
private val taskStatusAdapter: ColumnAdapter<TaskStatus, String> = EnumColumnAdapter(TaskStatus::valueOf)
private val dayStatusAdapter: ColumnAdapter<DayStatus, String> = EnumColumnAdapter(DayStatus::valueOf)
private val recurrenceFrequencyAdapter: ColumnAdapter<RecurrenceFrequency, String> = EnumColumnAdapter(RecurrenceFrequency::valueOf)
private val projectTypeAdapter: ColumnAdapter<ProjectType, String> = EnumColumnAdapter(ProjectType::valueOf)
private val reservedGroupAdapter: ColumnAdapter<ReservedGroup, String> = EnumColumnAdapter(ReservedGroup::valueOf)

fun createForwardAppDatabase(
    driverFactory: DatabaseDriverFactory,
): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driverFactory.createDriver(),
        ActivityRecordsAdapter = ActivityRecords.Adapter(
            relatedLinksAdapter = relatedLinksListAdapter
        ),
        InboxRecordsAdapter = InboxRecords.Adapter(),
        ListItemsAdapter = ListItems.Adapter(),
        DayPlansAdapter = DayPlans.Adapter(statusAdapter = dayStatusAdapter),
        DayTasksAdapter = DayTasks.Adapter(
            priorityAdapter = taskPriorityAdapter,
            statusAdapter = taskStatusAdapter,
            tagsAdapter = stringListAdapter,
            completedAdapter = booleanAdapter
        ),
        GoalsAdapter = Goals.Adapter(
            completedAdapter = booleanAdapter,
            createdAtAdapter = longAdapter,
            updatedAtAdapter = longAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            valueImportanceAdapter = doubleAdapter,
            valueImpactAdapter = doubleAdapter,
            effortAdapter = doubleAdapter,
            costAdapter = doubleAdapter,
            riskAdapter = doubleAdapter,
            weightEffortAdapter = doubleAdapter,
            weightCostAdapter = doubleAdapter,
            weightRiskAdapter = doubleAdapter,
            rawScoreAdapter = doubleAdapter,
            displayScoreAdapter = intAdapter,
            parentValueImportanceAdapter = doubleAdapter,
            impactOnParentGoalAdapter = doubleAdapter,
            timeCostAdapter = doubleAdapter,
            financialCostAdapter = doubleAdapter
        ),
        NoteDocumentsAdapter = NoteDocuments.Adapter(),
        NoteDocumentItemsAdapter = NoteDocumentItems.Adapter(
            isCompletedAdapter = booleanAdapter
        ),
        NotesAdapter = Notes.Adapter(),
        ChecklistsAdapter = Checklists.Adapter(),
        ChecklistItemsAdapter = ChecklistItems.Adapter(
            isCheckedAdapter = booleanAdapter
        ),
        AttachmentsAdapter = Attachments.Adapter(),
        ProjectAttachmentCrossRefAdapter = ProjectAttachmentCrossRef.Adapter(),
        RecurringTasksAdapter = RecurringTasks.Adapter(
            priorityAdapter = taskPriorityAdapter,
            frequencyAdapter = recurrenceFrequencyAdapter,
            daysOfWeekAdapter = stringListAdapter
        ),
        ProjectsAdapter = Projects.Adapter(
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            isExpandedAdapter = booleanAdapter,
            isAttachmentsExpandedAdapter = booleanAdapter,
            isCompletedAdapter = booleanAdapter,
            isProjectManagementEnabledAdapter = booleanAdapter,
            showCheckboxesAdapter = booleanAdapter,
            projectTypeAdapter = projectTypeAdapter,
            reservedGroupAdapter = reservedGroupAdapter
        ),
        ProjectExecutionLogsAdapter = ProjectExecutionLogs.Adapter(),
        ConversationFoldersAdapter = ConversationFolders.Adapter(),
        DailyMetricsAdapter = DailyMetrics.Adapter(
            dateAdapter = longAdapter,
            tasksPlannedAdapter = longAdapter,
            tasksCompletedAdapter = longAdapter,
            completionRateAdapter = doubleAdapter,
            totalPlannedTimeAdapter = longAdapter,
            totalActiveTimeAdapter = longAdapter,
            completedPointsAdapter = longAdapter,
            totalBreakTimeAdapter = longAdapter,
            morningEnergyLevelAdapter = longAdapter,
            eveningEnergyLevelAdapter = longAdapter,
            stressLevelAdapter = longAdapter,
            customMetricsAdapter = customMetricsAdapter,
            createdAtAdapter = longAdapter,
            updatedAtAdapter = longAdapter
        ),
        ProjectArtifactsAdapter = ProjectArtifacts.Adapter()
    )
}
