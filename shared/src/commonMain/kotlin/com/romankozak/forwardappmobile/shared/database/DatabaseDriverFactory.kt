package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.data.database.models.ReservedGroup
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer

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

private val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        return if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            Json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
        }
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}

private val customMetricsAdapter = object : ColumnAdapter<Map<String, Float>, String> {
    override fun decode(databaseValue: String): Map<String, Float> {
        return Json.decodeFromString(databaseValue)
    }

    override fun encode(value: Map<String, Float>): String {
        return Json.encodeToString(value)
    }
}

private val taskPriorityAdapter =
    EnumColumnAdapter<TaskPriority>()
private val taskStatusAdapter =
    EnumColumnAdapter<TaskStatus>()
private val dayStatusAdapter =
    EnumColumnAdapter<DayStatus>()
private val recurrenceFrequencyAdapter =
    EnumColumnAdapter<RecurrenceFrequency>()
private val projectTypeAdapter =
    EnumColumnAdapter<ProjectType>()
private val reservedGroupAdapter =
    EnumColumnAdapter<ReservedGroup>()

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
            relatedLinksAdapter = relatedLinksListAdapter
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
            tasksPlannedAdapter = longAdapter,
            tasksCompletedAdapter = longAdapter,
            completionRateAdapter = doubleAdapter,
            completedPointsAdapter = longAdapter,
            morningEnergyLevelAdapter = longAdapter,
            eveningEnergyLevelAdapter = longAdapter,
            stressLevelAdapter = longAdapter,
            customMetricsAdapter = customMetricsAdapter,
            dateAdapter = longAdapter,
            totalPlannedTimeAdapter = longAdapter,
            totalActiveTimeAdapter = longAdapter,
            totalBreakTimeAdapter = longAdapter,
            createdAtAdapter = longAdapter,
            updatedAtAdapter = longAdapter
        ),
        ProjectArtifactsAdapter = ProjectArtifacts.Adapter()
    )
}
